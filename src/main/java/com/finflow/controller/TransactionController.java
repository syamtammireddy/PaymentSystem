package com.finflow.controller;

import com.finflow.annotation.RateLimited;
import com.finflow.dto.TransactionRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.entity.Transaction;
import com.finflow.service.TransactionService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")

public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RateLimited(maxRequests = 10, windowSeconds = 60)
    @PostMapping
    public ResponseEntity<TransactionResponse> initiateTransfer(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransactionRequest request) {

        String transactionRef = transactionService.initiateTransfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new TransactionResponse(transactionRef, "ACCEPTED"));
    }

    @GetMapping("/{ref}")
    public ResponseEntity<TransactionResponse> getTransactionStatus(@PathVariable String ref) {
        Transaction transaction = transactionService.getTransactionStatus(ref);
        return ResponseEntity.ok(new TransactionResponse(transaction.getTransactionRef(), transaction.getStatus()));
    }
}
