package com.contenthub.repository;

import com.contenthub.entity.ContentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentFileRepository extends JpaRepository<ContentFile, Long> {
    List<ContentFile> findByContentItem_IdOrderByIdAsc(Long contentItemId);
    List<ContentFile> findByContentItem_IdOrderByDisplayOrderAsc(Long contentItemId);
    long countByContentItem_Id(Long contentItemId);
    void deleteByContentItem_Id(Long contentItemId);
}
