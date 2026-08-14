package com.contenthub.controller;

import com.contenthub.dto.ContentDtos.ContentResponse;
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
    public ResponseEntity<List<ContentResponse>> list(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        Long userId = principal != null && !principal.isAdmin() ? principal.getUserId() : null;
        return ResponseEntity.ok(contentService.listPublic(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> detail(@PathVariable Long id, Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        Long userId = principal != null && !principal.isAdmin() ? principal.getUserId() : null;
        return ResponseEntity.ok(contentService.getPublicById(id, userId));
    }
}
