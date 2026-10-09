package com.aiarticle.service.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Iconify 公开 API 配置。检索文档：https://iconify.design/docs/api/queries.html
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.iconify")
public class IconifyProperties {

    /**
     * Iconify API 根地址。
     */
    private String baseUrl = "https://api.iconify.design";

    /**
     * 搜索条数。公开 API 最小值为 32，实际只用第一条。
     */
    private int limit = 32;
}
