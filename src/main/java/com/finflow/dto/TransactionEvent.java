package com.finflow.dto;


public class TransactionEvent {
    private Long transactionId;

    public TransactionEvent() {}

    public TransactionEvent(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
