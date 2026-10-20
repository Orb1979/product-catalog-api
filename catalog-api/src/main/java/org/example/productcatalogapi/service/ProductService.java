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

    public Page<ProductResponse> getProducts(String query, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<Product> products = StringUtils.hasText(query)
                ? repository.search(query.strip(), request)
                : repository.findAll(request.withSort(Sort.by("sku")));
        return products.map(ProductResponse::from);
    }

    public ProductResponse getProduct(String sku) {
        return repository.findById(sku)
                .map(ProductResponse::from)
                .orElseThrow(ProductNotFoundException::new);
    }

}
