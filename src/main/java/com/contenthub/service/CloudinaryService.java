package com.contenthub.service;

import com.contenthub.exception.AppExceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

// Uploads files to Cloudinary instead of local disk. Local disk on Render's
// free tier is wiped on every restart/redeploy/spin-down — Cloudinary is
// permanent, free-tier-friendly storage that survives all of that.
//
// Files get an unguessable UUID-based public_id and are uploaded as public
// resources; access control is still enforced by our own backend (see
// ContentFileController) before ever handing out the URL — this is simpler
// and more reliable than replicating Cloudinary's private-asset signing
// scheme, at the cost of a knowing-the-exact-link being enough if someone
// deliberately extracts and shares it (it won't expire on its own).
@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    public CloudinaryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String upload(MultipartFile file, String folder) {
        if (isPlaceholder(cloudName) || isPlaceholder(apiKey) || isPlaceholder(apiSecret)) {
            throw new BadRequestException("File storage is not configured yet (missing Cloudinary credentials).");
        }
        try {
            String publicId = folder + "/" + UUID.randomUUID();
            long timestamp = System.currentTimeMillis() / 1000;

            // Cloudinary upload signature: sha1("folder=..&public_id=..&timestamp=.." + api_secret), hex-encoded.
            String paramsToSign = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp;
            String signature = sha1Hex(paramsToSign + apiSecret);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(file.getBytes(), file.getOriginalFilename()));
            body.add("public_id", publicId);
            body.add("folder", folder);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("api_key", apiKey);
            body.add("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            String resourceType = detectResourceType(file.getContentType());
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload";

            Map<?, ?> response = restTemplate.postForObject(uploadUrl, entity, Map.class);
            if (response == null || response.get("secure_url") == null) {
                throw new BadRequestException("File upload failed. Please try again.");
            }
            return response.get("secure_url").toString();
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file.");
        } catch (Exception e) {
            throw new BadRequestException("File upload failed: " + e.getMessage());
        }
    }

    private String detectResourceType(String contentType) {
        if (contentType == null) return "raw";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        return "raw"; // pdf and everything else
    }

    private boolean isPlaceholder(String v) {
        return v == null || v.isBlank() || v.startsWith("YOUR_");
    }

    private String sha1Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename != null ? filename : "file";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
