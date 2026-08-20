package com.contenthub.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Thin wrapper so the rest of the app (ContentService etc.) doesn't need to
// know or care that files live on Cloudinary now instead of local disk —
// store() still just takes a file and returns a String to persist on the
// entity, same as before.
@Service
public class FileStorageService {

    private final CloudinaryService cloudinaryService;

    public FileStorageService(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    // Returns a permanent Cloudinary URL (https://...) — store this directly
    // on ContentItem.thumbnailPath / filePath or ContentFile.filePath.
    public String store(MultipartFile file, String folder) {
        return cloudinaryService.upload(file, folder);
    }

    // Best-effort only: without also storing Cloudinary's public_id separately,
    // we can't reliably issue a destroy call here, so old assets are simply
    // left in Cloudinary storage rather than risk breaking update/delete flows
    // with fragile URL-parsing. Free-tier storage (25GB) comfortably absorbs
    // this for a small catalog — revisit if that ever becomes a real cost.
    public void delete(String urlOrPath) {
        // intentionally a no-op — see note above
    }
}
