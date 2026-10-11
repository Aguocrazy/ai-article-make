package com.aiarticle.service.image;

import com.aiarticle.util.CosFileClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * 按生图产物形态上传到 COS，得到文章可用的公开 URL。
 * <ul>
 *   <li>{@link ImagePayloadType#BASE64}：解码后上传</li>
 *   <li>{@link ImagePayloadType#URL}：下载远端文件再上传；失败则沿用原 URL</li>
 * </ul>
 */
@Slf4j
@Component
public class ImageCloudStore {

    private final CosFileClient cosFileClient;

    private final RestClient restClient;

    public ImageCloudStore(CosFileClient cosFileClient, RestClient.Builder restClientBuilder) {
        this.cosFileClient = cosFileClient;
        this.restClient = restClientBuilder.build();
    }

    /**
     * @return 云端 URL；无法处理时返回 {@code null}（URL 类型下载失败时回落原地址）
     */
    public String toPublicUrl(ImagePayload payload) {
        if (payload == null || !payload.hasContent()) {
            return null;
        }
        return switch (payload.getType()) {
            case URL -> uploadOrKeepUrl(payload);
            case BASE64 -> uploadBase64(payload);
        };
    }

    private String uploadOrKeepUrl(ImagePayload payload) {
        String sourceUrl = payload.getContent().trim();
        try {
            byte[] bytes = restClient.get().uri(sourceUrl).retrieve().body(byte[].class);
            String uploaded = uploadBytes(bytes, payload, guessExtension(payload, sourceUrl));
            return StringUtils.hasText(uploaded) ? uploaded : sourceUrl;
        } catch (RuntimeException e) {
            log.warn("按 URL 转存 COS 失败，沿用原地址, url={}, error={}", sourceUrl, e.getMessage());
            return sourceUrl;
        }
    }

    private String uploadBase64(ImagePayload payload) {
        try {
            byte[] bytes = decodeBase64(payload.getContent());
            return uploadBytes(bytes, payload, StringUtils.hasText(payload.getExtension()) ? payload.getExtension() : "bin");
        } catch (RuntimeException e) {
            log.warn("Base64 上传 COS 失败, error={}", e.getMessage());
            return null;
        }
    }

    private String uploadBytes(byte[] bytes, ImagePayload payload, String extension) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String prefix = StringUtils.hasText(payload.getKeyPrefix()) ? payload.getKeyPrefix().trim() : "images";
        String ext = StringUtils.hasText(extension) ? extension.trim().toLowerCase(Locale.ROOT) : "bin";
        String contentType = StringUtils.hasText(payload.getContentType())
                ? payload.getContentType()
                : contentTypeOf(ext);
        CosFileClient.CosUploadResult uploaded = cosFileClient.upload(
                bytes, prefix + "/" + UUID.randomUUID() + "." + ext, contentType);
        return uploaded == null ? null : uploaded.url();
    }

    static byte[] decodeBase64(String content) {
        String raw = content.trim();
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            raw = raw.substring(comma + 1);
        }
        return Base64.getDecoder().decode(raw.replaceAll("\\s+", ""));
    }

    private static String guessExtension(ImagePayload payload, String url) {
        if (StringUtils.hasText(payload.getExtension())) {
            return payload.getExtension();
        }
        String path = url.split("\\?", 2)[0].toLowerCase(Locale.ROOT);
        if (path.endsWith(".svg")) {
            return "svg";
        }
        if (path.endsWith(".png")) {
            return "png";
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "jpg";
        }
        if (path.endsWith(".webp")) {
            return "webp";
        }
        if (path.endsWith(".pdf")) {
            return "pdf";
        }
        return "bin";
    }

    private static String contentTypeOf(String extension) {
        return switch (extension) {
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
