package com.aiarticle.service.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Pexels 图片检索实现。
 * <p>
 * 按官方 API 要求通过 {@code Authorization} 请求头鉴权，调用
 * {@code GET /v1/search}，优先返回 1200×627 的 {@code src.landscape}。
 * API 异常或无结果时返回 {@code null}，由智能体5调用固定降级图片。
 */
@Slf4j
@Service
public class PexelsImageSearchService implements ImageSearchService {

    private static final String SEARCH_METHOD = "PEXELS";

    private final RestClient restClient;

    private final PexelsProperties properties;

    public PexelsImageSearchService(RestClient.Builder restClientBuilder, PexelsProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public String searchImage(String keywords) {
        if (!StringUtils.hasText(keywords) || !StringUtils.hasText(properties.getApiKey())) {
            log.warn("Pexels 图片检索已跳过：关键词或 API Key 为空");
            return null;
        }
        try {
            PexelsSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search")
                            .queryParam("query", keywords)
                            .queryParam("orientation", "landscape")
                            .queryParam("per_page", properties.getPerPage())
                            .build())
                    .header("Authorization", properties.getApiKey())
                    .retrieve()
                    .body(PexelsSearchResponse.class);
            if (response == null || response.photos() == null || response.photos().isEmpty()) {
                return null;
            }
            PexelsPhotoSource source = response.photos().getFirst().src();
            if (source == null) {
                return null;
            }
            return StringUtils.hasText(source.landscape()) ? source.landscape() : source.original();
        } catch (RestClientException e) {
            log.warn("Pexels 图片检索失败, keywords={}, error={}", keywords, e.getMessage());
            return null;
        }
    }

    @Override
    public String getSearchMethod() {
        return SEARCH_METHOD;
    }

    @Override
    public String getFallbackImageUrl(int position) {
        List<String> fallbackUrls = properties.getFallbackUrls();
        if (fallbackUrls == null || fallbackUrls.isEmpty()) {
            log.error("未配置 Pexels 降级图片");
            return null;
        }
        int index = Math.floorMod(position - 1, fallbackUrls.size());
        return fallbackUrls.get(index);
    }

    /**
     * Pexels 搜索响应中本项目需要的最小字段集合。
     */
    private record PexelsSearchResponse(List<PexelsPhoto> photos) {
    }

    private record PexelsPhoto(PexelsPhotoSource src) {
    }

    private record PexelsPhotoSource(String original, String landscape) {
    }
}
