package org.example.productcatalogapi.repository;

import org.example.productcatalogapi.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.search_vector @@ websearch_to_tsquery('simple', :query)
            ORDER BY ts_rank(p.search_vector, websearch_to_tsquery('simple', :query)) DESC, p.sku ASC
            """, countQuery = """
            SELECT count(*) FROM products p
            WHERE p.search_vector @@ websearch_to_tsquery('simple', :query)
            """, nativeQuery = true)
    Page<Product> search(@Param("query") String query, Pageable pageable);
}
