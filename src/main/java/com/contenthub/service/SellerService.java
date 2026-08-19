package com.contenthub.service;

import com.contenthub.dto.SellerDtos.AdminSellerResponse;
import com.contenthub.dto.SellerDtos.SellerApplicationRequest;
import com.contenthub.dto.SellerDtos.SellerStatusResponse;
import com.contenthub.entity.Seller;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.SellerRepository;
import com.contenthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    public SellerService(SellerRepository sellerRepository, UserRepository userRepository) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SellerStatusResponse apply(Long userId, SellerApplicationRequest req) {
        if (sellerRepository.existsByUser_Id(userId)) {
            throw new BadRequestException("You've already submitted a seller application.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Seller seller = new Seller();
        seller.setUser(user);
        seller.setBusinessName(req.businessName().trim());
        seller.setAccountHolderName(req.accountHolderName().trim());
        seller.setBankAccountNumber(req.bankAccountNumber().trim());
        seller.setIfscCode(req.ifscCode().trim().toUpperCase());
        seller.setBankName(req.bankName().trim());
        seller.setPanNumber(req.panNumber().trim().toUpperCase());
        seller.setPhone(req.phone().trim());
        seller.setAddress(req.address().trim());
        seller.setStatus("PENDING");

        Seller saved = sellerRepository.save(seller);
        return toStatusResponse(saved);
    }

    public SellerStatusResponse myStatus(Long userId) {
        return sellerRepository.findByUser_Id(userId)
                .map(this::toStatusResponse)
                .orElse(new SellerStatusResponse(false, null, null, null));
    }

    public List<AdminSellerResponse> adminListAll() {
        List<AdminSellerResponse> list = new ArrayList<>();
        for (Seller s : sellerRepository.findAllByOrderByAppliedAtDesc()) {
            list.add(toAdminResponse(s));
        }
        return list;
    }

    @Transactional
    public AdminSellerResponse adminApprove(Long sellerId) {
        Seller s = getOrThrow(sellerId);
        s.setStatus("APPROVED");
        s.setReviewedAt(LocalDateTime.now());
        s.setRejectionReason(null);
        return toAdminResponse(sellerRepository.save(s));
    }

    @Transactional
    public AdminSellerResponse adminReject(Long sellerId, String reason) {
        Seller s = getOrThrow(sellerId);
        s.setStatus("REJECTED");
        s.setReviewedAt(LocalDateTime.now());
        s.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        return toAdminResponse(sellerRepository.save(s));
    }

    private Seller getOrThrow(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller application not found"));
    }

    private SellerStatusResponse toStatusResponse(Seller s) {
        return new SellerStatusResponse(true, s.getStatus(), s.getBusinessName(), s.getRejectionReason());
    }

    private AdminSellerResponse toAdminResponse(Seller s) {
        return new AdminSellerResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getEmail(),
                s.getUser().getMobile(),
                s.getBusinessName(),
                s.getAccountHolderName(),
                s.getBankAccountNumber(),
                s.getIfscCode(),
                s.getBankName(),
                s.getPanNumber(),
                s.getPhone(),
                s.getAddress(),
                s.getStatus(),
                s.getAppliedAt(),
                s.getReviewedAt(),
                s.getRejectionReason()
        );
    }
}
