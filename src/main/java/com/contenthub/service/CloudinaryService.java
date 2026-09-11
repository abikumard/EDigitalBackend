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
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

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

    public String uploadImage(MultipartFile file, String folder) {
        return upload(file, folder);
    }

    public String uploadFile(MultipartFile file, String folder) {
        return upload(file, folder);
    }

    public String upload(MultipartFile file, String folder) {
        if (isPlaceholder(cloudName) || isPlaceholder(apiKey) || isPlaceholder(apiSecret)) {
            // In local/test mode without credentials, return placeholder URL
            return "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80";
        }
        try {
            String publicId = folder + "/" + UUID.randomUUID();
            long timestamp = System.currentTimeMillis() / 1000;

            String paramsToSign = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp;
            String signature = sha1Hex(paramsToSign + apiSecret);

            String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("api_key", apiKey);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("folder", folder);
            body.add("public_id", publicId);
            body.add("signature", signature);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForEntity(url, requestEntity, Map.class).getBody();

            if (response == null || response.get("secure_url") == null) {
                throw new BadRequestException("Cloudinary did not return a URL.");
            }
            return response.get("secure_url").toString();
        } catch (Exception e) {
            return "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80";
        }
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPlaceholder(String val) {
        return val == null || val.isBlank() || val.contains("YOUR_") || val.contains("placeholder");
    }
}
