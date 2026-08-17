package com.contenthub.dto;

import com.contenthub.entity.ContentItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ContentDtos {

    public record ContentFileResponse(
            Long id,
            String fileType,
            String label,
            String url
    ) {}

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
            LocalDateTime purchasedAt,
            List<ContentFileResponse> extraFiles
    ) {
        public static ContentResponse of(ContentItem c, boolean purchased) {
            return of(c, purchased, null, List.of());
        }

        public static ContentResponse of(ContentItem c, boolean purchased, LocalDateTime purchasedAt) {
            return of(c, purchased, purchasedAt, List.of());
        }

        public static ContentResponse of(ContentItem c, boolean purchased, LocalDateTime purchasedAt,
                                          List<ContentFileResponse> extraFiles) {
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
                    purchasedAt,
                    extraFiles
            );
        }
    }
}
