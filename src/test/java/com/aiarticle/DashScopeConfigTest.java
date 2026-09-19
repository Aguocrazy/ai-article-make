package com.aiarticle;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 DashScope（通义千问）配置是否注入成功。
 * 未设置真实 DASHSCOPE_API_KEY 时，只检查 Bean；设置后会再打一次极短对话。
 */
@SpringBootTest
class DashScopeConfigTest {

    private static final String PLACEHOLDER_KEY = "your-api-key-here";

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Test
    void dashScopeChatModelIsConfigured() {
        assertNotNull(dashScopeChatModel, "DashScopeChatModel 未注入，请检查 starter 依赖与 spring.ai.dashscope.api-key");
        assertNotNull(apiKey);
        assertFalse(apiKey.isBlank(), "spring.ai.dashscope.api-key 为空");
        System.out.println("DashScope 配置已生效：ChatModel=" + dashScopeChatModel.getClass().getSimpleName()
                + ", api-key 长度=" + apiKey.length()
                + ", 是否占位符=" + PLACEHOLDER_KEY.equals(apiKey));
    }

    @Test
    void simpleChatWhenApiKeyIsReal() {
        Assumptions.assumeFalse(PLACEHOLDER_KEY.equals(apiKey) || apiKey.isBlank(),
                "未配置真实 DASHSCOPE_API_KEY，跳过模型调用");
        ChatResponse response = dashScopeChatModel.call(new Prompt("只回复一个词：pong"));
        assertNotNull(response);
        assertNotNull(response.getResult());
        String text = response.getResult().getOutput().getText();
        assertNotNull(text);
        assertFalse(text.isBlank());
        System.out.println("模型回复: " + text);
    }
}
