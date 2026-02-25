package com.finflow.service;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.CreateAccountRequest;
import com.finflow.entity.Account;
import com.finflow.entity.LedgerEntry;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("Account number already exists");
        }

        Account account = new Account();
        account.setAccountNumber(request.getAccountNumber());
        account.setOwnerName(request.getOwnerName());
        account.setBalance(request.getInitialBalance());

        Account saved = accountRepository.save(account);

        return new AccountResponse(saved.getAccountNumber(), saved.getOwnerName(), saved.getBalance());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return new AccountResponse(account.getAccountNumber(), account.getOwnerName(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getAccountLedger(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getAccountLedgerPaginated(String accountNumber, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return ledgerEntryRepository.findByAccountId(account.getId(), pageable);
    }
}
