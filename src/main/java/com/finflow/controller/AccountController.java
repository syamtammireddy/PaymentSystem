package com.finflow.controller;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.CreateAccountRequest;
import com.finflow.entity.LedgerEntry;
import com.finflow.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountResponse> getBalance(@PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccountBalance(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/ledger")
    public ResponseEntity<Page<LedgerEntry>> getLedger(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LedgerEntry> entries = accountService.getAccountLedgerPaginated(accountNumber, pageRequest);
        return ResponseEntity.ok(entries);
    }
}
