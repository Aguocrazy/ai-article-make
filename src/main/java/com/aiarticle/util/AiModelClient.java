package com.aiarticle.util;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * 大模型调用工具：封装通义千问的两种调用方式。
 * <ul>
 *   <li>非流式 {@link #callLlm(String)}：等模型整段生成完再返回，适合标题 / 大纲 / 配图需求（一次性 JSON）</li>
 *   <li>流式 {@link #callLlmWithStreaming}：边生成边吐片段，适合正文，便于 SSE 往前端推</li>
 * </ul>
 */
@Slf4j
@Component
public class AiModelClient {

    @Resource
    private DashScopeChatModel chatModel;

    @Value("${spring.ai.dashscope.chat.options.model:}")
    private String model;

    @Value("${spring.ai.dashscope.chat.options.multi-model:false}")
    private boolean multiModel;

    /**
     * 调用 LLM（非流式）
     */
    public String callLlm(String prompt) {
        ChatResponse response = chatModel.call(promptOf(prompt));
        return response.getResult().getOutput().getText();
    }

    /**
     * 调用 LLM（流式输出）
     */
    public String callLlmWithStreaming(String prompt, Consumer<String> streamHandler, SseMessageTypeEnum messageType) {
        StringBuilder contentBuilder = new StringBuilder();

        Flux<ChatResponse> streamResponse = chatModel.stream(promptOf(prompt));

        streamResponse
                .doOnNext(response -> {
                    String chunk = response.getResult().getOutput().getText();
                    if (StringUtils.hasText(chunk)) {
                        contentBuilder.append(chunk);
                        streamHandler.accept(messageType.getStreamingPrefix() + chunk);
                    }
                })
                .doOnError(error -> log.error("LLM 流式调用失败， messageType={}", messageType, error))
                .blockLast();
        return contentBuilder.toString();
    }

    /**
     * 每次请求带上 multiModel，避免 RC2 合并 options 时把 YAML 里的 true 盖成 false，
     * 从而打到文本端点触发 url error。
     */
    private Prompt promptOf(String prompt) {
        var options = DashScopeChatOptions.builder().withMultiModel(multiModel);
        if (StringUtils.hasText(model)) {
            options.withModel(model);
        }
        return new Prompt(new UserMessage(prompt), options.build());
    }

    /**
     * 解析 JSON 响应
     */
    public <T> T parseJsonResponse(String content, Class<T> clazz, String name) {
        try {
            return GsonUtils.fromJson(content, clazz);
        } catch (JsonSyntaxException e) {
            log.error("{}解析失败, content={}", name, content, e);
            throw new RuntimeException(name + "解析失败");
        }
    }

    /**
     * 解析 JSON 列表响应
     */
    public <T> T parseJsonListResponse(String content, TypeToken<T> typeToken, String name) {
        try {
            return GsonUtils.fromJson(content, typeToken);
        } catch (JsonSyntaxException e) {
            log.error("{}解析失败, content={}", name, content, e);
            throw new RuntimeException(name + "解析失败");
        }
    }
}
