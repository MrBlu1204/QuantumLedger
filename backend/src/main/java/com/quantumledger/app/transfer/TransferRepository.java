package com.quantumledger.app.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByTransferRef(String transferRef);
}
