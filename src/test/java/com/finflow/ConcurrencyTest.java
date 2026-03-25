package com.finflow;

import com.finflow.entity.Account;
import com.finflow.entity.LedgerEntry;
import com.finflow.entity.Transaction;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.LedgerEntryRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
public class ConcurrencyTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Long accountAId;
    private Long accountBId;

    @BeforeEach
    public void setup() {
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account a = new Account();
        a.setAccountNumber("ACC-A");
        a.setOwnerName("Alice");
        a.setBalance(new BigDecimal("1000.0000"));
        a = accountRepository.save(a);
        accountAId = a.getId();

        Account b = new Account();
        b.setAccountNumber("ACC-B");
        b.setOwnerName("Bob");
        b.setBalance(new BigDecimal("1000.0000"));
        b = accountRepository.save(b);
        accountBId = b.getId();
    }

    @Test
    public void testConcurrentTransfers() throws Exception {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Callable<Void>> tasks = new ArrayList<>();
        List<Long> transactionIds = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            Transaction tx = new Transaction();
            tx.setTransactionRef(UUID.randomUUID().toString());
            tx.setIdempotencyKey("idemp-" + i);
            tx.setFromAccountId(accountAId);
            tx.setToAccountId(accountBId);
            tx.setAmount(new BigDecimal("10.0000")); // Each transfer is 10
            tx.setStatus("PENDING");
            tx = transactionRepository.save(tx);
            
            final Long txId = tx.getId();
            transactionIds.add(txId);
            
            tasks.add(() -> {
                ledgerService.executeTransferWithRetry(txId);
                return null;
            });
        }

        List<Future<Void>> futures = executorService.invokeAll(tasks);
        
        for (Future<Void> future : futures) {
            future.get(); // wait for completion
        }
        
        executorService.shutdown();

        Account finalA = accountRepository.findById(accountAId).orElseThrow();
        Account finalB = accountRepository.findById(accountBId).orElseThrow();

        assertEquals(new BigDecimal("800.0000").compareTo(finalA.getBalance()), 0, "Account A balance should be 800");
        assertEquals(new BigDecimal("1200.0000").compareTo(finalB.getBalance()), 0, "Account B balance should be 1200");

        List<LedgerEntry> entries = ledgerEntryRepository.findAll();
        assertEquals(40, entries.size(), "Should have exactly 40 ledger entries (2 per transaction)");

        BigDecimal totalDebits = entries.stream()
                .filter(e -> "DEBIT".equals(e.getEntryType()))
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream()
                .filter(e -> "CREDIT".equals(e.getEntryType()))
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, totalDebits.compareTo(totalCredits), "Sum of debits must equal sum of credits");
        assertEquals(new BigDecimal("200.0000").compareTo(totalDebits), 0, "Total debits should be 200");

        for (Long txId : transactionIds) {
            Transaction tx = transactionRepository.findById(txId).orElseThrow();
            assertEquals("SUCCESS", tx.getStatus(), "Transaction should be SUCCESS");
        }
    }
}
