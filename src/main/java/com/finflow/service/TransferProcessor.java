package com.finflow.service;

import com.finflow.entity.Account;
import com.finflow.entity.LedgerEntry;
import com.finflow.entity.Transaction;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.LedgerEntryRepository;
import com.finflow.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service

public class TransferProcessor {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;

    public TransferProcessor(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTransfer(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!"PENDING".equals(transaction.getStatus())) {
            return;
        }

        Account fromAccount = accountRepository.findById(transaction.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));
        Account toAccount = accountRepository.findById(transaction.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

        if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus("FAILED");
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            throw new IllegalStateException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(transaction.getAmount()));
        accountRepository.save(fromAccount);

        toAccount.setBalance(toAccount.getBalance().add(transaction.getAmount()));
        accountRepository.save(toAccount);

        LedgerEntry debit = new LedgerEntry();
        debit.setTransactionId(transaction.getId());
        debit.setAccountId(fromAccount.getId());
        debit.setEntryType("DEBIT");
        debit.setAmount(transaction.getAmount());
        debit.setBalanceAfter(fromAccount.getBalance());
        ledgerEntryRepository.save(debit);

        LedgerEntry credit = new LedgerEntry();
        credit.setTransactionId(transaction.getId());
        credit.setAccountId(toAccount.getId());
        credit.setEntryType("CREDIT");
        credit.setAmount(transaction.getAmount());
        credit.setBalanceAfter(toAccount.getBalance());
        ledgerEntryRepository.save(credit);

        transaction.setStatus("SUCCESS");
        transaction.setProcessedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTransactionFailed(Long transactionId) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            tx.setStatus("FAILED");
            tx.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }
}
