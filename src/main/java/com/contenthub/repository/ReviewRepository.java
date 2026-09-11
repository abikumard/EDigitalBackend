package com.contenthub.repository;

import com.contenthub.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByContentItem_IdOrderByCreatedAtDesc(Long contentId);
    long countByContentItem_Id(Long contentId);
}
