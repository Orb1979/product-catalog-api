package org.example.productcatalogapi.service;

import lombok.RequiredArgsConstructor;
import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.exception.ProductNotFoundException;
import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public Page<ProductResponse> getProducts(String query, int page, int size, Sort sort) {
        PageRequest request = PageRequest.of(page, size);
        String searchQuery = StringUtils.hasText(query) ? query.strip() : null;
        if (sort.isSorted()) {
            return repository.findSorted(searchQuery, request.withSort(databaseSort(sort)))
                    .map(ProductResponse::from);
        }
        Page<Product> products = searchQuery != null
                ? repository.search(searchQuery, request)
                : repository.findAll(request.withSort(Sort.by("sku")));
        return products.map(ProductResponse::from);
    }

    public ProductResponse getProduct(String sku) {
        return repository.findById(sku)
                .map(ProductResponse::from)
                .orElseThrow(ProductNotFoundException::new);
    }

    private Sort databaseSort(Sort sort) {
        Sort databaseSort = Sort.by(sort.stream().map(order -> order.withProperty(switch (order.getProperty()) {
            case "priceUpdatedAt" -> "price_updated_at";
            case "attributes" -> "attributes_sort";
            default -> order.getProperty();
        }).nullsLast()).toList());
        return sort.getOrderFor("sku") == null ? databaseSort.and(Sort.by("sku")) : databaseSort;
    }

}
