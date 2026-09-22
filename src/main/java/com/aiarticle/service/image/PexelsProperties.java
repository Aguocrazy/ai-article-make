package com.aiarticle.service.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片检索配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.pexels")
public class PexelsProperties {

    /**
     * Pexels API Key，通过 PEXELS_API_KEY 占位符注入。
     */
    private String apiKey;

    /**
     * Pexels API 根地址。
     */
    private String baseUrl = "https://api.pexels.com";

    /**
     * 单次搜索数量。当前只取第一张，保留该配置便于后续扩展随机选择。
     */
    private int perPage = 1;

    /**
     * API 无结果或不可用时使用的固定图片 URL。
     */
    private List<String> fallbackUrls = new ArrayList<>();
}
