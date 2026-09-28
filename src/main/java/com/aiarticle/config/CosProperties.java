package com.aiarticle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 腾讯云 COS 配置。密钥通过 {@code COS_SECRET_ID} / {@code COS_SECRET_KEY} 注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {

    private String secretId;

    private String secretKey;

    /**
     * 存储桶地域简称，例如 ap-guangzhou。
     */
    private String region = "ap-guangzhou";

    /**
     * 存储桶名称，格式一般为 {@code <name>-<appid>}。
     */
    private String bucket;

    /**
     * 对象键统一前缀，可为空。
     */
    private String keyPrefix = "";

    /**
     * 自定义访问域名（CDN 或静态网站），为空则使用 COS 默认域名。
     */
    private String customDomain = "";

    public boolean isConfigured() {
        return StringUtils.hasText(secretId)
                && StringUtils.hasText(secretKey)
                && StringUtils.hasText(region)
                && StringUtils.hasText(bucket);
    }
}
