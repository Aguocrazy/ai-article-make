package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TitleAgentTest {

    @Mock
    private AiModelClient aiModelClient;

    @InjectMocks
    private TitleAgent titleAgent;

    @Test
    void generate_replacesTopic_callsNonStreaming_setsTitle() {
        ArticleState state = new ArticleState();
        state.setTopic("AI 如何改变工作");
        TitleResult expected = new TitleResult();
        expected.setMainTitle("主标题");
        expected.setSubTitle("副标题");
        when(aiModelClient.callLlm(anyString())).thenReturn("{\"mainTitle\":\"主标题\",\"subTitle\":\"副标题\"}");
        when(aiModelClient.parseJsonResponse(anyString(), eq(TitleResult.class), eq("标题"))).thenReturn(expected);

        ArticleState result = titleAgent.generate(state);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlm(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertEquals(PromptConstant.AGENT1_TITLE_PROMPT.replace("{topic}", "AI 如何改变工作"), prompt);
        verify(aiModelClient).parseJsonResponse(
                "{\"mainTitle\":\"主标题\",\"subTitle\":\"副标题\"}", TitleResult.class, "标题");
        assertSame(expected, result.getTitle());
        assertSame(state, result);
    }

    @Test
    void generate_rejectsBlankTopic() {
        ArticleState state = new ArticleState();
        state.setTopic("  ");
        assertThrows(BusinessException.class, () -> titleAgent.generate(state));
    }
}
