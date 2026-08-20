package com.contenthub.controller;

import com.contenthub.entity.ContentFile;
import com.contenthub.entity.ContentItem;
import com.contenthub.exception.AppExceptions.PaymentException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.ContentFileRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import com.contenthub.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// Files now live on Cloudinary (see FileStorageService/CloudinaryService) —
// this controller's job is purely access control: check the request is
// allowed, then redirect the browser straight to the Cloudinary URL. Range
// requests for video seeking are handled by Cloudinary's own CDN once
// redirected there, so there's no need to reimplement that here.
@RestController
@RequestMapping("/api/content")
public class ContentFileController {

    private final ContentService contentService;
    private final PaymentService paymentService;
    private final ContentFileRepository contentFileRepository;

    public ContentFileController(ContentService contentService, PaymentService paymentService,
                                  ContentFileRepository contentFileRepository) {
        this.contentService = contentService;
        this.paymentService = paymentService;
        this.contentFileRepository = contentFileRepository;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Void> streamFile(@PathVariable Long id, Authentication authentication) {
        ContentItem item = contentService.getEntityOrThrow(id);
        requireAccess(id, authentication);
        return redirectTo(item.getFilePath());
    }

    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<Void> streamExtraFile(@PathVariable Long id, @PathVariable Long fileId,
                                                 Authentication authentication) {
        requireAccess(id, authentication);
        ContentFile extra = contentFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!extra.getContentItem().getId().equals(id)) {
            throw new ResourceNotFoundException("File not found");
        }
        return redirectTo(extra.getFilePath());
    }

    private ResponseEntity<Void> redirectTo(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private void requireAccess(Long contentId, Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        boolean allowed = principal.isAdmin() || paymentService.hasAccess(principal.getUserId(), contentId);
        if (!allowed) {
            throw new PaymentException("Please purchase this content to access it.");
        }
    }
}
