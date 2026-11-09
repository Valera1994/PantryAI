package ru.spbptu.pantryai.controller;

import ru.spbptu.pantryai.service.ProductCommandService;
import ru.spbptu.pantryai.repository.ProductRepository;
import ru.spbptu.pantryai.model.Product;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/command")
public class ProductCommandController {

    private final ProductCommandService commandService;
    private final ProductRepository productRepository;

    public ProductCommandController(
            ProductCommandService commandService,
            ProductRepository productRepository) {
        this.commandService = commandService;
        this.productRepository = productRepository;
    }

    @PostMapping("/parse")
    public Map<String, Object> parseCommand(
            @RequestBody Map<String, String> request) {

        String command = request.get("command");

        if (command == null || command.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Команда не должна быть пустой");
        }

        if (command.length() > 500) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Команда слишком длинная");
        }

        List<ProductCommandService.ParsedProduct> products =
                commandService.parseCommand(command);

        return Map.of("products", products);
    }

    @PostMapping("/save")
    public Map<String, Object> saveProducts(
            @RequestBody List<ProductCommandService.ParsedProduct> products) {

        if (products == null || products.isEmpty() || products.size() > 50) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Список продуктов пуст или слишком большой");
        }

        List<Product> savedProducts = products.stream()
                .map(item -> {
                    Product product = new Product(
                            item.name().trim(),
                            item.quantity(),
                            item.unit()
                    );
                    return productRepository.save(product);
                })
                .toList();

        return Map.of("saved", savedProducts.size());
    }
}
