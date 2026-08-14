package com.contenthub.repository;

import com.contenthub.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByUser_IdAndContent_IdAndStatus(Long userId, Long contentId, Purchase.Status status);

    Optional<Purchase> findByRazorpayOrderId(String razorpayOrderId);

    List<Purchase> findAllByRazorpayOrderId(String razorpayOrderId);

    List<Purchase> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, Purchase.Status status);

    List<Purchase> findAllByStatusOrderByCreatedAtDesc(Purchase.Status status);

    long countByStatus(Purchase.Status status);

    @Query("select coalesce(sum(p.amount), 0) from Purchase p where p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") Purchase.Status status);

    @Query("select count(p) from Purchase p where p.user.id = :userId and p.status = :status")
    long countByUserAndStatus(@Param("userId") Long userId, @Param("status") Purchase.Status status);

    List<Purchase> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
