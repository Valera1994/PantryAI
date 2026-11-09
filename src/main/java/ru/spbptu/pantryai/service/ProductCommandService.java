package ru.spbptu.pantryai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class ProductCommandService {

    private static final Set<String> ALLOWED_UNITS =
            Set.of("шт.", "г", "кг", "мл", "л");

    private String normalizeUnit(String unit) {
        if (unit == null) {
            return null;
        }

        String value = unit.trim().toLowerCase();

        return switch (value) {
            case "шт", "штука", "штуки", "штук", "шт." -> "шт.";
            case "г", "гр", "грамм", "грамма", "граммов" -> "г";
            case "кг", "килограмм", "килограмма", "килограммов" -> "кг";
            case "мл", "миллилитр", "миллилитра", "миллилитров" -> "мл";
            case "л", "литр", "литра", "литров" -> "л";
            default -> null;
        };
    }


    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductCommandService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<ParsedProduct> parseCommand(String command) {
        String response = chatClient.prompt()
                .system("""
                        Ты помощник для учёта продуктов на кухне.
                        Преобразуй команду пользователя в JSON-массив.
                        Каждый объект содержит:
                        name — название продукта,
                        quantity — положительное целое число,
                        unit — одна из единиц: шт., г, кг, мл, л.

                        Не добавляй продукты, которых пользователь не называл и не являются продуктами питания.
                        Если продуктов нет, верни сообщение, что В запросе не обнаружено продуктов.
                        Ответ должен содержать только JSON без Markdown.
                        """)
                .user(command)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("ИИ вернул пустой ответ");
        }

        String json = response.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();

        try {
            List<ParsedProduct> products = objectMapper.readValue(
                    json, new TypeReference<List<ParsedProduct>>() {});

            for (ParsedProduct product : products) {
                if (product.name() == null
                        || product.name().isBlank()
                        || product.name().length() > 100) {
                    throw new IllegalArgumentException(
                            "Некорректное название продукта");
                }

                if (product.quantity() == null
                        || product.quantity().compareTo(BigDecimal.ZERO) <= 0
                        || product.quantity().stripTrailingZeros().scale() > 0) {
                    throw new IllegalArgumentException(
                            "Количество должно быть положительным целым числом");
                }

                String normalizedUnit = normalizeUnit(product.unit());

                if (normalizedUnit == null
                        || !ALLOWED_UNITS.contains(normalizedUnit)) {
                    throw new IllegalArgumentException(
                            "Неизвестная единица измерения: " + product.unit());
                }
            }

            return products.stream()
                    .map(product -> new ParsedProduct(
                            product.name().trim(),
                            product.quantity(),
                            normalizeUnit(product.unit())))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось разобрать ответ ИИ как корректный список продуктов",
                    e);
        }
    }

    public record ParsedProduct(
            String name,
            BigDecimal quantity,
            String unit) {
    }
}
