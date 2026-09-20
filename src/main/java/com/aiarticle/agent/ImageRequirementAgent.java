package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 智能体4：读取主标题与正文，非流式分析配图需求，
 * 并将结果写入 {@link ArticleState#getImageRequirements()}。
 */
@Slf4j
@Component
public class ImageRequirementAgent {

    @Resource
    private AiModelClient aiModelClient;

    /**
     * @param state 至少包含智能体1的 title 与智能体3的 content
     * @return 同一份 state，已填入 imageRequirements
     */
    public ArticleState generate(ArticleState state) {
        TitleResult title = state == null ? null : state.getTitle();
        ThrowUtils.throwIf(title == null || !StringUtils.hasText(title.getMainTitle()),
                ErrorCode.PARAMS_ERROR, "标题结果不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(state.getContent()),
                ErrorCode.PARAMS_ERROR, "正文内容不能为空");

        String prompt = PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT
                .replace("{mainTitle}", title.getMainTitle())
                .replace("{content}", state.getContent());
        String raw = aiModelClient.callLlm(prompt);
        List<ImageRequirement> requirements = aiModelClient.parseJsonListResponse(
                raw, new TypeToken<List<ImageRequirement>>() {}, "配图需求");
        state.setImageRequirements(requirements);

        log.info("智能体4完成, taskId={}, requirementCount={}",
                state.getTaskId(), requirements == null ? 0 : requirements.size());
        return state;
    }
}
