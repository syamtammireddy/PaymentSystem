package com.finflow.service;

import com.finflow.dto.TransactionEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service

public class TransactionConsumer {

    private final LedgerService ledgerService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionConsumer.class);

    public TransactionConsumer(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @KafkaListener(topics = "transaction-requests", groupId = "finflow-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        log.info("Received transaction event for transactionId: {}", event.getTransactionId());
        ledgerService.executeTransferWithRetry(event.getTransactionId());
    }
}
