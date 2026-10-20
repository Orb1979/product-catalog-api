package org.example.productcatalogapi.controller;

import java.util.List;
import java.util.Map;

import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.exception.ProductNotFoundException;
import org.example.productcatalogapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final ProductResponse PRODUCT = new ProductResponse(
        "SKU-000001", "Lamp", null, "Phillips", "Home", Map.of("color", "blue"), null, null, null);

    @MockitoBean
    private ProductService service;

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldReturnDefaultProductPage() throws Exception {
        when(service.getProducts(0, 20)).thenReturn(
                new PageImpl<>(List.of(PRODUCT), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-000001"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldReturnUnpricedProduct() throws Exception {
        when(service.getProduct("SKU-000001")).thenReturn(PRODUCT);

        mvc.perform(get("/api/products/SKU-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-000001"))
                .andExpect(jsonPath("$.attributes.color").value("blue"))
                .andExpect(jsonPath("$.price").value(nullValue()));
    }

    @Test
    void shouldReturnProblemWhenProductDoesNotExist() throws Exception {
        when(service.getProduct("UNKNOWN")).thenThrow(new ProductNotFoundException());

        mvc.perform(get("/api/products/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product not found."));
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101", "abc,20"})
    void shouldRejectInvalidPagination(String page, String size) throws Exception {
        mvc.perform(get("/api/products").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

}
