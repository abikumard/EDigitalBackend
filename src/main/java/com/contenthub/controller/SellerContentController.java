package com.contenthub.controller;

import com.contenthub.dto.CommonDtos.MessageResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Seller;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.SellerRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

// Product management for approved sellers — same shape as AdminContentController,
// but every call is scoped to the caller's own Seller record.
@RestController
@RequestMapping("/api/user/seller/products")
public class SellerContentController {

    private final ContentService contentService;
    private final SellerRepository sellerRepository;

    public SellerContentController(ContentService contentService, SellerRepository sellerRepository) {
        this.contentService = contentService;
        this.sellerRepository = sellerRepository;
    }

    @GetMapping
    public ResponseEntity<List<ContentResponse>> listMine(Authentication authentication) {
        Seller seller = requireApprovedSeller(authentication);
        return ResponseEntity.ok(contentService.sellerListMine(seller.getId()));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> create(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam String contentType,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("file") MultipartFile file
    ) {
        Seller seller = requireApprovedSeller(authentication);
        ContentItem.ContentType type = parseType(contentType);
        return ResponseEntity.ok(contentService.sellerCreate(seller.getId(), title, description, price, type, thumbnail, file));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Seller seller = requireApprovedSeller(authentication);
        ContentItem.ContentType type = contentType != null ? parseType(contentType) : null;
        return ResponseEntity.ok(contentService.sellerUpdate(seller.getId(), id, title, description, price, type, active, thumbnail, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(Authentication authentication, @PathVariable Long id) {
        Seller seller = requireApprovedSeller(authentication);
        contentService.sellerDelete(seller.getId(), id);
        return ResponseEntity.ok(new MessageResponse("Product deleted."));
    }

    @PostMapping(value = "/{id}/files", consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> addExtraFile(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String fileType,
            @RequestParam(required = false) String label,
            @RequestParam("file") MultipartFile file
    ) {
        Seller seller = requireApprovedSeller(authentication);
        ContentItem.ContentType type = parseType(fileType);
        return ResponseEntity.ok(contentService.sellerAddExtraFile(seller.getId(), id, type, label, file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<ContentResponse> removeExtraFile(Authentication authentication, @PathVariable Long id, @PathVariable Long fileId) {
        Seller seller = requireApprovedSeller(authentication);
        return ResponseEntity.ok(contentService.sellerRemoveExtraFile(seller.getId(), id, fileId));
    }

    @Transactional(readOnly = true)
    private Seller requireApprovedSeller(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        Seller seller = sellerRepository.findByUser_Id(principal.getUserId())
                .orElseThrow(() -> new UnauthorizedException("You need a seller account for this — apply from the Sell page."));
        if (!"APPROVED".equals(seller.getStatus())) {
            throw new UnauthorizedException("Your seller application isn't approved yet.");
        }
        return seller;
    }

    private ContentItem.ContentType parseType(String value) {
        try {
            return ContentItem.ContentType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("contentType must be VIDEO, PDF or PHOTO.");
        }
    }
}
