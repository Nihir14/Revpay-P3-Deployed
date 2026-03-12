package com.revpay;

import com.revpay.client.LoanClient;
import com.revpay.client.TransactionClient;
import com.revpay.client.WalletClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    // Demonstrating the use of @MockBean for all @FeignClient interfaces in
    // integration tests
    @MockBean
    private LoanClient loanClient;

    @MockBean
    private TransactionClient transactionClient;

    @MockBean
    private WalletClient walletClient;

    @Test
    void contextLoadsWithIsolatedFeignClients() {
        // This test ensures that the User Service application context loads
        // successfully.
        // The Feign clients are mocked using @MockBean to prevent actual HTTP network
        // calls,
        // and the Eureka client is disabled via application-test.yml.
        assertTrue(true, "Context loaded successfully isolated with MockBeans");
    }
}
