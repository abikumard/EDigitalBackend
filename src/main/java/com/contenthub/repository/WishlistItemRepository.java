package com.contenthub.repository;

import com.contenthub.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser_IdOrderByCreatedAtDesc(Long userId);
    boolean existsByUser_IdAndContent_Id(Long userId, Long contentId);
    void deleteByUser_IdAndContent_Id(Long userId, Long contentId);
    long countByUser_Id(Long userId);
}
