package com.neueda.leap.merchantportal;

import java.util.List;

public class BatchPayoutJob {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchPayoutJob.class);

    private BankTransferClient bankTransferClient;
    private PayoutRepository payoutRepository;

    public BatchPayoutJob(BankTransferClient bankTransferClient, PayoutRepository payoutRepository) {
        this.bankTransferClient = bankTransferClient;
        this.payoutRepository = payoutRepository;
    }

    // Only APPROVED payouts are attempted, so a re-run after a partial
    // failure skips anything already PAID/FAILED instead of retransferring.
    // A failed transfer is marked FAILED, never PAID, so success and
    // failure can never be confused with each other.
    public void runNightlyBatch(List<PayoutRequest> approvedPayouts) {
        for (PayoutRequest payout : approvedPayouts) {
            if (!"APPROVED".equals(payout.getApprovalStatus())) {
                continue;
            }
            try {
                bankTransferClient.transfer(payout.getMerchantId(), payout.getAmount());
                payout.setApprovalStatus("PAID");
            } catch (BankTransferException e) {
                log.error("Transfer failed for payout {}: {}", payout.getId(), e.getMessage(), e);
                payout.setApprovalStatus("FAILED");
            }
            payoutRepository.save(payout);
        }
    }
}
