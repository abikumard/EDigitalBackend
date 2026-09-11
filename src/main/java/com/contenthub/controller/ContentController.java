package com.contenthub.controller;

import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.dto.ContentDtos.SamplePreviewResponse;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ResponseEntity<List<ContentResponse>> listActive(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        Long currentUserId = getUserId(authentication);
        return ResponseEntity.ok(contentService.listActive(currentUserId, category, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> getDetail(
            @PathVariable Long id,
            Authentication authentication) {
        Long currentUserId = getUserId(authentication);
        return ResponseEntity.ok(contentService.getDetail(id, currentUserId));
    }

    @GetMapping("/{id}/sample")
    public ResponseEntity<SamplePreviewResponse> getSamplePreview(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getSamplePreview(id));
    }

    @GetMapping("/my-library")
    public ResponseEntity<List<ContentResponse>> listPurchased(Authentication authentication) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(contentService.listPurchased(currentUserId));
    }

    private Long getUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        return principal != null ? principal.getUserId() : null;
    }

    private Long requireUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to view library.");
        }
        return principal.getUserId();
    }
}
