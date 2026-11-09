package ru.spbptu.pantryai.repository;

import ru.spbptu.pantryai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, Long> {
}
