package com.aiarticle.agent;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageRequirement;
import com.aiarticle.model.state.ArticleState.TitleResult;
import com.aiarticle.util.AiModelClient;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageRequirementAgentTest {

    @Mock
    private AiModelClient aiModelClient;

    @InjectMocks
    private ImageRequirementAgent imageRequirementAgent;

    @Test
    void generate_usesTitleAndContent_callsNonStreamingAndSetsRequirements() {
        ArticleState state = createState();
        String raw = "[{\"position\":1,\"type\":\"cover\"}]";
        ImageRequirement requirement = new ImageRequirement();
        requirement.setPosition(1);
        requirement.setType("cover");
        List<ImageRequirement> expected = List.of(requirement);
        when(aiModelClient.callLlm(anyString())).thenReturn(raw);
        when(aiModelClient.parseJsonListResponse(eq(raw), any(TypeToken.class), eq("配图需求")))
                .thenReturn(expected);

        ArticleState result = imageRequirementAgent.generate(state);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).callLlm(promptCaptor.capture());
        assertEquals(PromptConstant.AGENT4_IMAGE_REQUIREMENTS_PROMPT
                .replace("{mainTitle}", "主标题")
                .replace("{content}", "## 第一章\n正文"), promptCaptor.getValue());
        assertSame(expected, state.getImageRequirements());
        assertSame(state, result);
    }

    @Test
    void generate_rejectsMissingContent() {
        ArticleState state = createState();
        state.setContent(" ");
        assertThrows(BusinessException.class, () -> imageRequirementAgent.generate(state));
    }

    @Test
    void generate_rejectsMissingTitle() {
        ArticleState state = createState();
        state.setTitle(null);
        assertThrows(BusinessException.class, () -> imageRequirementAgent.generate(state));
    }

    private ArticleState createState() {
        TitleResult title = new TitleResult();
        title.setMainTitle("主标题");
        title.setSubTitle("副标题");

        ArticleState state = new ArticleState();
        state.setTitle(title);
        state.setContent("## 第一章\n正文");
        return state;
    }
}
