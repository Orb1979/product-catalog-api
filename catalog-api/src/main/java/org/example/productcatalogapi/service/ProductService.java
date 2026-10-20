package org.example.productcatalogapi.service;

import lombok.RequiredArgsConstructor;
import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.exception.ProductNotFoundException;
import org.example.productcatalogapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public Page<ProductResponse> getProducts(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("sku")))
                .map(ProductResponse::from);
    }

    public ProductResponse getProduct(String sku) {
        return repository.findById(sku)
                .map(ProductResponse::from)
                .orElseThrow(ProductNotFoundException::new);
    }

}
