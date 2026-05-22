package com.wanderai.controller;

import com.wanderai.dto.PaymentDto;
import com.wanderai.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/packages")
    public ResponseEntity<List<PaymentDto.CreditPackage>> getPackages() {
        return ResponseEntity.ok(paymentService.getPackages());
    }

    @PostMapping("/create-order")
    public ResponseEntity<PaymentDto.CreateOrderResponse> createOrder(@RequestBody PaymentDto.CreateOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(request.getPackageName()));
    }

    @GetMapping("/billing-history")
    public ResponseEntity<PaymentDto.BillingHistoryResponse> getBillingHistory() {
        return ResponseEntity.ok(paymentService.getBillingHistory());
    }
    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, String> payload) {
        paymentService.verifyAndAddCredits(
                payload.get("razorpayOrderId"),
                payload.get("razorpayPaymentId")
        );
        // Return updated credits
        var billing = paymentService.getBillingHistory();
        return ResponseEntity.ok(Map.of(
                "credits", billing.getCurrentCredits(),
                "message", "Credits added successfully"
        ));
    }
}
