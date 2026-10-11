package com.aiarticle.service.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Base64;

/**
 * 生图策略的原始产物。策略本身不上传，由 {@link ImageCloudStore} 转成可访问 URL。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePayload {

    private ImagePayloadType type;

    /**
     * URL 或 Base64 文本。
     */
    private String content;

    private String contentType;

    /**
     * 对象扩展名，如 {@code svg}、{@code png}。
     */
    private String extension;

    /**
     * COS 对象键前缀，如 {@code mermaid}、{@code iconify}。
     */
    private String keyPrefix;

    public static ImagePayload url(String url) {
        return ImagePayload.builder()
                .type(ImagePayloadType.URL)
                .content(url)
                .build();
    }

    public static ImagePayload base64(byte[] data, String contentType, String extension, String keyPrefix) {
        if (data == null || data.length == 0) {
            return null;
        }
        return ImagePayload.builder()
                .type(ImagePayloadType.BASE64)
                .content(Base64.getEncoder().encodeToString(data))
                .contentType(contentType)
                .extension(extension)
                .keyPrefix(keyPrefix)
                .build();
    }

    public boolean hasContent() {
        return type != null && StringUtils.hasText(content);
    }
}
