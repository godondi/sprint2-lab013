package com.neueda.leap.merchantportal;

public class PayoutApprovalService {

    private PayoutRepository payoutRepository;

    public PayoutApprovalService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    // FIXED (A06): Enforced segregation of duties - the approver cannot be
    // the same user who requested the payout. This prevents a single user
    // from both requesting and approving their own transactions.
    public void approve(Long payoutId, Long approvingUserId) {
        PayoutRequest payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        // Enforce segregation of duties: approver cannot be the requester
        if (payout.getRequestedByUserId().equals(approvingUserId)) {
            throw new RuntimeException("Segregation of duties violation: a payout requester cannot approve their own request");
        }

        // Additional safety check: ensure payout is in valid state for approval
        if (!"PENDING".equals(payout.getApprovalStatus())) {
            throw new RuntimeException("Payout is not in PENDING status and cannot be approved");
        }

        payout.setApprovalStatus("APPROVED");
        payout.setApprovedByUserId(approvingUserId);
        payoutRepository.save(payout);
    }
}
