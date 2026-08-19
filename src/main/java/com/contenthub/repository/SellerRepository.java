package com.contenthub.repository;

import com.contenthub.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByUser_Id(Long userId);
    boolean existsByUser_Id(Long userId);
    List<Seller> findAllByOrderByAppliedAtDesc();
}
