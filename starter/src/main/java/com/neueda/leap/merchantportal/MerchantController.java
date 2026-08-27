package com.neueda.leap.merchantportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MerchantController {

    @Autowired
    private PayoutRepository payoutRepository;

    // Caller's merchant id is the identity resolved upstream from the
    // authenticated session/token (represented here as a header for the lab).
    // Mismatches return 404, same as a missing payout, so IDs can't be
    // enumerated based on differing 403 vs 404 responses.
    @GetMapping("/api/payouts/{payoutId}")
    public PayoutRequest getPayout(@PathVariable Long payoutId,
            @RequestHeader("X-Merchant-Id") Long callerMerchantId) {
        PayoutRequest payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (!payout.getMerchantId().equals(callerMerchantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found");
        }

        return payout;
    }
}
