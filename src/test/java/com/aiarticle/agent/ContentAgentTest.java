package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.OutlineResult;
import com.aiarticle.model.state.ArticleState.OutlineSection;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import com.aiarticle.util.GsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentAgentTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private Consumer<String> streamHandler;

    @InjectMocks
    private ContentAgent contentAgent;

    @Test
    void generate_usesTitleAndOutline_streamsAndSetsContent() {
        ArticleState state = createState();
        String markdown = "## 第一章\n正文";
        when(aiModelClient.callLlmWithStreaming(
                anyString(), eq(streamHandler), eq(SseMessageTypeEnum.AGENT3_STREAMING)))
                .thenReturn(markdown);

        ArticleState result = contentAgent.generate(state, streamHandler);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlmWithStreaming(
                promptCaptor.capture(), eq(streamHandler), eq(SseMessageTypeEnum.AGENT3_STREAMING));
        String expectedPrompt = PromptConstant.AGENT3_CONTENT_PROMPT
                .replace("{mainTitle}", "主标题")
                .replace("{subTitle}", "副标题")
                .replace("{outline}", GsonUtils.toJson(state.getOutline()));
        assertEquals(expectedPrompt, promptCaptor.getValue());
        assertEquals(markdown, state.getContent());
        assertSame(state, result);
    }

    @Test
    void generate_rejectsMissingOutline() {
        ArticleState state = createState();
        state.setOutline(null);
        assertThrows(BusinessException.class, () -> contentAgent.generate(state, streamHandler));
    }

    @Test
    void generate_rejectsNullStreamHandler() {
        assertThrows(BusinessException.class, () -> contentAgent.generate(createState(), null));
    }

    private ArticleState createState() {
        TitleResult title = new TitleResult();
        title.setMainTitle("主标题");
        title.setSubTitle("副标题");

        OutlineSection section = new OutlineSection();
        section.setSection(1);
        section.setTitle("第一章");
        section.setPoints(List.of("要点1", "要点2"));
        OutlineResult outline = new OutlineResult();
        outline.setSections(List.of(section));

        ArticleState state = new ArticleState();
        state.setTitle(title);
        state.setOutline(outline);
        return state;
    }
}
