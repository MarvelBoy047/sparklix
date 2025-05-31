package com.sparklix.paymentservice.controller;

import com.sparklix.paymentservice.dto.PaymentStatusDto;
import com.sparklix.paymentservice.dto.PaymentVerificationRequestDto;
import com.sparklix.paymentservice.dto.RazorpayOrderRequestDto;
import com.sparklix.paymentservice.dto.RazorpayOrderResponseDto;
import com.sparklix.paymentservice.service.RazorpayService;
import com.razorpay.RazorpayException; // Ensure this is imported
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final RazorpayService razorpayService;

    public PaymentController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    public ResponseEntity<?> createRazorpayOrder(@RequestBody RazorpayOrderRequestDto orderRequest) {
        logger.info("PAYMENT-CONTROLLER: Received request to create Razorpay order for bookingId: {}", orderRequest.getBookingId());
        try {
            RazorpayOrderResponseDto orderResponse = razorpayService.createOrder(orderRequest);
            return ResponseEntity.ok(orderResponse);
        } catch (RazorpayException e) {
            logger.error("PAYMENT-CONTROLLER: RazorpayException while creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new PaymentStatusDto(orderRequest.getBookingId(), null, "ORDER_CREATION_FAILED", "Error creating Razorpay order: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("PAYMENT-CONTROLLER: Unexpected error while creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new PaymentStatusDto(orderRequest.getBookingId(), null, "ORDER_CREATION_FAILED", "Unexpected error creating order."));
        }
    }

    @PostMapping("/verify-signature")
    // This endpoint could be called by your frontend after Razorpay checkout success.
    // Or, if you primarily rely on webhooks, this might be less used or used as a fallback.
    @PreAuthorize("isAuthenticated()") // Or public, if the frontend doesn't have a user session for this.
    public ResponseEntity<PaymentStatusDto> verifyPaymentSignature(@RequestBody PaymentVerificationRequestDto verificationRequest) {
        logger.info("PAYMENT-CONTROLLER: Received request to verify payment signature for Razorpay orderId: {}", verificationRequest.getRazorpay_order_id());
        boolean isValid = razorpayService.verifyPaymentSignature(verificationRequest);
        if (isValid) {
            logger.info("PAYMENT-CONTROLLER: Payment signature VERIFIED for Razorpay orderId: {}", verificationRequest.getRazorpay_order_id());
            // The RazorpayService.verifyPaymentSignature will now call notifyBookingServicePaymentSuccess
            return ResponseEntity.ok(new PaymentStatusDto(
                verificationRequest.getInternalBookingId(),
                verificationRequest.getRazorpay_payment_id(),
                "SIGNATURE_VERIFIED_SUCCESS", // Or a more generic "PROCESSING"
                "Payment signature verified successfully. Booking update initiated."
            ));
        } else {
            logger.warn("PAYMENT-CONTROLLER: Payment signature VERIFICATION FAILED for Razorpay orderId: {}", verificationRequest.getRazorpay_order_id());
            // The RazorpayService.verifyPaymentSignature will now call notifyBookingServicePaymentFailure
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PaymentStatusDto(
                verificationRequest.getInternalBookingId(),
                verificationRequest.getRazorpay_payment_id(),
                "SIGNATURE_VERIFICATION_FAILED",
                "Payment signature verification failed."
            ));
        }
    }

    // Webhook endpoint for Razorpay
    // This endpoint MUST BE PUBLICLY ACCESSIBLE by Razorpay servers.
    // No JWT auth here. Security is via Razorpay's X-Razorpay-Signature header.
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(@RequestBody String payload,
                                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        logger.info("PAYMENT-CONTROLLER: Received Razorpay webhook. Signature: {}", signature != null ? "Present" : "MISSING_OR_NOT_CONFIGURED_IN_RAZORPAY");
        
        if (signature == null) {
             logger.warn("PAYMENT-CONTROLLER: Webhook received without X-Razorpay-Signature header. Ensure Razorpay is configured to send it.");
             // Depending on strictness, you might reject if signature is expected but missing.
        }

        boolean processed = razorpayService.handleWebhookEvent(payload, signature);

        if(processed) {
            // Razorpay expects a 200 OK for successfully received webhooks.
            // The body doesn't really matter to Razorpay for the response.
            return ResponseEntity.ok("Webhook event processed");
        } else {
            // If processing failed or signature was invalid (and you checked it strictly)
            // Still return 200 if you ACK the receipt but couldn't process,
            // or 400/500 if you want Razorpay to retry (check their retry policy).
            // For now, let's assume any failure to process is a bad request from our side.
            logger.warn("PAYMENT-CONTROLLER: Webhook event not fully processed or an error occurred during handling.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook event not processed or error occurred");
        }
    }
}