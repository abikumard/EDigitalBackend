package com.contenthub.service;

import com.contenthub.dto.ContentDtos.CreateBookRequest;
import com.contenthub.dto.PayoutDtos.PayoutResponse;
import com.contenthub.dto.PayoutDtos.RequestPayoutRequest;
import com.contenthub.dto.SellerDtos.*;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.PayoutRequest;
import com.contenthub.entity.Purchase;
import com.contenthub.entity.Seller;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PayoutRequestRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.SellerRepository;
import com.contenthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final ContentItemRepository contentItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final PayoutRequestRepository payoutRequestRepository;

    public SellerService(SellerRepository sellerRepository,
                         UserRepository userRepository,
                         ContentItemRepository contentItemRepository,
                         PurchaseRepository purchaseRepository,
                         PayoutRequestRepository payoutRequestRepository) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.contentItemRepository = contentItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.payoutRequestRepository = payoutRequestRepository;
    }

    @Transactional
    public SellerStatusResponse apply(Long userId, SellerApplicationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Seller seller = sellerRepository.findByUser_Id(userId).orElseGet(() -> {
            Seller s = new Seller();
            s.setUser(user);
            return s;
        });

        seller.setBusinessName(req.businessName());
        seller.setAccountHolderName(req.accountHolderName());
        seller.setBankAccountNumber(req.bankAccountNumber());
        seller.setIfscCode(req.ifscCode());
        seller.setBankName(req.bankName());
        seller.setPanNumber(req.panNumber());
        seller.setPhone(req.phone());
        seller.setAddress(req.address());
        seller.setStatus("APPROVED");

        seller = sellerRepository.save(seller);
        return SellerStatusResponse.of(seller);
    }

    @Transactional(readOnly = true)
    public SellerStatusResponse myStatus(Long userId) {
        return sellerRepository.findByUser_Id(userId)
                .map(SellerStatusResponse::of)
                .orElse(new SellerStatusResponse("NONE", null, null, null, null));
    }

    @Transactional(readOnly = true)
    public List<AdminSellerResponse> adminListAll() {
        return sellerRepository.findAll().stream()
                .map(AdminSellerResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminSellerResponse adminApprove(Long id) {
        Seller s = sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        s.setStatus("APPROVED");
        s.setReviewedAt(LocalDateTime.now());
        s.setRejectionReason(null);
        return AdminSellerResponse.of(sellerRepository.save(s));
    }

    @Transactional
    public AdminSellerResponse adminReject(Long id, String reason) {
        Seller s = sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        s.setStatus("REJECTED");
        s.setReviewedAt(LocalDateTime.now());
        s.setRejectionReason(reason);
        return AdminSellerResponse.of(sellerRepository.save(s));
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getProfile(Long userId) {
        Seller seller = getOrCreateSellerForUser(userId);
        return SellerProfileResponse.of(seller);
    }

    @Transactional
    public SellerProfileResponse updateProfile(Long userId, SellerApplyRequest req) {
        Seller seller = getOrCreateSellerForUser(userId);
        if (req.businessName() != null) seller.setBusinessName(req.businessName());
        if (req.penName() != null) seller.setPenName(req.penName());
        if (req.bio() != null) seller.setBio(req.bio());
        if (req.accountHolderName() != null) seller.setAccountHolderName(req.accountHolderName());
        if (req.bankAccountNumber() != null) seller.setBankAccountNumber(req.bankAccountNumber());
        if (req.ifscCode() != null) seller.setIfscCode(req.ifscCode());
        if (req.bankName() != null) seller.setBankName(req.bankName());
        if (req.upiId() != null) seller.setUpiId(req.upiId());
        if (req.panNumber() != null) seller.setPanNumber(req.panNumber());
        if (req.phone() != null) seller.setPhone(req.phone());
        if (req.address() != null) seller.setAddress(req.address());
        seller.setStatus("APPROVED");

        seller = sellerRepository.save(seller);
        return SellerProfileResponse.of(seller);
    }

    @Transactional(readOnly = true)
    public PublisherAnalyticsResponse getAnalytics(Long userId) {
        Seller seller = getOrCreateSellerForUser(userId);
        List<Purchase> sales = purchaseRepository.findBySeller_IdAndStatus(seller.getId(), Purchase.Status.SUCCESS);

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal platformCommission = BigDecimal.ZERO;
        BigDecimal netAuthorEarnings = BigDecimal.ZERO;

        for (Purchase p : sales) {
            grossRevenue = grossRevenue.add(p.getAmount());
            BigDecimal fee = p.getPlatformFee() != null ? p.getPlatformFee() : p.getAmount().multiply(new BigDecimal("0.03"));
            BigDecimal roy = p.getPublisherRoyalty() != null ? p.getPublisherRoyalty() : p.getAmount().subtract(fee);
            platformCommission = platformCommission.add(fee);
            netAuthorEarnings = netAuthorEarnings.add(roy);
        }

        return new PublisherAnalyticsResponse(
                grossRevenue,
                platformCommission,
                netAuthorEarnings,
                sales.size(),
                seller.getAvailableBalance()
        );
    }

    @Transactional(readOnly = true)
    public List<ContentItem> getBookshelf(Long userId) {
        Seller seller = getOrCreateSellerForUser(userId);
        return contentItemRepository.findBySeller_IdOrderByCreatedAtDesc(seller.getId());
    }

    @Transactional
    public ContentItem publishBook(Long userId, CreateBookRequest req, String coverUrl, String manuscriptUrl) {
        Seller seller = getOrCreateSellerForUser(userId);

        ContentItem book = new ContentItem();
        book.setSeller(seller);
        book.setTitle(req.title());
        book.setSubtitle(req.subtitle());
        book.setAuthorName(req.authorName() != null && !req.authorName().isBlank() ? req.authorName() : seller.getPenName());
        book.setDescription(req.description());
        book.setPrice(req.price() != null ? req.price() : new BigDecimal("299.00"));
        book.setPaperbackPrice(req.paperbackPrice());
        book.setCategory(req.category() != null ? req.category() : "AI & Technology");
        book.setKeywords(req.keywords());
        book.setLanguage(req.language() != null ? req.language() : "English");
        book.setPrintLength(req.printLength() != null ? req.printLength() : 33);
        book.setSampleText(req.sampleText());
        book.setContentType(ContentItem.ContentType.EBOOK);
        book.setThumbnailPath(coverUrl != null ? coverUrl : "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80");
        book.setFilePath(manuscriptUrl != null ? manuscriptUrl : "https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/default_manuscript.pdf");
        book.setStatus("LIVE");
        book.setActive(true);

        return contentItemRepository.save(book);
    }

    @Transactional
    public PayoutResponse requestPayout(Long userId, RequestPayoutRequest req) {
        Seller seller = getOrCreateSellerForUser(userId);
        if (req.amount().compareTo(BigDecimal.TEN) < 0) {
            throw new BadRequestException("Minimum payout withdrawal is ₹100.00");
        }
        if (seller.getAvailableBalance().compareTo(req.amount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance. Available: ₹" + seller.getAvailableBalance());
        }

        PayoutRequest pr = new PayoutRequest();
        pr.setSeller(seller);
        pr.setAmount(req.amount());
        pr.setPayoutMethod(req.payoutMethod() != null ? req.payoutMethod() : "BANK_TRANSFER");
        pr.setPayoutDetails(req.payoutDetails() != null ? req.payoutDetails() :
                "A/C: " + seller.getBankAccountNumber() + " | IFSC: " + seller.getIfscCode() + " | UPI: " + seller.getUpiId());
        pr.setStatus("PENDING");

        seller.setAvailableBalance(seller.getAvailableBalance().subtract(req.amount()));
        sellerRepository.save(seller);

        pr = payoutRequestRepository.save(pr);

        return new PayoutResponse(
                pr.getId(),
                pr.getAmount(),
                pr.getPayoutMethod(),
                pr.getPayoutDetails(),
                pr.getStatus(),
                pr.getTransactionRef(),
                pr.getRequestedAt(),
                pr.getProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayoutHistory(Long userId) {
        Seller seller = getOrCreateSellerForUser(userId);
        return payoutRequestRepository.findBySeller_IdOrderByRequestedAtDesc(seller.getId())
                .stream()
                .map(pr -> new PayoutResponse(
                        pr.getId(),
                        pr.getAmount(),
                        pr.getPayoutMethod(),
                        pr.getPayoutDetails(),
                        pr.getStatus(),
                        pr.getTransactionRef(),
                        pr.getRequestedAt(),
                        pr.getProcessedAt()
                ))
                .collect(Collectors.toList());
    }

    public Seller getOrCreateSellerForUser(Long userId) {
        return sellerRepository.findByUser_Id(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Seller s = new Seller();
            s.setUser(user);
            s.setBusinessName(user.getName() + " Publishing");
            s.setPenName(user.getName());
            s.setBio("Independent author and publisher on KDP Cloud.");
            s.setAccountHolderName(user.getName());
            s.setBankAccountNumber("XXXX-XXXX-XXXX");
            s.setIfscCode("SBIN0001234");
            s.setBankName("State Bank of India");
            s.setPanNumber("ABCDE1234F");
            s.setPhone("9876543210");
            s.setAddress("Digital Publisher Studio");
            s.setStatus("APPROVED");
            return sellerRepository.save(s);
        });
    }
}
