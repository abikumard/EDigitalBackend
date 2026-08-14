package com.contenthub.service;

import com.contenthub.dto.PaymentDtos.CreateCartOrderResponse;
import com.contenthub.dto.PaymentDtos.CreateOrderResponse;
import com.contenthub.dto.PaymentDtos.VerifyCartPaymentRequest;
import com.contenthub.dto.PaymentDtos.VerifyCartPaymentResponse;
import com.contenthub.dto.PaymentDtos.VerifyPaymentRequest;
import com.contenthub.dto.PaymentDtos.VerifyPaymentResponse;
import com.contenthub.entity.CartItem;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Purchase;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.PaymentException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.CartItemRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final PurchaseRepository purchaseRepository;
    private final CartItemRepository cartItemRepository;
    private final ContentService contentService;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;

    @Value("${app.razorpay.currency}")
    private String currency;

    public PaymentService(PurchaseRepository purchaseRepository,
                           CartItemRepository cartItemRepository,
                           ContentService contentService,
                           RestTemplate restTemplate,
                           UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.cartItemRepository = cartItemRepository;
        this.contentService = contentService;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateOrderResponse createOrder(Long userId, Long contentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ContentItem content = contentService.getEntityOrThrow(contentId);

        boolean already = purchaseRepository.existsByUser_IdAndContent_IdAndStatus(userId, contentId, Purchase.Status.SUCCESS);
        if (already) {
            return new CreateOrderResponse(null, null, 0, content.getPrice(), currency, keyId, true);
        }

        if (isPlaceholder(keyId) || isPlaceholder(keySecret)) {
            throw new PaymentException("Payment is not configured yet. Add your Razorpay keys in application.properties.");
        }

        long amountInPaise = content.getPrice().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setContent(content);
        purchase.setAmount(content.getPrice());
        purchase.setStatus(Purchase.Status.CREATED);
        purchase.setRazorpayOrderId("pending");
        purchase = purchaseRepository.save(purchase);

        String receipt = "rcpt_" + purchase.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", currency,
                "receipt", receipt
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForEntity(RAZORPAY_ORDERS_URL, entity, Map.class).getBody();
            if (response == null || response.get("id") == null) {
                throw new PaymentException("Could not create payment order. Please try again.");
            }
            String razorpayOrderId = response.get("id").toString();
            purchase.setRazorpayOrderId(razorpayOrderId);
            purchaseRepository.save(purchase);

            return new CreateOrderResponse(purchase.getId(), razorpayOrderId, amountInPaise, content.getPrice(), currency, keyId, false);
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception e) {
            log.error("Razorpay order creation failed", e);
            throw new PaymentException("Could not reach the payment gateway. Please try again.");
        }
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(Long userId, VerifyPaymentRequest req) {
        Purchase purchase = purchaseRepository.findById(req.purchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));

        if (!purchase.getUser().getId().equals(userId)) {
            throw new BadRequestException("This purchase does not belong to you.");
        }
        if (!purchase.getRazorpayOrderId().equals(req.razorpayOrderId())) {
            throw new BadRequestException("Order mismatch.");
        }

        boolean valid = verifySignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());

        purchase.setRazorpayPaymentId(req.razorpayPaymentId());
        purchase.setRazorpaySignature(req.razorpaySignature());
        purchase.setStatus(valid ? Purchase.Status.SUCCESS : Purchase.Status.FAILED);
        purchaseRepository.save(purchase);

        if (!valid) {
            throw new PaymentException("Payment verification failed.");
        }

        return new VerifyPaymentResponse(true, "Payment successful. Content unlocked.", purchase.getContent().getId());
    }

    // Bundles everything currently in the user's cart into ONE Razorpay order.
    // Items already owned are skipped from billing (and from the created
    // Purchase rows) but stay counted as "in the order" for the response.
    @Transactional
    public CreateCartOrderResponse createCartOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser_IdOrderByAddedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty.");
        }

        List<ContentItem> toCharge = new ArrayList<>();
        for (CartItem ci : cartItems) {
            boolean owned = purchaseRepository.existsByUser_IdAndContent_IdAndStatus(
                    userId, ci.getContent().getId(), Purchase.Status.SUCCESS);
            if (!owned) {
                toCharge.add(ci.getContent());
            }
        }
        if (toCharge.isEmpty()) {
            throw new BadRequestException("Everything in your cart is already unlocked.");
        }

        if (isPlaceholder(keyId) || isPlaceholder(keySecret)) {
            throw new PaymentException("Payment is not configured yet. Add your Razorpay keys in application.properties.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ContentItem c : toCharge) {
            totalAmount = totalAmount.add(c.getPrice());
        }
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();

        List<Purchase> purchases = new ArrayList<>();
        for (ContentItem c : toCharge) {
            Purchase p = new Purchase();
            p.setUser(user);
            p.setContent(c);
            p.setAmount(c.getPrice());
            p.setStatus(Purchase.Status.CREATED);
            p.setRazorpayOrderId("pending");
            purchases.add(purchaseRepository.save(p));
        }

        String receipt = "cart_" + userId + "_" + System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", currency,
                "receipt", receipt
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForEntity(RAZORPAY_ORDERS_URL, entity, Map.class).getBody();
            if (response == null || response.get("id") == null) {
                throw new PaymentException("Could not create payment order. Please try again.");
            }
            String razorpayOrderId = response.get("id").toString();
            for (Purchase p : purchases) {
                p.setRazorpayOrderId(razorpayOrderId);
                purchaseRepository.save(p);
            }
            return new CreateCartOrderResponse(razorpayOrderId, amountInPaise, totalAmount, currency, keyId, purchases.size());
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception e) {
            log.error("Razorpay cart order creation failed", e);
            throw new PaymentException("Could not reach the payment gateway. Please try again.");
        }
    }

    @Transactional
    public VerifyCartPaymentResponse verifyCartPayment(Long userId, VerifyCartPaymentRequest req) {
        List<Purchase> purchases = purchaseRepository.findAllByRazorpayOrderId(req.razorpayOrderId());
        if (purchases.isEmpty()) {
            throw new ResourceNotFoundException("Order not found.");
        }
        for (Purchase p : purchases) {
            if (!p.getUser().getId().equals(userId)) {
                throw new BadRequestException("This order does not belong to you.");
            }
        }

        boolean valid = verifySignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());

        for (Purchase p : purchases) {
            p.setRazorpayPaymentId(req.razorpayPaymentId());
            p.setRazorpaySignature(req.razorpaySignature());
            p.setStatus(valid ? Purchase.Status.SUCCESS : Purchase.Status.FAILED);
            purchaseRepository.save(p);
        }

        if (!valid) {
            throw new PaymentException("Payment verification failed.");
        }

        // Successful checkout — clear the cart (also drops any already-owned
        // items that were skipped from billing above).
        cartItemRepository.deleteByUser_Id(userId);

        return new VerifyCartPaymentResponse(true, "Payment successful. " + purchases.size() + " item(s) unlocked.", purchases.size());
    }

    @Transactional
    public void handleWebhook(String rawPayload, String signatureHeader) {
        if (isPlaceholder(webhookSecret) || signatureHeader == null) {
            return; // webhook not configured; rely on client-side verify flow only
        }
        try {
            String expected = hmacHex(webhookSecret, rawPayload);
            if (!expected.equalsIgnoreCase(signatureHeader)) {
                log.warn("Webhook signature mismatch");
                return;
            }
            // Best-effort: pull order id out of the payload and mark that purchase SUCCESS
            // if it isn't already, without failing the request on any parsing issue.
            String orderId = extractJsonValue(rawPayload, "\"order_id\"");
            if (orderId != null) {
                // Cart checkouts create multiple Purchase rows sharing one order id,
                // so this must update all of them, not assume exactly one match.
                for (Purchase p : purchaseRepository.findAllByRazorpayOrderId(orderId)) {
                    if (p.getStatus() != Purchase.Status.SUCCESS) {
                        p.setStatus(Purchase.Status.SUCCESS);
                        purchaseRepository.save(p);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
        }
    }

    public boolean hasAccess(Long userId, Long contentId) {
        return purchaseRepository.existsByUser_IdAndContent_IdAndStatus(userId, contentId, Purchase.Status.SUCCESS);
    }

    private boolean verifySignature(String orderId, String paymentId, String providedSignature) {
        String generated = hmacHex(keySecret, orderId + "|" + paymentId);
        return generated.equalsIgnoreCase(providedSignature);
    }

    private String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new PaymentException("Signature verification error.");
        }
    }

    private boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.startsWith("YOUR_");
    }

    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }
}
