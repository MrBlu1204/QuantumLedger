package com.quantumledger.app.reconciliation;

import com.quantumledger.app.config.SchedulingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.reconciliation.enabled", havingValue = "true")
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final ReconciliationService reconciliationService;

    public ReconciliationJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "${app.reconciliation.cron}")
    public void run() {
        ReconciliationReport report = reconciliationService.check();

        if(report.isHealthy()){
            log.info("Reconciliation OK");
            return;
        }

        log.error("RECONCILIATION FAILED: {} unbalanced transaction(s), {} balance mismatch(es)",
                report.unbalancedTransactions().size(), report.balanceMismatches().size());

        report.unbalancedTransactions().forEach(reconciliation ->
            log.error("     ledger transaction {} is out of balance by {} minor units",
                    reconciliation.ledgerTransactionId(), reconciliation.imbalanceMinor())
        );

        report.balanceMismatches().forEach(reconciliation ->
                log.error("     account {}  stored balance {} but entries add up to {}",
                        reconciliation.accountId(), reconciliation.storedMinor(), reconciliation.computedMinor())
        );
    }
}
