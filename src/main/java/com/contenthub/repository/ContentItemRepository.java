package com.contenthub.repository;

import com.contenthub.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {

    List<ContentItem> findByActiveTrueOrderByCreatedAtDesc();

    List<ContentItem> findByActiveTrueAndCategoryIgnoreCaseOrderByCreatedAtDesc(String category);

    List<ContentItem> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT c FROM ContentItem c WHERE c.active = true AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.subtitle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.authorName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.keywords) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ContentItem> searchBooks(@Param("query") String query);
}
