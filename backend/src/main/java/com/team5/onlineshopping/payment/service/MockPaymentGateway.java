package com.team5.onlineshopping.payment.service;

import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Mock Payment Gateway for college demonstration.
 * Never connects to external networks and never collects real financial credentials.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final String gatewaySecret;
    private final Map<String, GatewayTransactionRecord> simulatedLedger = new ConcurrentHashMap<>();

    public record GatewayTransactionRecord(
            String paymentReference,
            String gatewayReference,
            Long orderId,
            BigDecimal amount,
            PaymentStatus expectedStatus,
            String failureReason
    ) {}

    public MockPaymentGateway(@Value("${security.jwt.secret:defaultMockGatewaySecretKeyForDemo384BitLongEnough}") String secret) {
        this.gatewaySecret = secret;
    }

    @Override
    public GatewayInitiateResult initiatePayment(Long orderId, String orderNumber, BigDecimal amount,
                                                 PaymentMethod paymentMethod, String simulatedOutcome) {
        String paymentReference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String gatewayReference = "MOCK-GW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        PaymentStatus expectedStatus = PaymentStatus.CAPTURED;
        String failureReason = null;

        if ("FAIL".equalsIgnoreCase(simulatedOutcome) || "FAILED".equalsIgnoreCase(simulatedOutcome)) {
            expectedStatus = PaymentStatus.FAILED;
            failureReason = "Payment simulation configured as FAILED by customer or demo scenario";
        }

        simulatedLedger.put(paymentReference, new GatewayTransactionRecord(
                paymentReference,
                gatewayReference,
                orderId,
                amount,
                expectedStatus,
                failureReason
        ));

        String redirectUrl = "https://mock-gateway.local/pay/" + paymentReference;

        return new GatewayInitiateResult(
                paymentReference,
                gatewayReference,
                PaymentStatus.PENDING,
                redirectUrl
        );
    }

    @Override
    public GatewayVerifyResult verifyPayment(String paymentReference, String signature) {
        GatewayTransactionRecord record = simulatedLedger.get(paymentReference);
        if (record == null) {
            return new GatewayVerifyResult(false, PaymentStatus.FAILED, null, "Unknown payment reference in gateway ledger");
        }

        String expectedSignature = generateSignature(paymentReference, record.expectedStatus());
        boolean signatureValid = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );

        if (!signatureValid) {
            return new GatewayVerifyResult(false, PaymentStatus.FAILED, record.gatewayReference(), "Invalid gateway signature");
        }

        return new GatewayVerifyResult(
                true,
                record.expectedStatus(),
                record.gatewayReference(),
                record.failureReason()
        );
    }

    @Override
    public String generateSignature(String paymentReference, PaymentStatus status) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(gatewaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            String payload = paymentReference + ":" + (status != null ? status.name() : "");
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error computing gateway signature", e);
        }
    }
}
