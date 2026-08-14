package com.contenthub.controller;

import com.contenthub.dto.CartDtos.CartItemResponse;
import com.contenthub.dto.CartDtos.CartResponse;
import com.contenthub.dto.CommonDtos.MessageResponse;
import com.contenthub.entity.CartItem;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.CartItemRepository;
import com.contenthub.repository.UserRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import com.contenthub.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user/cart")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ContentService contentService;
    private final PaymentService paymentService;

    public CartController(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           ContentService contentService,
                           PaymentService paymentService) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.contentService = contentService;
        this.paymentService = paymentService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<CartResponse> myCart(Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        return ResponseEntity.ok(buildCartResponse(principal.getUserId()));
    }

    @PostMapping("/{contentId}")
    @Transactional
    public ResponseEntity<CartResponse> add(@PathVariable Long contentId, Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        if (!cartItemRepository.existsByUser_IdAndContent_Id(principal.getUserId(), contentId)) {
            User user = userRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            ContentItem content = contentService.getEntityOrThrow(contentId);
            CartItem item = new CartItem();
            item.setUser(user);
            item.setContent(content);
            cartItemRepository.save(item);
        }
        return ResponseEntity.ok(buildCartResponse(principal.getUserId()));
    }

    @DeleteMapping("/{contentId}")
    @Transactional
    public ResponseEntity<CartResponse> remove(@PathVariable Long contentId, Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        cartItemRepository.deleteByUser_IdAndContent_Id(principal.getUserId(), contentId);
        return ResponseEntity.ok(buildCartResponse(principal.getUserId()));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<MessageResponse> clear(Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        cartItemRepository.deleteByUser_Id(principal.getUserId());
        return ResponseEntity.ok(new MessageResponse("Cart cleared."));
    }

    private CartResponse buildCartResponse(Long userId) {
        List<CartItemResponse> responses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem c : cartItemRepository.findByUser_IdOrderByAddedAtDesc(userId)) {
            ContentItem content = c.getContent();
            boolean owned = paymentService.hasAccess(userId, content.getId());
            responses.add(new CartItemResponse(
                    content.getId(),
                    content.getTitle(),
                    content.getContentType().name(),
                    "/api/content/" + content.getId() + "/thumbnail",
                    content.getPrice(),
                    owned
            ));
            if (!owned) {
                total = total.add(content.getPrice());
            }
        }
        return new CartResponse(responses, responses.size(), total);
    }

    private AuthPrincipal requireUser(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal;
    }
}
