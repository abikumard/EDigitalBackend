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
    private final ContentFileRepository contentFileRepository;

    public ContentFileController(ContentService contentService, FileStorageService fileStorageService,
                                  PaymentService paymentService, ContentFileRepository contentFileRepository) {
        this.contentService = contentService;
        this.fileStorageService = fileStorageService;
        this.paymentService = paymentService;
        this.contentFileRepository = contentFileRepository;
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
        ContentItem item = contentService.getEntityOrThrow(id);
        requireAccess(id, authentication);
        String downloadName = item.getOriginalFileName() != null ? item.getOriginalFileName() : item.getTitle();
        return streamPath(fileStorageService.resolve(item.getFilePath()), downloadName, headers);
    }

    // Extra files bundled onto a product (e.g. photo 2, photo 3, a bonus PDF)
    // — same purchase-gated access as the main file, just a different row.
    @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<ResourceRegion> streamExtraFile(@PathVariable Long id,
                                                            @PathVariable Long fileId,
                                                            @RequestHeader HttpHeaders headers,
                                                            Authentication authentication) throws IOException {
        requireAccess(id, authentication);
        ContentFile extra = contentFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!extra.getContentItem().getId().equals(id)) {
            throw new ResourceNotFoundException("File not found");
        }
        String downloadName = extra.getOriginalFileName() != null ? extra.getOriginalFileName() : "file";
        return streamPath(fileStorageService.resolve(extra.getFilePath()), downloadName, headers);
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

    private ResponseEntity<ResourceRegion> streamPath(Path path, String downloadName, HttpHeaders headers) throws IOException {
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
