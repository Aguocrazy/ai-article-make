package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 智能体1：根据选题非流式生成主标题、副标题，写入 {@link ArticleState#getTitle()}。
 */
@Slf4j
@Component
public class TitleAgent {

    @Resource
    private AiModelClient aiModelClient;

    /**
     * @param state 至少包含 topic
     * @return 同一份 state，已填入 title
     */
    public ArticleState generate(ArticleState state) {
        ThrowUtils.throwIf(state == null || !StringUtils.hasText(state.getTopic()),
                ErrorCode.PARAMS_ERROR, "选题不能为空");
        String prompt = PromptConstant.AGENT1_TITLE_PROMPT.replace("{topic}", state.getTopic());
        String raw = aiModelClient.callLlm(prompt);
        TitleResult title = aiModelClient.parseJsonResponse(raw, TitleResult.class, "标题");
        state.setTitle(title);
        log.info("智能体1完成, taskId={}, mainTitle={}", state.getTaskId(), title.getMainTitle());
        return state;
    }
}
