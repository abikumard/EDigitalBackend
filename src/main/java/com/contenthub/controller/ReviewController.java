package com.contenthub.controller;

import com.contenthub.dto.ReviewDtos.CreateReviewRequest;
import com.contenthub.dto.ReviewDtos.ReviewResponse;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Review;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.ReviewRepository;
import com.contenthub.repository.UserRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ContentItemRepository contentItemRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository,
                            ContentItemRepository contentItemRepository,
                            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.contentItemRepository = contentItemRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/book/{contentId}")
    public ResponseEntity<List<ReviewResponse>> getBookReviews(@PathVariable Long contentId) {
        List<Review> reviews = reviewRepository.findByContentItem_IdOrderByCreatedAtDesc(contentId);
        return ResponseEntity.ok(reviews.stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getUser().getName(),
                        r.getRating(),
                        r.getHeadline(),
                        r.getComment(),
                        r.isVerifiedPurchase(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            Authentication authentication,
            @RequestBody CreateReviewRequest req) {
        Long currentUserId = requireUserId(authentication);

        User user = userRepository.findById(currentUserId).orElseThrow();
        ContentItem book = contentItemRepository.findById(req.contentId()).orElseThrow();

        Review r = new Review();
        r.setUser(user);
        r.setContentItem(book);
        r.setRating(req.rating() != null ? req.rating() : 5);
        r.setHeadline(req.headline());
        r.setComment(req.comment());
        r.setVerifiedPurchase(true);
        r = reviewRepository.save(r);

        return ResponseEntity.ok(new ReviewResponse(
                r.getId(),
                user.getName(),
                r.getRating(),
                r.getHeadline(),
                r.getComment(),
                r.isVerifiedPurchase(),
                r.getCreatedAt()
        ));
    }

    private Long requireUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal.getUserId();
    }
}
