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
            List<ContentFileResponse> extraFiles,
            String sellerName
    ) {
        public static ContentResponse of(ContentItem c, boolean purchased) {
            return of(c, purchased, null, List.of());
        }

        public static ContentResponse of(ContentItem c, boolean purchased, LocalDateTime purchasedAt) {
            return of(c, purchased, purchasedAt, List.of());
        }

        public static ContentResponse of(ContentItem c, boolean purchased, LocalDateTime purchasedAt,
                                          List<ContentFileResponse> extraFiles) {
            // c.getSeller() is a lazy relation — callers building this from a
            // list/detail read path must run inside @Transactional(readOnly = true)
            // (see ContentService) or this throws LazyInitializationException.
            String sellerName = c.getSeller() != null ? c.getSeller().getBusinessName() : null;
            return new ContentResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getDescription(),
                    c.getPrice(),
                    c.getContentType().name(),
                    // Now a full Cloudinary URL stored directly on the entity —
                    // no more local-disk indirection through this backend.
                    c.getThumbnailPath(),
                    c.isActive(),
                    purchased,
                    c.getCreatedAt(),
                    purchasedAt,
                    extraFiles,
                    sellerName
            );
        }
    }
}
