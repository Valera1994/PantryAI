package ru.spbptu.pantryai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.spbptu.pantryai.model.Product;
import ru.spbptu.pantryai.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductRepository repository;

    @Test
    void shouldAddProduct() throws Exception {
        Product product = new Product(
                "Молоко", new BigDecimal("1"), "л"
        );

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Молоко"))
                .andExpect(jsonPath("$.unit").value("л"));

        verify(repository).save(any(Product.class));
    }

    @Test
    void shouldRejectProductWithBlankName() throws Exception {
        String json = """
                {
                    "name": " ",
                    "quantity": 1,
                    "unit": "л"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void shouldDeleteExistingProduct() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingProduct()
            throws Exception {
        when(repository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound());

        verify(repository, never()).deleteById(99L);
    }

    @Test
    void shouldUpdateExistingProduct() throws Exception {
        Product existing = new Product(
                "Молоко", new BigDecimal("1"), "л"
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String json = """
                {
                    "name": "Молоко",
                    "quantity": 250,
                    "unit": "мл"
                }
                """;

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(250))
                .andExpect(jsonPath("$.unit").value("мл"));

        verify(repository).save(any(Product.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingProduct()
            throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        String json = """
                {
                    "name": "Молоко",
                    "quantity": 1,
                    "unit": "л"
                }
                """;

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());

        verify(repository, never()).save(any(Product.class));
    }
}
