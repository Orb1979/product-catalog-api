package org.example.mockpricingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MockPricingSystemApplicationTest {

    @Test
    void shouldLoadMockPricingApplication(ApplicationContext context) {
        assertThat(context.getBean(MockPricingSystemApplication.class)).isNotNull();
    }

}
