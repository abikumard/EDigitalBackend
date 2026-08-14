package com.contenthub.dto;

import com.contenthub.entity.ContentItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContentDtos {

    public record ContentResponse(
            Long id,
            String title,
            String description,
            BigDecimal price,
            String contentType,
            String thumbnailUrl,
            boolean active,
            boolean purchased,
            LocalDateTime createdAt,
            LocalDateTime purchasedAt
    ) {
        public static ContentResponse of(ContentItem c, boolean purchased) {
            return of(c, purchased, null);
        }

        public static ContentResponse of(ContentItem c, boolean purchased, LocalDateTime purchasedAt) {
            return new ContentResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getDescription(),
                    c.getPrice(),
                    c.getContentType().name(),
                    "/api/content/" + c.getId() + "/thumbnail",
                    c.isActive(),
                    purchased,
                    c.getCreatedAt(),
                    purchasedAt
            );
        }
    }
}
