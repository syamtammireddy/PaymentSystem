package com.finflow.service;

import com.finflow.dto.TransactionRequest;
import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service

public class TransactionService {

    private final IdempotencyService idempotencyService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionProducer transactionProducer;

    public TransactionService(IdempotencyService idempotencyService, TransactionRepository transactionRepository, AccountRepository accountRepository, TransactionProducer transactionProducer) {
        this.idempotencyService = idempotencyService;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionProducer = transactionProducer;
    }

    public String initiateTransfer(TransactionRequest request, String idempotencyKey) {
        String newTransactionRef = UUID.randomUUID().toString();
        
        String cachedRef = idempotencyService.getOrReserve(idempotencyKey, newTransactionRef);
        if (cachedRef != null) {
            return cachedRef; // Duplicate request, return cached response
        }

        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

        Transaction transaction = new Transaction();
        transaction.setTransactionRef(newTransactionRef);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setFromAccountId(fromAccount.getId());
        transaction.setToAccountId(toAccount.getId());
        transaction.setAmount(request.getAmount());
        transaction.setStatus("PENDING");
        
        transaction = transactionRepository.save(transaction);

        transactionProducer.publishTransactionEvent(transaction.getId());

        return newTransactionRef;
    }

    public Transaction getTransactionStatus(String transactionRef) {
        return transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }
}
