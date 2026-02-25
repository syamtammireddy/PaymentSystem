package com.finflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReconciliationService {

    private final JdbcTemplate jdbcTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReconciliationService.class);

    public ReconciliationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs every hour to verify that the total money in the system is consistent.
     * SUM(all account balances) should equal the net of all ledger entries.
     * If they don't match, something has gone wrong.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void reconcile() {
        log.info("[RECONCILIATION] Starting scheduled reconciliation at {}", LocalDateTime.now());

        BigDecimal totalAccountBalances = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(balance), 0) FROM accounts", BigDecimal.class);

        BigDecimal totalCredits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE entry_type = 'CREDIT'", BigDecimal.class);

        BigDecimal totalDebits = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE entry_type = 'DEBIT'", BigDecimal.class);

        BigDecimal ledgerNetMovement = totalCredits.subtract(totalDebits);

        log.info("[RECONCILIATION] Total Account Balances: {}", totalAccountBalances);
        log.info("[RECONCILIATION] Total Credits: {}", totalCredits);
        log.info("[RECONCILIATION] Total Debits: {}", totalDebits);
        log.info("[RECONCILIATION] Net Ledger Movement (Credits - Debits): {}", ledgerNetMovement);

        if (ledgerNetMovement.compareTo(BigDecimal.ZERO) != 0) {
            log.error("[RECONCILIATION] ⚠️ CRITICAL: Ledger imbalance detected! Net movement is {} (should be 0). " +
                    "Credits and Debits do not cancel out. Investigate immediately!", ledgerNetMovement);
        } else {
            log.info("[RECONCILIATION] ✅ All clear. Ledger is balanced. Credits and Debits cancel out perfectly.");
        }
    }
}
