package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.OutlineResult;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import com.aiarticle.util.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

/**
 * 智能体3：读取标题与大纲，流式生成 Markdown 正文，
 * 并将完整结果写入 {@link ArticleState#getContent()}。
 */
@Slf4j
@Component
public class ContentAgent {

    @Resource
    private AiModelClient aiModelClient;

    /**
     * @param state         至少包含智能体1的 title 与智能体2的 outline
     * @param streamHandler 接收带 {@code AGENT3_STREAMING:} 前缀的正文增量
     * @return 同一份 state，已填入 content
     */
    public ArticleState generate(ArticleState state, Consumer<String> streamHandler) {
        TitleResult title = state == null ? null : state.getTitle();
        OutlineResult outline = state == null ? null : state.getOutline();
        ThrowUtils.throwIf(title == null
                        || !StringUtils.hasText(title.getMainTitle())
                        || !StringUtils.hasText(title.getSubTitle()),
                ErrorCode.PARAMS_ERROR, "标题结果不能为空");
        ThrowUtils.throwIf(outline == null
                        || outline.getSections() == null
                        || outline.getSections().isEmpty(),
                ErrorCode.PARAMS_ERROR, "大纲结果不能为空");
        ThrowUtils.throwIf(streamHandler == null, ErrorCode.PARAMS_ERROR, "流式处理器不能为空");

        String prompt = PromptConstant.AGENT3_CONTENT_PROMPT
                .replace("{mainTitle}", title.getMainTitle())
                .replace("{subTitle}", title.getSubTitle())
                .replace("{outline}", GsonUtils.toJson(outline));
        String content = aiModelClient.callLlmWithStreaming(
                prompt, streamHandler, SseMessageTypeEnum.AGENT3_STREAMING);
        state.setContent(content);

        log.info("智能体3完成, taskId={}, contentLength={}",
                state.getTaskId(), content == null ? 0 : content.length());
        return state;
    }
}
