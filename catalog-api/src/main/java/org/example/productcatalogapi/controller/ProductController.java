package org.example.productcatalogapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.service.ProductService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    @Operation(summary = "List products ordered by SKU")
    public PagedModel<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 100.");
        }
        return new PagedModel<>(service.getProducts(page, size));
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get a product by SKU")
    public ProductResponse getProduct(@PathVariable String sku) {
        return service.getProduct(sku);
    }

}
