package org.example.productcatalogapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductCatalogApiApplicationTest {

    @Test
    void shouldLoadCatalogApplication(ApplicationContext context) {
        assertThat(context.getBean(ProductCatalogApiApplication.class)).isNotNull();
    }

}
