package com.finflow;

import com.finflow.dto.TransactionRequest;
import com.finflow.entity.Account;
import com.finflow.repository.AccountRepository;
import com.finflow.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
public class TransactionFailureTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        accountRepository.deleteAll();
        
        Account sender = new Account();
        sender.setAccountNumber("TEST-SENDER");
        sender.setOwnerName("Sender");
        sender.setBalance(new BigDecimal("500.00"));
        accountRepository.save(sender);

        Account receiver = new Account();
        receiver.setAccountNumber("TEST-RECEIVER");
        receiver.setOwnerName("Receiver");
        receiver.setBalance(new BigDecimal("100.00"));
        accountRepository.save(receiver);
    }

    @Test
    void whenTransferFails_thenBalancesRemainUnchanged() {
        TransactionRequest request = new TransactionRequest();
        request.setFromAccount("TEST-SENDER");
        request.setToAccount("TEST-RECEIVER");
        request.setAmount(new BigDecimal("10000.00")); // Way more than they have!

        String idempotencyKey = UUID.randomUUID().toString();

        
        transactionService.initiateTransfer(request, idempotencyKey);
        
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        Account senderAfter = accountRepository.findByAccountNumber("TEST-SENDER").get();
        Account receiverAfter = accountRepository.findByAccountNumber("TEST-RECEIVER").get();

        assertEquals(new BigDecimal("500.0000"), senderAfter.getBalance(), "Sender balance should be completely untouched!");
        assertEquals(new BigDecimal("100.0000"), receiverAfter.getBalance(), "Receiver balance should be completely untouched!");
    }
}
