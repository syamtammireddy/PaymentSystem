package com.finflow.service;


import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service

public class LedgerService {

    private final TransferProcessor transferProcessor;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LedgerService.class);

    public LedgerService(TransferProcessor transferProcessor) {
        this.transferProcessor = transferProcessor;
    }
    private static final int MAX_RETRIES = 50;

    public void executeTransferWithRetry(Long transactionId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                transferProcessor.processTransfer(transactionId);
                log.info("Transaction {} processed successfully on attempt {}", transactionId, attempt + 1);
                return; // Success
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                log.warn("Optimistic locking failure for transaction {}, attempt {}", transactionId, attempt);
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries reached for transaction {}. Marking as failed.", transactionId);
                    transferProcessor.markTransactionFailed(transactionId);
                    return;
                }
                try {
                    long backoff = 50 + (long) (Math.random() * 100);
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                log.error("Error processing transaction {}", transactionId, e);
                transferProcessor.markTransactionFailed(transactionId);
                return;
            }
        }
    }
}
