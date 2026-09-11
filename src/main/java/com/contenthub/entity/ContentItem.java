package com.contenthub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_items")
public class ContentItem {

    public enum ContentType { VIDEO, PDF, PHOTO, EBOOK }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "author_name", length = 200)
    private String authorName = "Abikumar Dharmaraj";

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "paperback_price", precision = 10, scale = 2)
    private BigDecimal paperbackPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType = ContentType.EBOOK;

    @Column(name = "thumbnail_path", nullable = false, length = 500)
    private String thumbnailPath;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(length = 100)
    private String category = "AI & Technology";

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(length = 50)
    private String isbn;

    @Column(length = 50)
    private String language = "English";

    @Column(name = "print_length")
    private Integer printLength = 33;

    @Column(name = "sample_text", columnDefinition = "LONGTEXT")
    private String sampleText;

    @Column(length = 30)
    private String status = "LIVE"; // LIVE, IN_REVIEW, DRAFT, UNPUBLISHED

    @Column(name = "royalty_rate", precision = 5, scale = 2)
    private BigDecimal royaltyRate = new BigDecimal("97.00");

    @Column(name = "average_rating")
    private Double averageRating = 4.9;

    @Column(name = "review_count")
    private Integer reviewCount = 24;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "LIVE";
        if (this.authorName == null) this.authorName = "Abikumar Dharmaraj";
        if (this.category == null) this.category = "AI & Technology";
        if (this.averageRating == null) this.averageRating = 4.9;
        if (this.reviewCount == null) this.reviewCount = 24;
        if (this.royaltyRate == null) this.royaltyRate = new BigDecimal("97.00");
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getPaperbackPrice() { return paperbackPrice; }
    public void setPaperbackPrice(BigDecimal paperbackPrice) { this.paperbackPrice = paperbackPrice; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getPrintLength() { return printLength; }
    public void setPrintLength(Integer printLength) { this.printLength = printLength; }

    public String getSampleText() { return sampleText; }
    public void setSampleText(String sampleText) { this.sampleText = sampleText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getRoyaltyRate() { return royaltyRate; }
    public void setRoyaltyRate(BigDecimal royaltyRate) { this.royaltyRate = royaltyRate; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
}
