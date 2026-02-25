package com.finflow.service;

import com.finflow.dto.TransactionEvent;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service

public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    private static final String TOPIC = "transaction-requests";

    public void publishTransactionEvent(Long transactionId) {
        TransactionEvent event = new TransactionEvent(transactionId);
        kafkaTemplate.send(TOPIC, String.valueOf(transactionId), event);
    }
}
