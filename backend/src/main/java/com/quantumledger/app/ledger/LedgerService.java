package com.quantumledger.app.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    /** Balance in minor units, seen from account's normal direction */
    @Transactional(readOnly = true)
    public long balanceOf(Long accountId, Direction normalSide){
        long netCredits = entryRepository.netCredits(accountId);
        return normalSide == Direction.CREDIT ? netCredits : - netCredits;
    }
}
