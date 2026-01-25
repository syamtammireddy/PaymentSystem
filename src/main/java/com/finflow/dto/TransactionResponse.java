package com.finflow.dto;


public class TransactionResponse {
    private String transactionRef;
    private String status;

    public TransactionResponse() {}

    public TransactionResponse(String transactionRef, String status) {
        this.transactionRef = transactionRef;
        this.status = status;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
