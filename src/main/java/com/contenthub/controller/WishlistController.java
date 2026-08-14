package com.contenthub.controller;

import com.contenthub.dto.CommonDtos.MessageResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.User;
import com.contenthub.entity.WishlistItem;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.UserRepository;
import com.contenthub.repository.WishlistItemRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user/wishlist")
public class WishlistController {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ContentService contentService;

    public WishlistController(WishlistItemRepository wishlistItemRepository,
                               UserRepository userRepository,
                               ContentService contentService) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.contentService = contentService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ContentResponse>> myWishlist(Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        List<ContentResponse> items = new ArrayList<>();
        for (WishlistItem w : wishlistItemRepository.findByUser_IdOrderByCreatedAtDesc(principal.getUserId())) {
            items.add(ContentResponse.of(w.getContent(), false));
        }
        return ResponseEntity.ok(items);
    }

    // Cheap membership check so browse-grid cards can show a filled/outline
    // heart without fetching each item's full details.
    @GetMapping("/ids")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Long>> myWishlistIds(Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        List<Long> ids = new ArrayList<>();
        for (WishlistItem w : wishlistItemRepository.findByUser_IdOrderByCreatedAtDesc(principal.getUserId())) {
            ids.add(w.getContent().getId());
        }
        return ResponseEntity.ok(ids);
    }

    @PostMapping("/{contentId}")
    @Transactional
    public ResponseEntity<MessageResponse> add(@PathVariable Long contentId, Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        if (wishlistItemRepository.existsByUser_IdAndContent_Id(principal.getUserId(), contentId)) {
            return ResponseEntity.ok(new MessageResponse("Already in your wishlist."));
        }
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ContentItem content = contentService.getEntityOrThrow(contentId);

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setContent(content);
        wishlistItemRepository.save(item);
        return ResponseEntity.ok(new MessageResponse("Added to your wishlist."));
    }

    @DeleteMapping("/{contentId}")
    @Transactional
    public ResponseEntity<MessageResponse> remove(@PathVariable Long contentId, Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        wishlistItemRepository.deleteByUser_IdAndContent_Id(principal.getUserId(), contentId);
        return ResponseEntity.ok(new MessageResponse("Removed from your wishlist."));
    }

    private AuthPrincipal requireUser(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal;
    }
}
