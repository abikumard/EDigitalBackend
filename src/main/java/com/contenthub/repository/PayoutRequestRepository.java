package com.contenthub.repository;

import com.contenthub.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    List<PayoutRequest> findBySeller_IdOrderByRequestedAtDesc(Long sellerId);
    List<PayoutRequest> findByStatusOrderByRequestedAtDesc(String status);
}
