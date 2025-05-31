package com.sparklix.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.sparklix.paymentservice.dto.PaymentVerificationRequestDto;
import com.sparklix.paymentservice.dto.RazorpayOrderRequestDto;
import com.sparklix.paymentservice.dto.RazorpayOrderResponseDto;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class RazorpayService {
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    
    @Value("${razorpay.webhook.secret}")
    private String razorpayWebhookSecret;


    private RazorpayClient razorpayClient;
    
    // To call booking-service back
    private final RestTemplate loadBalancedRestTemplate;

    @Autowired // If you have only one RestTemplate bean, @Qualifier might not be needed.
                 // If you have multiple (like plain and loadBalanced), use @Qualifier.
    public RazorpayService(@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }


    @PostConstruct
    private void init() {
        try {
            this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            logger.info("Razorpay client initialized successfully.");
        } catch (RazorpayException e) {
            logger.error("Error initializing Razorpay client: {}", e.getMessage(), e);
            // Consider throwing a runtime exception to fail startup if client init fails
        }
    }

    public RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto orderRequest) throws RazorpayException {
        JSONObject orderRequestJson = new JSONObject();
        int amountInPaisa = orderRequest.getAmount().multiply(new BigDecimal("100")).intValue();
        orderRequestJson.put("amount", amountInPaisa);
        orderRequestJson.put("currency", orderRequest.getCurrency());
        orderRequestJson.put("receipt", orderRequest.getReceipt());
        JSONObject notes = new JSONObject();
        notes.put("internal_booking_id", orderRequest.getBookingId().toString());
        notes.put("user_id", orderRequest.getUserId());
        orderRequestJson.put("notes", notes);

        logger.debug("Creating Razorpay order with request: {}", orderRequestJson.toString());
        Order order = razorpayClient.orders.create(orderRequestJson);
        logger.info("Razorpay order created successfully: {}", order);

        return new RazorpayOrderResponseDto(
                order.get("id"),
                orderRequest.getBookingId(),
                order.get("currency"),
                order.get("amount"), // This is amount in paisa from Razorpay
                order.get("status"),
                this.razorpayKeyId
        );
    }

    public boolean verifyPaymentSignature(PaymentVerificationRequestDto verificationRequest) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", verificationRequest.getRazorpay_order_id());
            options.put("razorpay_payment_id", verificationRequest.getRazorpay_payment_id());
            options.put("razorpay_signature", verificationRequest.getRazorpay_signature());

            boolean isValid = Utils.verifyPaymentSignature(options, this.razorpayKeySecret);
            logger.info("Payment signature verification result for order_id {}: {}", verificationRequest.getRazorpay_order_id(), isValid);
            
            if (isValid) {
                // After successful signature verification, notify booking-service
                notifyBookingServicePaymentSuccess(verificationRequest.getInternalBookingId(), verificationRequest.getRazorpay_payment_id());
            } else {
                notifyBookingServicePaymentFailure(verificationRequest.getInternalBookingId());
            }
            return isValid;
        } catch (RazorpayException e) {
            logger.error("Error verifying payment signature for order_id {}: {}", verificationRequest.getRazorpay_order_id(), e.getMessage(), e);
            notifyBookingServicePaymentFailure(verificationRequest.getInternalBookingId());
            return false;
        }
    }

    public boolean handleWebhookEvent(String payload, String signature) {
        try {
            boolean isValidSignature = Utils.verifyWebhookSignature(payload, signature, this.razorpayWebhookSecret);
            if (!isValidSignature) {
                logger.warn("Webhook signature verification FAILED! Ignoring webhook.");
                return false; // Important to stop processing if signature is invalid
            }
            logger.info("Webhook signature VERIFIED. Processing event...");
            
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            logger.info("Received webhook event type: {}", eventType);

            if ("payment.captured".equals(eventType)) {
                JSONObject paymentEntity = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");
                String status = paymentEntity.getString("status");
                int amount = paymentEntity.getInt("amount");
                
                Long internalBookingId = null;
                if (paymentEntity.has("notes") && paymentEntity.getJSONObject("notes").has("internal_booking_id")) {
                   internalBookingId = Long.parseLong(paymentEntity.getJSONObject("notes").getString("internal_booking_id"));
                } else {
                    logger.warn("Webhook 'payment.captured': internal_booking_id missing in notes for order_id: {}", orderId);
                    // You might need a way to map razorpay_order_id back to your internalBookingId if notes are missing
                    return false; // Cannot proceed without internal booking ID
                }
                
                logger.info("Webhook: Payment captured - Payment ID: {}, Order ID: {}, Status: {}, Amount: {}, InternalBookingID: {}",
                            paymentId, orderId, status, amount, internalBookingId);
                
                notifyBookingServicePaymentSuccess(internalBookingId, paymentId);
                return true;
            } else if ("payment.failed".equals(eventType)) {
                JSONObject paymentEntity = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                 Long internalBookingId = null;
                if (paymentEntity.has("notes") && paymentEntity.getJSONObject("notes").has("internal_booking_id")) {
                   internalBookingId = Long.parseLong(paymentEntity.getJSONObject("notes").getString("internal_booking_id"));
                }
                logger.warn("Webhook: Payment failed event received for Order ID: {}, Internal Booking ID: {}", orderId, internalBookingId);
                if (internalBookingId != null) {
                    notifyBookingServicePaymentFailure(internalBookingId);
                }
                return true;
            } else {
                logger.info("Webhook: Received unhandled event type: {}", eventType);
            }
        } catch (RazorpayException e) {
            logger.error("RazorpayException while handling webhook: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("General exception while handling webhook: {}", e.getMessage(), e);
        }
        return false;
    }

    private void notifyBookingServicePaymentSuccess(Long internalBookingId, String razorpayPaymentId) {
        String bookingServiceUrl = "http://BOOKING-SERVICE/api/bookings/" + internalBookingId + "/confirm-payment?paymentId=" + razorpayPaymentId;
        try {
            logger.info("Notifying booking-service of payment success for bookingId {}: URL: {}", internalBookingId, bookingServiceUrl);
            // For PUT with query params and no body, HttpEntity can be null or an empty HttpEntity
            HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<String> response = loadBalancedRestTemplate.exchange(bookingServiceUrl, HttpMethod.PUT, requestEntity, String.class);
            logger.info("Booking-service notification response for success: {} - {}", response.getStatusCode(), response.getBody());
        } catch (RestClientException e) {
            logger.error("Error notifying booking-service of payment success for bookingId {}: {}", internalBookingId, e.getMessage());
            // TODO: Implement retry or dead-letter queue for failed notifications
        }
    }
    
    private void notifyBookingServicePaymentFailure(Long internalBookingId) {
        String bookingServiceUrl = "http://BOOKING-SERVICE/api/bookings/" + internalBookingId + "/payment-failed";
        try {
            logger.info("Notifying booking-service of payment failure for bookingId {}: URL: {}", internalBookingId, bookingServiceUrl);
             HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<String> response = loadBalancedRestTemplate.exchange(bookingServiceUrl, HttpMethod.PUT, requestEntity, String.class);
            logger.info("Booking-service notification response for failure: {} - {}", response.getStatusCode(), response.getBody());
        } catch (RestClientException e) {
            logger.error("Error notifying booking-service of payment failure for bookingId {}: {}", internalBookingId, e.getMessage());
            // TODO: Implement retry or dead-letter queue
        }
    }
}