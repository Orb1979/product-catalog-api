package org.example.productcatalogapi.repository;

import org.example.productcatalogapi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
