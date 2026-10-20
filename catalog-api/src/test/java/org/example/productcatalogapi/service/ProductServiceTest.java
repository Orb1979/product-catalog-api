package org.example.productcatalogapi.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.exception.ProductNotFoundException;
import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldMapProductToResponse() {
        Product product = new Product("SKU-000001", "Lamp", "Desk lamp", "Acme",
                "Home", Map.of("color", "blue"));
        when(repository.findById("SKU-000001")).thenReturn(Optional.of(product));

        ProductResponse response = service.getProduct("SKU-000001");

        assertThat(response).isEqualTo(new ProductResponse("SKU-000001", "Lamp",
                "Desk lamp", "Acme", "Home", Map.of("color", "blue"), null, null, null));
    }

    @Test
    void shouldReturnRequestedPageOrderedBySku() {
        PageRequest request = PageRequest.of(1, 10, Sort.by("sku"));
        Product product = new Product("SKU-000011", "Lamp", null, null, null, Map.of());
        when(repository.findAll(request)).thenReturn(new PageImpl<>(List.of(product), request, 21));

        Page<ProductResponse> response = service.getProducts(1, 10);

        assertThat(response.getContent()).extracting(ProductResponse::sku).containsExactly("SKU-000011");
        assertThat(response.getNumber()).isEqualTo(1);
        assertThat(response.getTotalElements()).isEqualTo(21);
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct("UNKNOWN"))
                .isInstanceOf(ProductNotFoundException.class);
    }

}
