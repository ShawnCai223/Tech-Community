package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.service.ObjectStorageService;
import com.shawnidea.community.util.AppUtil;
import com.shawnidea.community.util.HostHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadApiController {
    private static final Logger logger = LoggerFactory.getLogger(UploadApiController.class);

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private HostHolder hostHolder;

    @PostMapping
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (hostHolder.getUser() == null) {
            return ApiResponse.error(401, "You are not signed in.");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "No file selected.");
        }

        String suffix = resolveSuffix(file);
        boolean isImage = ALLOWED_IMAGE_EXTENSIONS.contains(suffix);
        boolean isVideo = ALLOWED_VIDEO_EXTENSIONS.contains(suffix);

        if (!isImage && !isVideo) {
            return ApiResponse.error(400, "Unsupported file type. Allowed: images (png, jpg, jpeg, gif, webp) and videos (mp4, webm, mov).");
        }

        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            return ApiResponse.error(400, "Image file too large. Maximum size is 10MB.");
        }
        if (isVideo && file.getSize() > MAX_VIDEO_SIZE) {
            return ApiResponse.error(400, "Video file too large. Maximum size is 100MB.");
        }

        try {
            String folder = isImage ? "images" : "videos";
            String key = "uploads/" + folder + "/" + AppUtil.generateUUID() + suffix;
            String url = objectStorageService.uploadShareStream(
                    key,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            String type = isImage ? "image" : "video";
            return ApiResponse.ok(Map.of("url", url, "type", type));
        } catch (IOException e) {
            logger.warn("Upload failed due to IO error.", e);
            return ApiResponse.error(500, "Upload failed.");
        } catch (Exception e) {
            logger.error("Upload failed unexpectedly.", e);
            return ApiResponse.error(500, "Upload failed.");
        }
    }

    private String resolveSuffix(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            int index = filename.lastIndexOf(".");
            if (index >= 0) {
                return filename.substring(index).toLowerCase();
            }
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return "";
        }

        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> "";
        };
    }
}
