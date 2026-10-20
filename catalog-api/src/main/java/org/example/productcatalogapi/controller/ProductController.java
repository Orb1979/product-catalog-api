package org.example.productcatalogapi.controller;

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.service.ProductService;
import org.springframework.data.domain.Sort;
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

    private static final Set<String> SORT_FIELDS = Set.of("sku", "name", "description", "brand",
            "category", "attributes", "price", "currency", "priceUpdatedAt");

    private final ProductService service;

    @GetMapping
    @Operation(summary = "List products or search their characteristics",
            description = "Search name, brand, category, description, and JSON attributes. "
                    + "Defaults to relevance for search, or SKU for browsing. "
                    + "Optional sort and direction parameters order all matching products before pagination.")
    public PagedModel<ProductResponse> getProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and 100.");
        }
        if (sort != null && !SORT_FIELDS.contains(sort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported product sort field.");
        }
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be asc or desc.");
        }
        Sort sorting = sort == null ? Sort.unsorted() : Sort.by(Sort.Direction.fromString(direction), sort);
        return new PagedModel<>(service.getProducts(query, page, size, sorting));
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get a product by SKU")
    public ProductResponse getProduct(@PathVariable String sku) {
        return service.getProduct(sku);
    }

}
