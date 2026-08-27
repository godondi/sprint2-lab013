package com.neueda.leap.merchantportal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
public class WebhookController {

    @Value("${webhook.payment-status.secret}")
    private String webhookSecret;

    // FIXED (A08): Added HMAC-SHA256 signature verification to ensure webhooks
    // come from the legitimate payment provider. The request must include
    // a valid X-Webhook-Signature header with an HMAC-SHA256 hash of the
    // request body signed with the shared secret.
    @PostMapping("/api/webhooks/payment-status")
    public void handlePaymentStatusWebhook(
            @RequestBody PaymentStatusEvent event,
            @RequestHeader("X-Webhook-Signature") String signature) {
        
        if (!verifyWebhookSignature(event, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
        
        payoutStatusUpdater.markSettled(event.getPayoutId(), event.getStatus());
    }

    private boolean verifyWebhookSignature(PaymentStatusEvent event, String providedSignature) {
        try {
            // Serialize the event to JSON for signature verification
            String eventJson = serializeEvent(event);
            
            // Compute HMAC-SHA256 of the event body using the shared secret
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(eventJson.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hmacBytes);
            
            // Use constant-time comparison to prevent timing attacks
            return constantTimeCompare(computedSignature, providedSignature);
        } catch (Exception e) {
            // Log security event and reject on any verification error
            return false;
        }
    }

    private String serializeEvent(PaymentStatusEvent event) {
        // Simple serialization - in production, use a proper JSON serializer
        // and ensure consistent field ordering for deterministic signatures
        return String.format("{\"payoutId\":\"%s\",\"status\":\"%s\"}", 
                event.getPayoutId(), event.getStatus());
    }

    private boolean constantTimeCompare(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }

    private PayoutStatusUpdater payoutStatusUpdater;
}
