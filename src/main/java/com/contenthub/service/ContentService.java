package com.contenthub.service;

import com.contenthub.dto.ContentDtos.ContentFileResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.ContentFile;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Purchase;
import com.contenthub.entity.Seller;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentFileRepository;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// c.getSeller() (lazy) is read inside ContentResponse.of() now, so every
// read path here needs an active transaction — hence class-level
// @Transactional(readOnly = true), with individual write methods overriding
// it with their own @Transactional.
@Service
@Transactional(readOnly = true)
public class ContentService {

    private static final int MAX_EXTRA_FILES = 4;

    private final ContentItemRepository contentItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final ContentFileRepository contentFileRepository;
    private final SellerRepository sellerRepository;
    private final FileStorageService fileStorageService;

    public ContentService(ContentItemRepository contentItemRepository,
                           PurchaseRepository purchaseRepository,
                           ContentFileRepository contentFileRepository,
                           SellerRepository sellerRepository,
                           FileStorageService fileStorageService) {
        this.contentItemRepository = contentItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.contentFileRepository = contentFileRepository;
        this.sellerRepository = sellerRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<ContentResponse> listPublic(Long userIdOrNull) {
        List<ContentResponse> list = new ArrayList<>();
        for (ContentItem c : contentItemRepository.findByActiveTrueOrderByCreatedAtDesc()) {
            list.add(ContentResponse.of(c, isPurchased(userIdOrNull, c.getId())));
        }
        return list;
    }

    public ContentResponse getPublicById(Long id, Long userIdOrNull) {
        ContentItem item = getEntityOrThrow(id);
        return ContentResponse.of(item, isPurchased(userIdOrNull, item.getId()), null, buildExtraFiles(id));
    }

    public List<ContentResponse> adminListAll() {
        List<ContentResponse> list = new ArrayList<>();
        for (ContentItem c : contentItemRepository.findAllByOrderByCreatedAtDesc()) {
            list.add(ContentResponse.of(c, false));
        }
        return list;
    }

    public ContentResponse adminGetOne(Long id) {
        ContentItem item = getEntityOrThrow(id);
        return ContentResponse.of(item, false, null, buildExtraFiles(id));
    }

    public List<ContentResponse> sellerListMine(Long sellerId) {
        List<ContentResponse> list = new ArrayList<>();
        for (ContentItem c : contentItemRepository.findBySeller_IdOrderByCreatedAtDesc(sellerId)) {
            list.add(ContentResponse.of(c, false, null, buildExtraFiles(c.getId())));
        }
        return list;
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
        ContentItem item = buildNewItem(title, description, price, type, thumbnail, file);
        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    @Transactional
    public ContentResponse sellerCreate(Long sellerId, String title, String description, BigDecimal price,
                                         ContentItem.ContentType type,
                                         MultipartFile thumbnail, MultipartFile file) {
        ContentItem item = buildNewItem(title, description, price, type, thumbnail, file);
        item.setSeller(sellerRepository.getReferenceById(sellerId));
        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    private ContentItem buildNewItem(String title, String description, BigDecimal price,
                                      ContentItem.ContentType type, MultipartFile thumbnail, MultipartFile file) {
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
        return item;
    }

    @Transactional
    public ContentResponse adminUpdate(Long id, String title, String description, BigDecimal price,
                                        ContentItem.ContentType type, Boolean active,
                                        MultipartFile thumbnail, MultipartFile file) {
        ContentItem item = getEntityOrThrow(id);
        applyUpdate(item, title, description, price, type, active, thumbnail, file);
        return ContentResponse.of(contentItemRepository.save(item), false, null, buildExtraFiles(id));
    }

    @Transactional
    public ContentResponse sellerUpdate(Long sellerId, Long id, String title, String description, BigDecimal price,
                                         ContentItem.ContentType type, Boolean active,
                                         MultipartFile thumbnail, MultipartFile file) {
        ContentItem item = getEntityOrThrow(id);
        requireOwnership(item, sellerId);
        applyUpdate(item, title, description, price, type, active, thumbnail, file);
        return ContentResponse.of(contentItemRepository.save(item), false, null, buildExtraFiles(id));
    }

    private void applyUpdate(ContentItem item, String title, String description, BigDecimal price,
                              ContentItem.ContentType type, Boolean active,
                              MultipartFile thumbnail, MultipartFile file) {
        if (title != null && !title.isBlank()) item.setTitle(title.trim());
        if (description != null) item.setDescription(description);
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Price must be greater than 0.");
            item.setPrice(price);
        }
        if (type != null) item.setContentType(type);
        if (active != null) item.setActive(active);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            item.setThumbnailPath(fileStorageService.store(thumbnail, "thumbnails"));
        }
        if (file != null && !file.isEmpty()) {
            item.setFilePath(fileStorageService.store(file, "files"));
            item.setOriginalFileName(file.getOriginalFilename());
        }
    }

    // Attaches one MORE file (photo/pdf/video) to an existing product — e.g.
    // a "3 photos" pack, or a couple of bonus PDFs alongside the main file.
    @Transactional
    public ContentResponse adminAddExtraFile(Long contentItemId, ContentItem.ContentType type,
                                              String label, MultipartFile file) {
        ContentItem item = getEntityOrThrow(contentItemId);
        addExtraFile(item, type, label, file);
        return adminGetOne(contentItemId);
    }

    @Transactional
    public ContentResponse sellerAddExtraFile(Long sellerId, Long contentItemId, ContentItem.ContentType type,
                                               String label, MultipartFile file) {
        ContentItem item = getEntityOrThrow(contentItemId);
        requireOwnership(item, sellerId);
        addExtraFile(item, type, label, file);
        return ContentResponse.of(item, false, null, buildExtraFiles(contentItemId));
    }

    private void addExtraFile(ContentItem item, ContentItem.ContentType type, String label, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to add.");
        }
        long existingCount = contentFileRepository.countByContentItem_Id(item.getId());
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
    }

