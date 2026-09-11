package com.contenthub.dto;

import java.time.LocalDateTime;

public class ReviewDtos {

    public record CreateReviewRequest(
            Long contentId,
            Integer rating,
            String headline,
            String comment
    ) {}

    public record ReviewResponse(
            Long id,
            String userName,
            Integer rating,
            String headline,
            String comment,
            boolean verifiedPurchase,
            LocalDateTime createdAt
    ) {}
}
