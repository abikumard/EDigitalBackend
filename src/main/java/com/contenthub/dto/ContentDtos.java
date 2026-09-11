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
            String subtitle,
            String authorName,
            String description,
            BigDecimal price,
            BigDecimal paperbackPrice,
            String contentType,
            String thumbnailUrl,
            String filePath,
            String category,
            String keywords,
            String isbn,
            String language,
            Integer printLength,
            String status,
            BigDecimal royaltyRate,
            Double averageRating,
            Integer reviewCount,
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
            String sellerName = c.getSeller() != null ? c.getSeller().getBusinessName() : c.getAuthorName();
            return new ContentResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getSubtitle(),
                    c.getAuthorName() != null ? c.getAuthorName() : "Abikumar Dharmaraj",
                    c.getDescription(),
                    c.getPrice(),
                    c.getPaperbackPrice(),
                    c.getContentType().name(),
                    c.getThumbnailPath(),
                    purchased ? c.getFilePath() : null,
                    c.getCategory(),
                    c.getKeywords(),
                    c.getIsbn(),
                    c.getLanguage(),
                    c.getPrintLength(),
                    c.getStatus(),
                    c.getRoyaltyRate(),
                    c.getAverageRating() != null ? c.getAverageRating() : 4.9,
                    c.getReviewCount() != null ? c.getReviewCount() : 24,
                    c.isActive(),
                    purchased,
                    c.getCreatedAt(),
                    purchasedAt,
                    extraFiles,
                    sellerName
            );
        }
    }

    public record SamplePreviewResponse(
            Long id,
            String title,
            String subtitle,
            String authorName,
            String thumbnailUrl,
            String category,
            Integer printLength,
            String sampleText
    ) {}

    public record CreateBookRequest(
            String title,
            String subtitle,
            String authorName,
            String description,
            BigDecimal price,
            BigDecimal paperbackPrice,
            String category,
            String keywords,
            String language,
            Integer printLength,
            String sampleText
    ) {}
}