    @Transactional
    public ContentResponse adminRemoveExtraFile(Long contentItemId, Long fileId) {
        removeExtraFile(contentItemId, fileId);
        return adminGetOne(contentItemId);
    }

    @Transactional
    public ContentResponse sellerRemoveExtraFile(Long sellerId, Long contentItemId, Long fileId) {
        ContentItem item = getEntityOrThrow(contentItemId);
        requireOwnership(item, sellerId);
        removeExtraFile(contentItemId, fileId);
        return ContentResponse.of(item, false, null, buildExtraFiles(contentItemId));
    }

    private void removeExtraFile(Long contentItemId, Long fileId) {
        ContentFile cf = contentFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        if (!cf.getContentItem().getId().equals(contentItemId)) {
            throw new BadRequestException("This file does not belong to that product.");
        }
        contentFileRepository.delete(cf);
    }

    @Transactional
    public void adminDelete(Long id) {
        ContentItem item = getEntityOrThrow(id);
        contentFileRepository.deleteAll(contentFileRepository.findByContentItem_IdOrderByIdAsc(id));
        contentItemRepository.delete(item);
    }

    @Transactional
    public void sellerDelete(Long sellerId, Long id) {
        ContentItem item = getEntityOrThrow(id);
        requireOwnership(item, sellerId);
        contentFileRepository.deleteAll(contentFileRepository.findByContentItem_IdOrderByIdAsc(id));
        contentItemRepository.delete(item);
    }

    private void requireOwnership(ContentItem item, Long sellerId) {
        if (item.getSeller() == null || !item.getSeller().getId().equals(sellerId)) {
            throw new BadRequestException("You don't have permission to modify this product.");
        }
    }

    private void validateBasics(String title, BigDecimal price, ContentItem.ContentType type) {
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required.");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Price must be greater than 0.");
        if (type == null) throw new BadRequestException("Content type is required.");
    }
}
