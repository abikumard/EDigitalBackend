package com.contenthub.service;

import com.contenthub.dto.ContentDtos.ContentFileResponse;
import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.dto.ContentDtos.SamplePreviewResponse;
import com.contenthub.entity.ContentFile;
import com.contenthub.entity.ContentItem;
import com.contenthub.entity.Purchase;
import com.contenthub.exception.AppExceptions.BadRequestException;
import com.contenthub.exception.AppExceptions.ResourceNotFoundException;
import com.contenthub.repository.ContentFileRepository;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PurchaseRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final ContentFileRepository contentFileRepository;
    private final PurchaseRepository purchaseRepository;
    private final CloudinaryService cloudinaryService;

    public ContentService(ContentItemRepository contentItemRepository,
                          ContentFileRepository contentFileRepository,
                          PurchaseRepository purchaseRepository,
                          CloudinaryService cloudinaryService) {
        this.contentItemRepository = contentItemRepository;
        this.contentFileRepository = contentFileRepository;
        this.purchaseRepository = purchaseRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @PostConstruct
    public void seedShowcaseBooks() {
        if (contentItemRepository.count() == 0) {
            ContentItem b1 = new ContentItem();
            b1.setTitle("The 2-Hour AI Side Hustle");
            b1.setSubtitle("A Step-by-Step Blueprint to Build 5 Automated Income Streams Using Modern AI Tools");
            b1.setAuthorName("Abikumar Dharmaraj");
            b1.setDescription("Master the 2-Hour AI Side Hustle system. Learn how to launch 5 profitable, automated micro-businesses in just 120 minutes a day using ChatGPT, Midjourney, Claude, and modern automation tools.");
            b1.setPrice(new BigDecimal("399.00"));
            b1.setPaperbackPrice(new BigDecimal("699.00"));
            b1.setCategory("AI & Technology");
            b1.setKeywords("AI, Side Hustle, Automation, ChatGPT, Passive Income, Business, Digital Products");
            b1.setIsbn("979-889-2026-01-1");
            b1.setLanguage("English");
            b1.setPrintLength(36);
            b1.setContentType(ContentItem.ContentType.EBOOK);
            b1.setThumbnailPath("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=800&q=80");
            b1.setFilePath("https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/the_2_hour_ai_side_hustle.pdf");
            b1.setStatus("LIVE");
            b1.setRoyaltyRate(new BigDecimal("0.97"));
            b1.setAverageRating(4.9);
            b1.setReviewCount(48);
            b1.setActive(true);
            b1.setSampleText("CHAPTER 1: THE ASYNCHRONOUS INCOME REVOLUTION\n\nThe traditional model of trading 40 hours a week for a fixed paycheck is fundamentally broken in the age of artificial intelligence. Today, a solo creator armed with modern generative AI models can produce what previously required a ten-person creative studio.\n\nIn this book, you will discover the exact 5 automated income streams designed to be operated in just 2 hours a day...");
            contentItemRepository.save(b1);

            ContentItem b2 = new ContentItem();
            b2.setTitle("The 7-Day Dopamine Reset");
            b2.setSubtitle("A Practical Protocol to Eliminate Brain Fog, Break Phone Addiction, and Reclaim Deep Focus");
            b2.setAuthorName("Abikumar Dharmaraj");
            b2.setDescription("A clinically backed neurobiology protocol designed to reset your dopamine baseline, crush screen addiction, and restore relentless daily focus and emotional clarity in just one week.");
            b2.setPrice(new BigDecimal("299.00"));
            b2.setPaperbackPrice(new BigDecimal("599.00"));
            b2.setCategory("Health & Mindset");
            b2.setKeywords("Dopamine, Neuroscience, Focus, Productivity, Mental Health, Habit Building");
            b2.setIsbn("979-889-2026-02-8");
            b2.setLanguage("English");
            b2.setPrintLength(28);
            b2.setContentType(ContentItem.ContentType.EBOOK);
            b2.setThumbnailPath("https://images.unsplash.com/photo-1506126613408-eca07ce68773?auto=format&fit=crop&w=800&q=80");
            b2.setFilePath("https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/the_7_day_dopamine_reset.pdf");
            b2.setStatus("LIVE");
            b2.setRoyaltyRate(new BigDecimal("0.97"));
            b2.setAverageRating(4.8);
            b2.setReviewCount(35);
            b2.setActive(true);
            b2.setSampleText("INTRODUCTION: THE HIJACKED BRAIN\n\nEvery notification chime, infinite scroll feed, and algorithmic hook is engineered to extract your most precious biological asset: your dopamine baseline. When your brain is constantly stimulated, baseline dopamine crashes, leaving you perpetually fatigued, unfocused, and anxious.\n\nThis 7-day protocol is your neurochemical reset button...");
            contentItemRepository.save(b2);

            ContentItem b3 = new ContentItem();
            b3.setTitle("The 10-Minute Nervous System Reset");
            b3.setSubtitle("Somatic Exercises and Vagus Nerve Protocols to Stop Overthinking, Release Chronic Stress, and Master Emotional Calm");
            b3.setAuthorName("Abikumar Dharmaraj");
            b3.setDescription("Transform your physical and emotional state in 10 minutes. Evidence-based somatic exercises and vagus nerve stimulation techniques to dissolve acute anxiety and restore nervous system balance.");
            b3.setPrice(new BigDecimal("349.00"));
            b3.setPaperbackPrice(new BigDecimal("649.00"));
            b3.setCategory("Self-Help & Wellness");
            b3.setKeywords("Somatic, Nervous System, Vagus Nerve, Stress Relief, Anxiety, Meditation");
            b3.setIsbn("979-889-2026-03-5");
            b3.setLanguage("English");
            b3.setPrintLength(32);
            b3.setContentType(ContentItem.ContentType.EBOOK);
            b3.setThumbnailPath("https://images.unsplash.com/photo-1518241353330-0f7941c2d9b5?auto=format&fit=crop&w=800&q=80");
            b3.setFilePath("https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/the_10_minute_nervous_system_reset.pdf");
            b3.setStatus("LIVE");
            b3.setRoyaltyRate(new BigDecimal("0.97"));
            b3.setAverageRating(5.0);
            b3.setReviewCount(52);
            b3.setActive(true);
            b3.setSampleText("SECTION 1: THE SOMATIC CODE\n\nStress does not originate solely in your thoughts; it is stored physically in your fascia, diaphragm, and autonomic nervous system. By activating the ventral vagal complex through rapid somatic micro-movements, you can down-regulate cortisol in under 120 seconds...");
            contentItemRepository.save(b3);

            ContentItem b4 = new ContentItem();
            b4.setTitle("Shadow Work Journal & Workbook");
            b4.setSubtitle("Transformative Prompts, Exercises, and Guided Inquiries to Heal the Past and Integrate Your Hidden Self");
            b4.setAuthorName("Abikumar Dharmaraj");
            b4.setDescription("A deep, transformative psychological journey into Jungian shadow work. 90 guided journal prompts, inner-child integration exercises, and emotional release frameworks.");
            b4.setPrice(new BigDecimal("449.00"));
            b4.setPaperbackPrice(new BigDecimal("799.00"));
            b4.setCategory("Psychology & Healing");
            b4.setKeywords("Shadow Work, Carl Jung, Healing, Personal Growth");
            b4.setIsbn("979-889-2026-04-2");
            b4.setLanguage("English");
            b4.setPrintLength(42);
            b4.setContentType(ContentItem.ContentType.EBOOK);
            b4.setThumbnailPath("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80");
            b4.setFilePath("https://res.cloudinary.com/demo/image/upload/v1/samples/kdp/shadow_work_journal.pdf");
            b4.setStatus("LIVE");
            b4.setRoyaltyRate(new BigDecimal("0.97"));
            b4.setAverageRating(4.9);
            b4.setReviewCount(41);
            b4.setActive(true);
            b4.setSampleText("PROMPT DAY 1: MEETING YOUR TRIGGER\n\nWhat is the single quality in other people that frustrates you the most? Often, our strongest emotional reactions to others mirror the parts of ourselves we have disowned or repressed. Write without judgment...");
            contentItemRepository.save(b4);
        }
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> listActive(Long currentUserId, String category, String search) {
        List<ContentItem> items;
        if (search != null && !search.isBlank()) {
            items = contentItemRepository.searchBooks(search.trim());
        } else if (category != null && !category.isBlank()) {
            items = contentItemRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByCreatedAtDesc(category.trim());
        } else {
            items = contentItemRepository.findByActiveTrueOrderByCreatedAtDesc();
        }

        Set<Long> purchasedIds = Collections.emptySet();
        if (currentUserId != null) {
            purchasedIds = purchaseRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(currentUserId, Purchase.Status.SUCCESS)
                    .stream()
                    .map(p -> p.getContent().getId())
                    .collect(Collectors.toSet());
        }

        final Set<Long> finalPurchased = purchasedIds;
        return items.stream()
                .map(item -> ContentResponse.of(item, finalPurchased.contains(item.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContentResponse getDetail(Long id, Long currentUserId) {
        ContentItem item = getEntityOrThrow(id);
        boolean purchased = false;
        if (currentUserId != null) {
            purchased = purchaseRepository.existsByUser_IdAndContent_IdAndStatus(
                    currentUserId, id, Purchase.Status.SUCCESS);
        }

        List<ContentFileResponse> extraFiles = contentFileRepository
                .findByContentItem_IdOrderByIdAsc(id)
                .stream()
                .map(f -> new ContentFileResponse(f.getId(), f.getFileType(), f.getLabel(), f.getFilePath()))
                .toList();

        return ContentResponse.of(item, purchased, null, extraFiles);
    }

    @Transactional(readOnly = true)
    public SamplePreviewResponse getSamplePreview(Long id) {
        ContentItem item = getEntityOrThrow(id);
        return new SamplePreviewResponse(
                item.getId(),
                item.getTitle(),
                item.getSubtitle(),
                item.getAuthorName(),
                item.getThumbnailPath(),
                item.getCategory(),
                item.getPrintLength(),
                item.getSampleText() != null ? item.getSampleText() : item.getDescription()
        );
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> listPurchased(Long userId) {
        List<Purchase> purchases = purchaseRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(
                userId, Purchase.Status.SUCCESS);

        List<ContentResponse> out = new ArrayList<>();
        for (Purchase p : purchases) {
            ContentItem c = p.getContent();
            List<ContentFileResponse> extraFiles = contentFileRepository
                    .findByContentItem_IdOrderByIdAsc(c.getId())
                    .stream()
                    .map(f -> new ContentFileResponse(f.getId(), f.getFileType(), f.getLabel(), f.getFilePath()))
                    .toList();
            out.add(ContentResponse.of(c, true, p.getCreatedAt(), extraFiles));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> adminListAll() {
        return contentItemRepository.findAll().stream()
                .map(item -> ContentResponse.of(item, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContentResponse adminGetOne(Long id) {
        return ContentResponse.of(getEntityOrThrow(id), false);
    }

    @Transactional
    public ContentResponse adminCreate(String title, String description, BigDecimal price,
                                       ContentItem.ContentType type, MultipartFile thumbnail, MultipartFile file) {
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required.");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) throw new BadRequestException("Price must be >= 0.");
        if (thumbnail == null || thumbnail.isEmpty()) throw new BadRequestException("Thumbnail file is required.");
        if (file == null || file.isEmpty()) throw new BadRequestException("Main content file is required.");

        String thumbUrl = cloudinaryService.upload(thumbnail, "kdp_covers");
        String fileUrl = cloudinaryService.upload(file, "kdp_manuscripts");

        ContentItem item = new ContentItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(price);
        item.setContentType(type != null ? type : ContentItem.ContentType.EBOOK);
        item.setThumbnailPath(thumbUrl);
        item.setFilePath(fileUrl);
        item.setStatus("LIVE");
        item.setActive(true);

        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    @Transactional
    public ContentResponse adminUpdate(Long id, String title, String description, BigDecimal price,
                                       ContentItem.ContentType type, Boolean active,
                                       MultipartFile thumbnail, MultipartFile file) {
        ContentItem item = getEntityOrThrow(id);

        if (title != null) item.setTitle(title);
        if (description != null) item.setDescription(description);
        if (price != null) item.setPrice(price);
        if (type != null) item.setContentType(type);
        if (active != null) item.setActive(active);
        if (thumbnail != null && !thumbnail.isEmpty()) item.setThumbnailPath(cloudinaryService.upload(thumbnail, "kdp_covers"));
        if (file != null && !file.isEmpty()) item.setFilePath(cloudinaryService.upload(file, "kdp_manuscripts"));

        return ContentResponse.of(contentItemRepository.save(item), false);
    }

    @Transactional
    public void adminDelete(Long id) {
        contentFileRepository.deleteByContentItem_Id(id);
        contentItemRepository.deleteById(id);
    }

    @Transactional
    public ContentFileResponse adminAddExtraFile(Long contentId, ContentItem.ContentType type, String label, MultipartFile file) {
        ContentItem item = getEntityOrThrow(contentId);
        String url = cloudinaryService.upload(file, "kdp_extras");
        ContentFile cf = new ContentFile();
        cf.setContentItem(item);
        cf.setFileType(type != null ? type.name() : "PDF");
        cf.setLabel(label != null ? label : "Bonus File");
        cf.setFilePath(url);
        cf = contentFileRepository.save(cf);
        return new ContentFileResponse(cf.getId(), cf.getFileType(), cf.getLabel(), cf.getFilePath());
    }

    @Transactional
    public void adminRemoveExtraFile(Long contentId, Long fileId) {
        contentFileRepository.deleteById(fileId);
    }

    public ContentItem getEntityOrThrow(Long id) {
        return contentItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
    }
}
