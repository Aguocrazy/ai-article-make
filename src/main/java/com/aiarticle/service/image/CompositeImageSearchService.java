package com.aiarticle.service.image;

import com.aiarticle.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

/**
 * 按 {@link ImageSearchRequest#getMethod()} 分发到各配图策略。
 * 未指定或未实现的 method 回落 Pexels。
 */
@Slf4j
@Service
@Primary
public class CompositeImageSearchService implements ImageSearchService {

    private final PexelsImageSearchService pexelsImageSearchService;

    private final Map<String, ImageSearchService> strategies;

    public CompositeImageSearchService(
            PexelsImageSearchService pexelsImageSearchService,
            MermaidService mermaidService,
            IconifyService iconifyService,
            SvgDiagramService svgDiagramService) {
        this.pexelsImageSearchService = pexelsImageSearchService;
        this.strategies = Map.of(
                ImageMethodEnum.PEXELS.getValue(), pexelsImageSearchService,
                ImageMethodEnum.MERMAID.getValue(), mermaidService,
                ImageMethodEnum.ICONIFY.getValue(), iconifyService,
                ImageMethodEnum.SVG_DIAGRAM.getValue(), svgDiagramService);
    }

    @Override
    public ImagePayload searchImage(ImageSearchRequest request) {
        return resolve(request).searchImage(request);
    }

    @Override
    public String getSearchMethod() {
        return pexelsImageSearchService.getSearchMethod();
    }

    @Override
    public String getFallbackImageUrl(int position) {
        return pexelsImageSearchService.getFallbackImageUrl(position);
    }

    private ImageSearchService resolve(ImageSearchRequest request) {
        String method = request == null || !StringUtils.hasText(request.getMethod())
                ? ImageMethodEnum.getDefaultSearchMethod().getValue()
                : request.getMethod().trim().toUpperCase(Locale.ROOT);
        ImageSearchService strategy = strategies.get(method);
        if (strategy == null) {
            log.warn("配图方式 {} 未实现，回落 Pexels", method);
            return pexelsImageSearchService;
        }
        return strategy;
    }
}
