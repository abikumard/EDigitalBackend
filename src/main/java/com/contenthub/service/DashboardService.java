package com.contenthub.service;

import com.contenthub.dto.AdminDtos.AdminPurchaseResponse;
import com.contenthub.dto.AdminDtos.AdminUserResponse;
import com.contenthub.dto.AdminDtos.DashboardStatsResponse;
import com.contenthub.entity.Purchase;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Purchase.user and Purchase.content are lazy-loaded (see Purchase.java). Every
// method here reads through one of those, so the class needs an open Hibernate
// session for its whole call — hence @Transactional(readOnly = true) at class level,
// the same pattern PaymentService already uses around the same entities.
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ContentItemRepository contentItemRepository;

    public DashboardService(UserRepository userRepository,
                             PurchaseRepository purchaseRepository,
                             ContentItemRepository contentItemRepository) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.contentItemRepository = contentItemRepository;
    }

    public DashboardStatsResponse stats() {
        long totalUsers = userRepository.count();
        long totalPurchases = purchaseRepository.countByStatus(Purchase.Status.SUCCESS);
        var totalRevenue = purchaseRepository.sumAmountByStatus(Purchase.Status.SUCCESS);
        long totalContentItems = contentItemRepository.count();
        return new DashboardStatsResponse(totalUsers, totalPurchases, totalRevenue, totalContentItems);
    }

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> toAdminUserResponse(u, false))
                .toList();
    }

    public AdminUserResponse userDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAdminUserResponse(user, true);
    }

    public List<AdminPurchaseResponse> allPurchases() {
        return purchaseRepository.findAllByStatusOrderByCreatedAtDesc(Purchase.Status.SUCCESS).stream()
                .map(this::toAdminPurchaseResponse)
                .toList();
    }

    private AdminUserResponse toAdminUserResponse(User u, boolean includePurchases) {
        long count = purchaseRepository.countByUserAndStatus(u.getId(), Purchase.Status.SUCCESS);
        List<AdminPurchaseResponse> purchases = includePurchases
                ? purchaseRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(u.getId(), Purchase.Status.SUCCESS)
                    .stream().map(this::toAdminPurchaseResponse).toList()
                : null;
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getMobile(), u.getName(), u.getCreatedAt(), u.getLastLoginAt(), count, purchases);
    }

    private AdminPurchaseResponse toAdminPurchaseResponse(Purchase p) {
        String userIdentifier = p.getUser().getEmail() != null ? p.getUser().getEmail() : p.getUser().getMobile();
        return new AdminPurchaseResponse(
                p.getId(),
                userIdentifier,
                p.getContent().getId(),
                p.getContent().getTitle(),
                p.getContent().getContentType().name(),
                p.getAmount(),
                p.getStatus().name(),
                p.getCreatedAt()
        );
    }
}
