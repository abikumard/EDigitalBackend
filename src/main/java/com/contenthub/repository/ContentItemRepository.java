package com.contenthub.repository;

import com.contenthub.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, Long> {
    List<ContentItem> findByActiveTrueOrderByCreatedAtDesc();
    List<ContentItem> findAllByOrderByCreatedAtDesc();
    List<ContentItem> findBySeller_IdOrderByCreatedAtDesc(Long sellerId);
}
