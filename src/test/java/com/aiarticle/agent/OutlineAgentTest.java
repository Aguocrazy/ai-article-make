package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.OutlineResult;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutlineAgentTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private Consumer<String> streamHandler;

    @InjectMocks
    private OutlineAgent outlineAgent;

    @Test
    void generate_usesTitle_streamsAndSetsOutline() {
        ArticleState state = new ArticleState();
        TitleResult title = new TitleResult();
        title.setMainTitle("主标题");
        title.setSubTitle("副标题");
        state.setTitle(title);

        String raw = "{\"sections\":[]}";
        OutlineResult expected = new OutlineResult();
        when(aiModelClient.callLlmWithStreaming(
                anyString(), eq(streamHandler), eq(SseMessageTypeEnum.AGENT2_STREAMING))).thenReturn(raw);
        when(aiModelClient.parseJsonResponse(raw, OutlineResult.class, "大纲")).thenReturn(expected);

        ArticleState result = outlineAgent.generate(state, streamHandler);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlmWithStreaming(
                promptCaptor.capture(), eq(streamHandler), eq(SseMessageTypeEnum.AGENT2_STREAMING));
        assertEquals(PromptConstant.AGENT2_OUTLINE_PROMPT
                .replace("{mainTitle}", "主标题")
                .replace("{subTitle}", "副标题"), promptCaptor.getValue());
        assertSame(expected, state.getOutline());
        assertSame(state, result);
    }

    @Test
    void generate_rejectsMissingTitle() {
        assertThrows(BusinessException.class,
                () -> outlineAgent.generate(new ArticleState(), streamHandler));
    }

    @Test
    void generate_rejectsNullStreamHandler() {
        ArticleState state = new ArticleState();
        state.setTitle(new TitleResult());
        assertThrows(BusinessException.class, () -> outlineAgent.generate(state, null));
    }
}
