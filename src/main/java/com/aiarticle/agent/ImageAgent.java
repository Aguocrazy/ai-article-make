package com.aiarticle.agent;

import com.aiarticle.enums.ImageMethodEnum;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.ImageResult;
import com.aiarticle.service.image.ImageCloudStore;
import com.aiarticle.service.image.ImagePayload;
import com.aiarticle.service.image.ImageSearchRequest;
import com.aiarticle.service.image.ImageSearchService;
import com.aiarticle.util.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 智能体5：按智能体4给出的配图需求顺序取图。
 * 优先走 AI_GENERATE / MERMAID；失败时用 PEXELS 兜底检索，再不行用固定图。
 */
@Slf4j
@Component
public class ImageAgent {

    @Resource
    private ImageSearchService imageSearchService;

    @Resource
    private ImageCloudStore imageCloudStore;

    /**
     * @param state         至少包含智能体4生成的 imageRequirements
     * @param streamHandler 接收带 {@code IMAGE_COMPLETE:} 前缀的单图结果
     * @return 同一份 state，已填入 images；position=1 的 URL 同时写入 coverImage
     */
    public ArticleState generate(ArticleState state, Consumer<String> streamHandler) {
        List<ImageRequirement> requirements = state == null ? null : state.getImageRequirements();
        ThrowUtils.throwIf(requirements == null || requirements.isEmpty(),
                ErrorCode.PARAMS_ERROR, "配图需求不能为空");
        ThrowUtils.throwIf(streamHandler == null,
                ErrorCode.PARAMS_ERROR, "进度处理器不能为空");

        List<ImageResult> results = new ArrayList<>(requirements.size());
        for (ImageRequirement requirement : requirements) {
            ImageResult result = search(requirement);
            results.add(result);
            if (Integer.valueOf(1).equals(result.getPosition())) {
                state.setCoverImage(result.getUrl());
            }
            streamHandler.accept(SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                    + GsonUtils.toJson(result));
        }
        state.setImages(results);

        log.info("智能体5完成, taskId={}, imageCount={}", state.getTaskId(), results.size());
        return state;
    }

    private ImageResult search(ImageRequirement requirement) {
        ThrowUtils.throwIf(requirement == null || requirement.getPosition() == null,
                ErrorCode.PARAMS_ERROR, "配图需求参数不完整");
        ThrowUtils.throwIf(!StringUtils.hasText(requirement.getKeywords())
                        && !StringUtils.hasText(requirement.getPrompt()),
                ErrorCode.PARAMS_ERROR, "配图需求缺少 keywords 或 prompt");

        String requestedMethod = StringUtils.hasText(requirement.getMethod())
                ? requirement.getMethod().trim().toUpperCase(Locale.ROOT)
                : ImageMethodEnum.getDefaultSearchMethod().getValue();
        String fallbackMethod = ImageMethodEnum.getFallbackMethod().getValue();

        String url = tryGenerate(requirement, requestedMethod);
        String method = StringUtils.hasText(url) ? requestedMethod : null;

        if (!StringUtils.hasText(url) && !fallbackMethod.equalsIgnoreCase(requestedMethod)) {
            log.warn("主策略失败，改用 Pexels 兜底, position={}, method={}",
                    requirement.getPosition(), requestedMethod);
            url = tryGenerate(requirement, fallbackMethod);
            if (StringUtils.hasText(url)) {
                method = fallbackMethod;
            }
        }

        if (!StringUtils.hasText(url)) {
            url = imageSearchService.getFallbackImageUrl(requirement.getPosition());
            method = fallbackMethod;
        }
        ThrowUtils.throwIf(!StringUtils.hasText(url),
                ErrorCode.OPERATION_ERROR, "图片检索及降级均失败");

        ImageResult result = new ImageResult();
        result.setPosition(requirement.getPosition());
        result.setUrl(url);
        result.setMethod(method);
        result.setKeywords(requirement.getKeywords());
        result.setSectionTitle(requirement.getSectionTitle());
        result.setDescription(requirement.getType());
        return result;
    }

    private String tryGenerate(ImageRequirement requirement, String method) {
        try {
            ImagePayload payload = imageSearchService.searchImage(toSearchRequest(requirement, method));
            return imageCloudStore.toPublicUrl(payload);
        } catch (RuntimeException e) {
            log.warn("图片生成失败, position={}, method={}, error={}",
                    requirement.getPosition(), method, e.getMessage());
            return null;
        }
    }

    private static ImageSearchRequest toSearchRequest(ImageRequirement requirement, String method) {
        return ImageSearchRequest.builder()
                .position(requirement.getPosition())
                .type(requirement.getType())
                .sectionTitle(requirement.getSectionTitle())
                .keywords(requirement.getKeywords())
                .prompt(requirement.getPrompt())
                .method(method)
                .build();
    }
}
