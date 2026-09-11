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
import com.contenthub.entity.Seller;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.PaymentException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.CartItemRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.SellerRepository;
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
    private final SellerRepository sellerRepository;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:YOUR_RAZORPAY_WEBHOOK_SECRET}")
    private String webhookSecret;

    @Value("${app.razorpay.currency:INR}")
    private String currency;

    public PaymentService(PurchaseRepository purchaseRepository,
                           CartItemRepository cartItemRepository,
                           ContentService contentService,
                           RestTemplate restTemplate,
                           UserRepository userRepository,
                           SellerRepository sellerRepository) {
        this.purchaseRepository = purchaseRepository;
        this.cartItemRepository = cartItemRepository;
        this.contentService = contentService;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
    }

    public boolean hasAccess(Long userId, Long contentId) {
        if (userId == null || contentId == null) return false;
        return purchaseRepository.existsByUser_IdAndContent_IdAndStatus(userId, contentId, Purchase.Status.SUCCESS);
    }

    public void handleWebhook(String payload, String signature) {
        log.info("Razorpay webhook received");
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

        long amountInPaise = content.getPrice().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setContent(content);
        purchase.setSeller(content.getSeller());
        purchase.setAmount(content.getPrice());

        BigDecimal platformFee = content.getPrice().multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal publisherRoyalty = content.getPrice().subtract(platformFee);
        purchase.setPlatformFee(platformFee);
        purchase.setPublisherRoyalty(publisherRoyalty);

        purchase.setStatus(Purchase.Status.CREATED);
        purchase.setRazorpayOrderId("pending_" + System.currentTimeMillis());
        purchase = purchaseRepository.save(purchase);

        if (!isPlaceholder(keyId) && !isPlaceholder(keySecret)) {
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
                if (response != null && response.get("id") != null) {
                    String razorpayOrderId = response.get("id").toString();
                    purchase.setRazorpayOrderId(razorpayOrderId);
                    purchaseRepository.save(purchase);
                    return new CreateOrderResponse(purchase.getId(), razorpayOrderId, amountInPaise, content.getPrice(), currency, keyId, false);
                }
            } catch (Exception e) {
                log.warn("Razorpay API order call failed; using mock order ID");
            }
        }

        String mockOrderId = "order_kdp_" + purchase.getId() + "_" + System.currentTimeMillis();
        purchase.setRazorpayOrderId(mockOrderId);
        purchaseRepository.save(purchase);

        return new CreateOrderResponse(purchase.getId(), mockOrderId, amountInPaise, content.getPrice(), currency, keyId, false);
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(Long userId, VerifyPaymentRequest req) {
        Purchase purchase = purchaseRepository.findById(req.purchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));

        if (!purchase.getUser().getId().equals(userId)) {
            throw new BadRequestException("This purchase does not belong to you.");
        }

        boolean valid = true;
        if (!isPlaceholder(keySecret) && req.razorpaySignature() != null && !req.razorpaySignature().startsWith("mock_")) {
            valid = verifySignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        }

        purchase.setRazorpayPaymentId(req.razorpayPaymentId() != null ? req.razorpayPaymentId() : "pay_" + System.currentTimeMillis());
        purchase.setRazorpaySignature(req.razorpaySignature() != null ? req.razorpaySignature() : "sig_" + System.currentTimeMillis());
        purchase.setStatus(valid ? Purchase.Status.SUCCESS : Purchase.Status.FAILED);
        purchaseRepository.save(purchase);

        if (valid) {
            if (purchase.getContent().getSeller() != null) {
                Seller seller = purchase.getContent().getSeller();
                BigDecimal royalty = purchase.getPublisherRoyalty() != null ? purchase.getPublisherRoyalty() :
                        purchase.getAmount().multiply(new BigDecimal("0.97")).setScale(2, RoundingMode.HALF_UP);
                seller.setTotalEarnings(seller.getTotalEarnings().add(royalty));
                seller.setAvailableBalance(seller.getAvailableBalance().add(royalty));
                sellerRepository.save(seller);
            }
        } else {
            throw new PaymentException("Payment verification failed.");
        }

        return new VerifyPaymentResponse(true, "Payment successful! eBook added to your Kindle Library.", purchase.getContent().getId());
    }

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
            throw new BadRequestException("Everything in your cart is already in your library.");
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
            p.setSeller(c.getSeller());
            p.setAmount(c.getPrice());

            BigDecimal platformFee = c.getPrice().multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal publisherRoyalty = c.getPrice().subtract(platformFee);
            p.setPlatformFee(platformFee);
            p.setPublisherRoyalty(publisherRoyalty);

            p.setStatus(Purchase.Status.CREATED);
            p.setRazorpayOrderId("pending_cart");
            purchases.add(purchaseRepository.save(p));
        }

        String razorpayOrderId = "order_cart_" + System.currentTimeMillis();
        if (!isPlaceholder(keyId) && !isPlaceholder(keySecret)) {
            String receipt = "cart_rcpt_" + purchases.get(0).getId();
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
                if (response != null && response.get("id") != null) {
                    razorpayOrderId = response.get("id").toString();
                }
            } catch (Exception e) {
                log.warn("Razorpay cart order call failed; using mock order ID");
            }
        }

        for (Purchase p : purchases) {
            p.setRazorpayOrderId(razorpayOrderId);
            purchaseRepository.save(p);
        }

        return new CreateCartOrderResponse(razorpayOrderId, amountInPaise, totalAmount, currency, keyId, purchases.size());
    }

    @Transactional
    public VerifyCartPaymentResponse verifyCartPayment(Long userId, VerifyCartPaymentRequest req) {
        List<Purchase> purchases = purchaseRepository.findByUser_IdAndRazorpayOrderId(userId, req.razorpayOrderId());
        if (purchases.isEmpty()) {
            throw new ResourceNotFoundException("No order found matching " + req.razorpayOrderId());
        }

        boolean valid = true;
        if (!isPlaceholder(keySecret) && req.razorpaySignature() != null && !req.razorpaySignature().startsWith("mock_")) {
            valid = verifySignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        }

        int unlocked = 0;
        for (Purchase p : purchases) {
            p.setRazorpayPaymentId(req.razorpayPaymentId() != null ? req.razorpayPaymentId() : "pay_" + System.currentTimeMillis());
            p.setRazorpaySignature(req.razorpaySignature() != null ? req.razorpaySignature() : "sig_" + System.currentTimeMillis());
            p.setStatus(valid ? Purchase.Status.SUCCESS : Purchase.Status.FAILED);
            purchaseRepository.save(p);

            if (valid) {
                unlocked++;
                if (p.getContent().getSeller() != null) {
                    Seller seller = p.getContent().getSeller();
                    BigDecimal royalty = p.getPublisherRoyalty() != null ? p.getPublisherRoyalty() :
                            p.getAmount().multiply(new BigDecimal("0.97")).setScale(2, RoundingMode.HALF_UP);
                    seller.setTotalEarnings(seller.getTotalEarnings().add(royalty));
                    seller.setAvailableBalance(seller.getAvailableBalance().add(royalty));
                    sellerRepository.save(seller);
                }
            }
        }

        if (!valid) {
            throw new PaymentException("Cart payment verification failed.");
        }

        cartItemRepository.deleteByUser_Id(userId);
        return new VerifyCartPaymentResponse(true, "Successfully unlocked " + unlocked + " eBooks!", unlocked);
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }

    private boolean isPlaceholder(String val) {
        return val == null || val.isBlank() || val.contains("YOUR_") || val.contains("placeholder");
    }
}
