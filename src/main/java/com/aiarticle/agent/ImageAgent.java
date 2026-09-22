package com.aiarticle.agent;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.ImageResult;
import com.aiarticle.service.image.ImageSearchService;
import com.aiarticle.util.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 智能体5：按智能体4给出的配图需求顺序检索图片。
 * 每完成一张图片就推送一次 {@code IMAGE_COMPLETE}，全部结果写回 ArticleState。
 */
@Slf4j
@Component
public class ImageAgent {

    @Resource
    private ImageSearchService imageSearchService;

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
        ThrowUtils.throwIf(requirement == null
                        || requirement.getPosition() == null
                        || !StringUtils.hasText(requirement.getKeywords()),
                ErrorCode.PARAMS_ERROR, "配图需求参数不完整");

        String url = null;
        String method = null;
        try {
            url = imageSearchService.searchImage(requirement.getKeywords());
            if (StringUtils.hasText(url)) {
                method = imageSearchService.getSearchMethod();
            }
        } catch (RuntimeException e) {
            log.warn("图片检索失败，改用降级图片, position={}, keywords={}, error={}",
                    requirement.getPosition(), requirement.getKeywords(), e.getMessage());
        }

        if (!StringUtils.hasText(url)) {
            url = imageSearchService.getFallbackImageUrl(requirement.getPosition());
            method = "FALLBACK";
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
}
