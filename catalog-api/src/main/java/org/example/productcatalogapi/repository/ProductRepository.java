package org.example.productcatalogapi.repository;

import java.math.BigDecimal;
import java.time.Instant;

import org.example.productcatalogapi.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = """
            SELECT p.*, CAST(p.attributes AS text) AS attributes_sort FROM products p
            WHERE CAST(:query AS text) IS NULL
               OR p.search_vector @@ websearch_to_tsquery('simple', :query)
            """, countQuery = """
            SELECT count(*) FROM products p
            WHERE CAST(:query AS text) IS NULL
               OR p.search_vector @@ websearch_to_tsquery('simple', :query)
            """, nativeQuery = true)
    Page<Product> findSorted(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query(value = """
            UPDATE products
            SET price = :price, currency = :currency, price_updated_at = :updatedAt
            WHERE sku = :sku AND (price_updated_at IS NULL OR price_updated_at < :updatedAt)
            """, nativeQuery = true)
    int updatePriceIfNewer(@Param("sku") String sku, @Param("price") BigDecimal price,
                           @Param("currency") String currency, @Param("updatedAt") Instant updatedAt);
}
