package com.wanderai.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.wanderai.dto.PaymentDto;
import com.wanderai.entity.Transaction;
import com.wanderai.entity.User;
import com.wanderai.repository.TransactionRepository;
import com.wanderai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    // Credit packages configuration
    private static final Map<String, PaymentDto.CreditPackage> PACKAGES = Map.of(
        "BASIC", PaymentDto.CreditPackage.builder()
            .name("BASIC").credits(10).price(199.0).currency("INR")
            .description("10 AI itineraries").build(),
        "PRO", PaymentDto.CreditPackage.builder()
            .name("PRO").credits(30).price(499.0).currency("INR")
            .description("30 AI itineraries + priority support").build(),
        "ENTERPRISE", PaymentDto.CreditPackage.builder()
            .name("ENTERPRISE").credits(100).price(1299.0).currency("INR")
            .description("100 AI itineraries + premium features").build()
    );

    public List<PaymentDto.CreditPackage> getPackages() {
        return List.copyOf(PACKAGES.values());
    }

    @Transactional
    public PaymentDto.CreateOrderResponse createOrder(String packageName) {
        PaymentDto.CreditPackage pkg = PACKAGES.get(packageName.toUpperCase());
        if (pkg == null) throw new RuntimeException("Invalid package: " + packageName);

        User user = getCurrentUser();

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(pkg.getPrice() * 100)); // paise
            orderRequest.put("currency", pkg.getCurrency());
            orderRequest.put("receipt", "wanderai_" + user.getId() + "_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            // Save pending transaction
            Transaction transaction = Transaction.builder()
                    .user(user)
                    .razorpayOrderId(orderId)
                    .amount(pkg.getPrice())
                    .currency(pkg.getCurrency())
                    .creditsAdded(pkg.getCredits())
                    .packageName(packageName.toUpperCase())
                    .status(Transaction.TransactionStatus.CREATED)
                    .build();

            transactionRepository.save(transaction);

            return PaymentDto.CreateOrderResponse.builder()
                    .orderId(orderId)
                    .amount(pkg.getPrice())
                    .currency(pkg.getCurrency())
                    .keyId(razorpayKeyId)
                    .packageName(packageName)
                    .creditsToAdd(pkg.getCredits())
                    .build();

        } catch (Exception e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment order");
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        try {
            // Verify webhook signature
            // In production: RazorpayClient.validateWebhookSignature(payload, signature, webhookSecret)

            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            if ("payment.captured".equals(eventType)) {
                JSONObject paymentEntity = event.getJSONObject("payload")
                        .getJSONObject("payment").getJSONObject("entity");

                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");

                processSuccessfulPayment(orderId, paymentId);
            } else if ("payment.failed".equals(eventType)) {
                JSONObject paymentEntity = event.getJSONObject("payload")
                        .getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                processFailedPayment(orderId);
            }

        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            throw new RuntimeException("Webhook processing failed");
        }
    }

    private void processSuccessfulPayment(String orderId, String paymentId) {
        Transaction transaction = transactionRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for order: " + orderId));

        if (transaction.getStatus() == Transaction.TransactionStatus.SUCCESS) {
            log.warn("Duplicate webhook for order: {}", orderId);
            return; // Idempotency check
        }

        transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
        transaction.setRazorpayPaymentId(paymentId);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Add credits to user
        User user = transaction.getUser();
        user.setCreditsBalance(user.getCreditsBalance() + transaction.getCreditsAdded());
        userRepository.save(user);

        log.info("Payment successful. Added {} credits to user {}", transaction.getCreditsAdded(), user.getEmail());
    }

    private void processFailedPayment(String orderId) {
        transactionRepository.findByRazorpayOrderId(orderId).ifPresent(t -> {
            t.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(t);
        });
    }

    public PaymentDto.BillingHistoryResponse getBillingHistory() {
        User user = getCurrentUser();
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<PaymentDto.TransactionResponse> responses = transactions.stream()
                .map(t -> PaymentDto.TransactionResponse.builder()
                        .id(t.getId())
                        .razorpayOrderId(t.getRazorpayOrderId())
                        .razorpayPaymentId(t.getRazorpayPaymentId())
                        .amount(t.getAmount())
                        .currency(t.getCurrency())
                        .creditsAdded(t.getCreditsAdded())
                        .packageName(t.getPackageName())
                        .status(t.getStatus().name())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PaymentDto.BillingHistoryResponse.builder()
                .currentCredits(user.getCreditsBalance())
                .transactions(responses)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Transactional
    public void verifyAndAddCredits(String orderId, String paymentId) {
        processSuccessfulPayment(orderId, paymentId);
    }
}
