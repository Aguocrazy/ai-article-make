package com.aiarticle.service.image;

import com.aiarticle.enums.ImageMethodEnum;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.util.CosFileClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Iconify 生图策略：按关键词检索图标，拉取 SVG，优先上传 COS。
 * 尚未接入智能体分流，默认配图仍走 Pexels。
 *
 * @see <a href="https://iconify.design/docs/api/search.html">Iconify /search</a>
 * @see <a href="https://iconify.design/docs/api/svg.html">Iconify SVG</a>
 */
@Slf4j
@Service
public class IconifyService implements ImageSearchService {

    private final RestClient restClient;

    private final IconifyProperties properties;

    private final CosFileClient cosFileClient;

    public IconifyService(
            RestClient.Builder restClientBuilder,
            IconifyProperties properties,
            CosFileClient cosFileClient) {
        this.properties = properties;
        this.cosFileClient = cosFileClient;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public String searchImage(ImageSearchRequest request) {
        String query = request == null ? null : request.resolveQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        try {
            IconifySearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("query", query)
                            .queryParam("limit", Math.max(properties.getLimit(), 32))
                            .build())
                    .retrieve()
                    .body(IconifySearchResponse.class);
            if (response == null || response.icons() == null || response.icons().isEmpty()) {
                return null;
            }
            String iconId = response.icons().getFirst();
            int colon = iconId.indexOf(':');
            if (colon <= 0 || colon == iconId.length() - 1) {
                log.warn("Iconify 图标名无法解析: {}", iconId);
                return null;
            }
            String prefix = iconId.substring(0, colon);
            String name = iconId.substring(colon + 1);
            String publicSvgUrl = properties.getBaseUrl().replaceAll("/+$", "") + "/" + prefix + "/" + name + ".svg";
            String svg = restClient.get()
                    .uri("/{prefix}/{name}.svg", prefix, name)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(svg)) {
                return publicSvgUrl;
            }
            try {
                CosFileClient.CosUploadResult uploaded = cosFileClient.upload(
                        svg.getBytes(StandardCharsets.UTF_8),
                        "iconify/" + UUID.randomUUID() + ".svg",
                        "image/svg+xml");
                return uploaded == null ? publicSvgUrl : uploaded.url();
            } catch (BusinessException e) {
                log.warn("Iconify SVG 未上传 COS，改用公开地址, error={}", e.getMessage());
                return publicSvgUrl;
            }
        } catch (RestClientException e) {
            log.warn("Iconify 检索失败, query={}, error={}", query, e.getMessage());
            return null;
        }
    }

    @Override
    public String getSearchMethod() {
        return ImageMethodEnum.ICONIFY.getValue();
    }

    @Override
    public String getFallbackImageUrl(int position) {
        return null;
    }

    private record IconifySearchResponse(List<String> icons, Integer total) {
    }
}
