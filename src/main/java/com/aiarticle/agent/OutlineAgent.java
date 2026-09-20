package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.OutlineResult;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

/**
 * 智能体2：读取智能体1生成的标题，流式生成文章大纲，
 * 并将最终 JSON 解析后写入 {@link ArticleState#getOutline()}。
 */
@Slf4j
@Component
public class OutlineAgent {

    @Resource
    private AiModelClient aiModelClient;

    /**
     * @param state         至少包含智能体1生成的 title
     * @param streamHandler 接收带 {@code AGENT2_STREAMING:} 前缀的大纲增量
     * @return 同一份 state，已填入 outline
     */
    public ArticleState generate(ArticleState state, Consumer<String> streamHandler) {
        TitleResult title = state == null ? null : state.getTitle();
        ThrowUtils.throwIf(title == null
                        || !StringUtils.hasText(title.getMainTitle())
                        || !StringUtils.hasText(title.getSubTitle()),
                ErrorCode.PARAMS_ERROR, "标题结果不能为空");
        ThrowUtils.throwIf(streamHandler == null, ErrorCode.PARAMS_ERROR, "流式处理器不能为空");

        String prompt = PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", title.getMainTitle())
                .replace("{subTitle}", title.getSubTitle());
        String raw = aiModelClient.callLlmWithStreaming(
                prompt, streamHandler, SseMessageTypeEnum.AGENT2_STREAMING);
        OutlineResult outline = aiModelClient.parseJsonResponse(raw, OutlineResult.class, "大纲");
        state.setOutline(outline);

        log.info("智能体2完成, taskId={}, sectionCount={}",
                state.getTaskId(),
                outline.getSections() == null ? 0 : outline.getSections().size());
        return state;
    }
}
