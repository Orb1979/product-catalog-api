package org.example.productcatalogapi;

import java.time.Clock;

import org.example.productcatalogapi.configuration.PricingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PricingProperties.class)
public class ProductCatalogApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogApiApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

}
