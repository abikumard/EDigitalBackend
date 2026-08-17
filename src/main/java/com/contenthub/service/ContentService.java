package com.contenthub.service;

import com.contenthub.dto.ContentDtos.ContentFileResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentFile;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Purchase;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentFileRepository;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContentService {

    private static final int MAX_EXTRA_FILES = 4;

    private final ContentItemRepository contentItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final ContentFileRepository contentFileRepository;
    private final FileStorageService fileStorageService;

    public ContentService(ContentItemRepository contentItemRepository,
                           PurchaseRepository purchaseRepository,
                           ContentFileRepository contentFileRepository,
                           FileStorageService fileStorageService) {
        this.contentItemRepository = contentItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.contentFileRepository = contentFileRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<ContentResponse> listPublic(Long userIdOrNull) {
        return contentItemRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(c -> ContentResponse.of(c, isPurchased(userIdOrNull, c.getId())))
                .toList();
    }

    public ContentResponse getPublicById(Long id, Long userIdOrNull) {
        ContentItem item = getEntityOrThrow(id);
        return ContentResponse.of(item, isPurchased(userIdOrNull, item.getId()), null, buildExtraFiles(id));
    }

    public List<ContentResponse> adminListAll() {
        return contentItemRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> ContentResponse.of(c, false))
                .toList();
    }

    public ContentResponse adminGetOne(Long id) {
        ContentItem item = getEntityOrThrow(id);
        return ContentResponse.of(item, false, null, buildExtraFiles(id));
    }

    public ContentItem getEntityOrThrow(Long id) {
        return contentItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));
    }

    private boolean isPurchased(Long userId, Long contentId) {
        if (userId == null) return false;
        return purchaseRepository.existsByUser_IdAndContent_IdAndStatus(userId, contentId, Purchase.Status.SUCCESS);
    }

    private List<ContentFileResponse> buildExtraFiles(Long contentItemId) {
        List<ContentFileResponse> extra = new ArrayList<>();
        for (ContentFile f : contentFileRepository.findByContentItem_IdOrderByIdAsc(contentItemId)) {
            extra.add(new ContentFileResponse(
                    f.getId(),
                    f.getFileType(),
                    f.getLabel(),
                    "/api/content/" + contentItemId + "/files/" + f.getId()
            ));
        }
        return extra;
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

        return ContentResponse.of(contentItemRepository.save(item), false, null, buildExtraFiles(id));
    }

    // Attaches one MORE file (photo/pdf/video) to an existing product — e.g.
    // a "3 photos" pack, or a couple of bonus PDFs alongside the main file.
    @Transactional
    public ContentResponse adminAddExtraFile(Long contentItemId, ContentItem.ContentType type,
                                              String label, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to add.");
        }
        ContentItem item = getEntityOrThrow(contentItemId);

        long existingCount = contentFileRepository.countByContentItem_Id(contentItemId);
        if (existingCount >= MAX_EXTRA_FILES) {
            throw new BadRequestException("You can attach up to " + MAX_EXTRA_FILES + " extra files per product.");
        }

        ContentFile cf = new ContentFile();
        cf.setContentItem(item);
        cf.setFileType(type.name());
        cf.setFilePath(fileStorageService.store(file, "files"));
        cf.setOriginalFileName(file.getOriginalFilename());
        cf.setLabel(label != null && !label.isBlank() ? label.trim() : null);
        contentFileRepository.save(cf);

        return adminGetOne(contentItemId);
    }

    @Transactional
    public ContentResponse adminRemoveExtraFile(Long contentItemId, Long fileId) {
        ContentFile cf = contentFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!cf.getContentItem().getId().equals(contentItemId)) {
            throw new BadRequestException("This file does not belong to that product.");
        }
        fileStorageService.delete(cf.getFilePath());
        contentFileRepository.delete(cf);
        return adminGetOne(contentItemId);
    }

    @Transactional
    public void adminDelete(Long id) {
        ContentItem item = getEntityOrThrow(id);
        List<ContentFile> extras = contentFileRepository.findByContentItem_IdOrderByIdAsc(id);
        for (ContentFile f : extras) {
            fileStorageService.delete(f.getFilePath());
        }
        contentFileRepository.deleteAll(extras);
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
