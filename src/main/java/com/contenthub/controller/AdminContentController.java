package com.contenthub.controller;

import com.contenthub.dto.CommonDtos.MessageResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentItem;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final ContentService contentService;

    public AdminContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ResponseEntity<List<ContentResponse>> listAll() {
        return ResponseEntity.ok(contentService.adminListAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.adminGetOne(id));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam String contentType,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("file") MultipartFile file
    ) {
        ContentItem.ContentType type = parseType(contentType);
        return ResponseEntity.ok(contentService.adminCreate(title, description, price, type, thumbnail, file));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        ContentItem.ContentType type = contentType != null ? parseType(contentType) : null;
        return ResponseEntity.ok(contentService.adminUpdate(id, title, description, price, type, active, thumbnail, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        contentService.adminDelete(id);
        return ResponseEntity.ok(new MessageResponse("Content deleted."));
    }

    @PostMapping(value = "/{id}/files", consumes = "multipart/form-data")
    public ResponseEntity<ContentResponse> addExtraFile(
            @PathVariable Long id,
            @RequestParam String fileType,
            @RequestParam(required = false) String label,
            @RequestParam("file") MultipartFile file
    ) {
        ContentItem.ContentType type = parseType(fileType);
        return ResponseEntity.ok(contentService.adminAddExtraFile(id, type, label, file));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ResponseEntity<ContentResponse> removeExtraFile(@PathVariable Long id, @PathVariable Long fileId) {
        return ResponseEntity.ok(contentService.adminRemoveExtraFile(id, fileId));
    }

    private ContentItem.ContentType parseType(String value) {
        try {
            return ContentItem.ContentType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("contentType must be VIDEO, PDF or PHOTO.");
        }
    }
}
