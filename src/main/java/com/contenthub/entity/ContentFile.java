package com.contenthub.entity;

import jakarta.persistence.*;

// An EXTRA file bundled into a product (in addition to its main file).
// A product with 2-3 photos, or a couple of bonus PDFs, ends up as one
// ContentItem (title/price/access) plus several of these rows.
@Entity
@Table(name = "content_files")
public class ContentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_item_id", nullable = false)
    private ContentItem contentItem;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType; // VIDEO | PDF | PHOTO

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "label", length = 150)
    private String label;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ContentItem getContentItem() { return contentItem; }
    public void setContentItem(ContentItem contentItem) { this.contentItem = contentItem; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
