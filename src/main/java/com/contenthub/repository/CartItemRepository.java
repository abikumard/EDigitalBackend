package com.contenthub.repository;

import com.contenthub.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser_IdOrderByAddedAtDesc(Long userId);
    boolean existsByUser_IdAndContent_Id(Long userId, Long contentId);
    void deleteByUser_IdAndContent_Id(Long userId, Long contentId);
    void deleteByUser_Id(Long userId);
    void deleteByContent_Id(Long contentId);
    long countByUser_Id(Long userId);
}
