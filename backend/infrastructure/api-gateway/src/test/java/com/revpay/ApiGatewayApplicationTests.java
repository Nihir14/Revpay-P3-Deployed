package com.revpay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the API Gateway application context loads
        // successfully without attempting to connect to the Eureka Discovery Server,
        // as configured in application-test.yml.
    }
}
