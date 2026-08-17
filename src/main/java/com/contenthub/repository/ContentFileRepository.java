package com.contenthub.repository;

import com.contenthub.entity.ContentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentFileRepository extends JpaRepository<ContentFile, Long> {
    List<ContentFile> findByContentItem_IdOrderByIdAsc(Long contentItemId);
    long countByContentItem_Id(Long contentItemId);
}
