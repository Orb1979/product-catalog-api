package org.example.mockpricingsystem;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockPricingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockPricingSystemApplication.class, args);
    }

    @Bean
    Clock pricingClock() {
        return Clock.systemUTC();
    }

}
