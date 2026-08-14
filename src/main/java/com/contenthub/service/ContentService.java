package com.contenthub.service;

import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Purchase;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final FileStorageService fileStorageService;

    public ContentService(ContentItemRepository contentItemRepository,
                           PurchaseRepository purchaseRepository,
                           FileStorageService fileStorageService) {
        this.contentItemRepository = contentItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<ContentResponse> listPublic(Long userIdOrNull) {
        return contentItemRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(c -> ContentResponse.of(c, isPurchased(userIdOrNull, c.getId())))
                .toList();
    }

    public ContentResponse getPublicById(Long id, Long userIdOrNull) {
        ContentItem item = getEntityOrThrow(id);
        return ContentResponse.of(item, isPurchased(userIdOrNull, item.getId()));
    }

    public List<ContentResponse> adminListAll() {
        return contentItemRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> ContentResponse.of(c, false))
                .toList();
    }

    public ContentItem getEntityOrThrow(Long id) {
        return contentItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));
    }

    private boolean isPurchased(Long userId, Long contentId) {
        if (userId == null) return false;
        return purchaseRepository.existsByUser_IdAndContent_IdAndStatus(userId, contentId, Purchase.Status.SUCCESS);
    }

    @Transactional
    public ContentResponse adminCreate(String title, String description, BigDecimal price,
                                        ContentItem.ContentType type,
                                        MultipartFile thumbnail, MultipartFile file) {
        validateBasics(title, price, type);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The content file (video/pdf/photo) is required.");
        }
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new BadRequestException("A thumbnail image is required.");
        }

        ContentItem item = new ContentItem();
        item.setTitle(title.trim());
        item.setDescription(description);
        item.setPrice(price);
        item.setContentType(type);
        item.setThumbnailPath(fileStorageService.store(thumbnail, "thumbnails"));
        item.setFilePath(fileStorageService.store(file, "files"));
        item.setOriginalFileName(file.getOriginalFilename());
        item.setActive(true);

        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    @Transactional
    public ContentResponse adminUpdate(Long id, String title, String description, BigDecimal price,
                                        ContentItem.ContentType type, Boolean active,
                                        MultipartFile thumbnail, MultipartFile file) {
        ContentItem item = getEntityOrThrow(id);

        if (title != null && !title.isBlank()) item.setTitle(title.trim());
        if (description != null) item.setDescription(description);
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Price must be greater than 0.");
            item.setPrice(price);
        }
        if (type != null) item.setContentType(type);
        if (active != null) item.setActive(active);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String old = item.getThumbnailPath();
            item.setThumbnailPath(fileStorageService.store(thumbnail, "thumbnails"));
            fileStorageService.delete(old);
        }
        if (file != null && !file.isEmpty()) {
            String old = item.getFilePath();
            item.setFilePath(fileStorageService.store(file, "files"));
            item.setOriginalFileName(file.getOriginalFilename());
            fileStorageService.delete(old);
        }

        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    @Transactional
    public void adminDelete(Long id) {
        ContentItem item = getEntityOrThrow(id);
        fileStorageService.delete(item.getThumbnailPath());
        fileStorageService.delete(item.getFilePath());
        contentItemRepository.delete(item);
    }

    private void validateBasics(String title, BigDecimal price, ContentItem.ContentType type) {
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required.");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Price must be greater than 0.");
        if (type == null) throw new BadRequestException("Content type is required.");
    }
}
