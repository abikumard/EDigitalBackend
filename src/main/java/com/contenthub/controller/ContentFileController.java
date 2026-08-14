package com.contenthub.controller;

import com.contenthub.entity.ContentItem;
import com.contenthub.exception.AppExceptions.PaymentException;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.ContentService;
import com.contenthub.service.FileStorageService;
import com.contenthub.service.PaymentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentFileController {

    private final ContentService contentService;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;

    public ContentFileController(ContentService contentService, FileStorageService fileStorageService, PaymentService paymentService) {
        this.contentService = contentService;
        this.fileStorageService = fileStorageService;
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<org.springframework.core.io.Resource> thumbnail(@PathVariable Long id) throws IOException {
        ContentItem item = contentService.getEntityOrThrow(id);
        Path path = fileStorageService.resolve(item.getThumbnailPath());
        MediaType mediaType = detectMediaType(path);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<ResourceRegion> streamFile(@PathVariable Long id,
                                                       @RequestHeader HttpHeaders headers,
                                                       Authentication authentication) throws IOException {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }

        ContentItem item = contentService.getEntityOrThrow(id);

        boolean allowed = principal.isAdmin() || paymentService.hasAccess(principal.getUserId(), id);
        if (!allowed) {
            throw new PaymentException("Please purchase this content to access it.");
        }

        Path path = fileStorageService.resolve(item.getFilePath());
        FileSystemResource resource = new FileSystemResource(path);
        long contentLength = resource.contentLength();
        MediaType mediaType = detectMediaType(path);

        List<HttpRange> ranges = headers.getRange();
        ResourceRegion region;
        HttpStatus status;

        if (ranges.isEmpty()) {
            region = new ResourceRegion(resource, 0, contentLength);
            status = HttpStatus.OK;
        } else {
            HttpRange range = ranges.get(0);
            region = range.toResourceRegion(resource);
            status = HttpStatus.PARTIAL_CONTENT;
        }

        String downloadName = item.getOriginalFileName() != null ? item.getOriginalFileName() : (item.getTitle());

        return ResponseEntity.status(status)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitize(downloadName) + "\"")
                .body(region);
    }

    private String sanitize(String name) {
        return name == null ? "file" : name.replaceAll("[\"\\r\\n]", "_");
    }

    private MediaType detectMediaType(Path path) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) return MediaType.parseMediaType(probed);
        } catch (Exception ignored) { }

        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        if (name.endsWith(".webm")) return MediaType.valueOf("video/webm");
        if (name.endsWith(".mov")) return MediaType.valueOf("video/quicktime");
        if (name.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".webp")) return MediaType.valueOf("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
