package com.finflow.repository;

import com.finflow.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    Page<LedgerEntry> findByAccountId(Long accountId, Pageable pageable);
}
