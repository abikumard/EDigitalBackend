package com.contenthub.controller;

import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.dto.ContentDtos.CreateBookRequest;
import com.contenthub.entity.ContentItem;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.CloudinaryService;
import com.contenthub.service.SellerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller/books")
public class SellerContentController {

    private final SellerService sellerService;
    private final CloudinaryService cloudinaryService;

    public SellerContentController(SellerService sellerService, CloudinaryService cloudinaryService) {
        this.sellerService = sellerService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/bookshelf")
    public ResponseEntity<List<ContentResponse>> getBookshelf(Authentication authentication) {
        Long currentUserId = requireUserId(authentication);
        List<ContentItem> books = sellerService.getBookshelf(currentUserId);
        return ResponseEntity.ok(books.stream()
                .map(b -> ContentResponse.of(b, false))
                .collect(Collectors.toList()));
    }

    @PostMapping(value = "/publish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> publishBook(
            Authentication authentication,
            @RequestParam("title") String title,
            @RequestParam(value = "subtitle", required = false) String subtitle,
            @RequestParam(value = "authorName", required = false) String authorName,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "paperbackPrice", required = false) BigDecimal paperbackPrice,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "printLength", required = false) Integer printLength,
            @RequestParam(value = "sampleText", required = false) String sampleText,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestPart(value = "manuscript", required = false) MultipartFile manuscript
    ) {
        Long currentUserId = requireUserId(authentication);

        String coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80";
        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                coverUrl = cloudinaryService.uploadImage(coverImage, "kdp_covers");
            } catch (Exception e) {
                // fallback
            }
        }

        String manuscriptUrl = "https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/default_manuscript.pdf";
        if (manuscript != null && !manuscript.isEmpty()) {
            try {
                manuscriptUrl = cloudinaryService.uploadFile(manuscript, "kdp_manuscripts");
            } catch (Exception e) {
                // fallback
            }
        }

        CreateBookRequest req = new CreateBookRequest(
                title, subtitle, authorName, description, price, paperbackPrice,
                category, keywords, language, printLength, sampleText
        );

        ContentItem created = sellerService.publishBook(currentUserId, req, coverUrl, manuscriptUrl);
        return ResponseEntity.ok(ContentResponse.of(created, false));
    }

    private Long requireUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal.getUserId();
    }
}
