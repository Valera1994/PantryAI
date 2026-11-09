
package ru.spbptu.pantryai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductCommandServiceTest {

    private ChatClient.Builder builder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private ProductCommandService service;

    @BeforeEach
    void setUp() {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        service = new ProductCommandService(builder);
    }

    @Test
    void shouldParseValidProducts() {
        when(responseSpec.content()).thenReturn("""
                [
                  {"name":"молоко","quantity":1,"unit":"литр"},
                  {"name":"яйца","quantity":6,"unit":"штук"},
                  {"name":"сыр","quantity":200,"unit":"граммов"}
                ]
                """);

        List<ProductCommandService.ParsedProduct> products =
                service.parseCommand("Добавь литр молока, шесть яиц и сыр");

        assertEquals(3, products.size());
        assertEquals("молоко", products.get(0).name());
        assertEquals(new BigDecimal("1"), products.get(0).quantity());
        assertEquals("л", products.get(0).unit());
        assertEquals("шт.", products.get(1).unit());
        assertEquals("г", products.get(2).unit());
    }

    @Test
    void shouldParseJsonInsideMarkdownFence() {
        when(responseSpec.content()).thenReturn("""
                ```json
                [{"name":"Молоко","quantity":1,"unit":"л"}]
                ```
                """);

        List<ProductCommandService.ParsedProduct> products =
                service.parseCommand("Добавь молоко");

        assertEquals(1, products.size());
        assertEquals("Молоко", products.get(0).name());
    }

    @Test
    void shouldReturnEmptyListWhenAiReturnsEmptyArray() {
        when(responseSpec.content()).thenReturn("[]");

        List<ProductCommandService.ParsedProduct> products =
                service.parseCommand("Ничего не добавляй");

        assertTrue(products.isEmpty());
    }

    @Test
    void shouldRejectBlankProductName() {
        when(responseSpec.content()).thenReturn("""
                [{"name":" ","quantity":1,"unit":"л"}]
                """);

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь продукт"));
    }

    @Test
    void shouldRejectZeroQuantity() {
        when(responseSpec.content()).thenReturn("""
                [{"name":"Молоко","quantity":0,"unit":"л"}]
                """);

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь молоко"));
    }

    @Test
    void shouldRejectFractionalQuantity() {
        when(responseSpec.content()).thenReturn("""
                [{"name":"Молоко","quantity":1.5,"unit":"л"}]
                """);

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь молоко"));
    }

    @Test
    void shouldRejectUnknownUnit() {
        when(responseSpec.content()).thenReturn("""
                [{"name":"Молоко","quantity":1,"unit":"ведро"}]
                """);

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь молоко"));
    }

    @Test
    void shouldRejectInvalidJson() {
        when(responseSpec.content()).thenReturn("это не JSON");

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь молоко"));
    }

    @Test
    void shouldRejectBlankAiResponse() {
        when(responseSpec.content()).thenReturn("  ");

        assertThrows(IllegalArgumentException.class,
                () -> service.parseCommand("Добавь молоко"));
    }
}
