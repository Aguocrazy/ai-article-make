package com.aiarticle.controller;

import com.aiarticle.common.BaseResponse;
import com.aiarticle.model.dto.article.ArticleCreateRequest;
import com.aiarticle.model.entity.User;
import com.aiarticle.model.vo.ArticleTaskVO;
import com.aiarticle.service.ArticleGenerationService;
import com.aiarticle.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleGenerationService articleGenerationService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest servletRequest;

    private ArticleController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new ArticleController(articleGenerationService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void create_bindsCurrentLoginUserAndDelegates() {
        User user = User.builder().id(99L).build();
        ArticleTaskVO task = new ArticleTaskVO("task-99");
        when(userService.getLoginUser(servletRequest)).thenReturn(user);
        when(articleGenerationService.create("主题", 99L)).thenReturn(task);

        BaseResponse<ArticleTaskVO> response =
                controller.create(new ArticleCreateRequest("主题"), servletRequest);

        assertEquals(0, response.getCode());
        assertSame(task, response.getData());
        verify(articleGenerationService).create("主题", 99L);
    }

    @Test
    void stream_bindsCurrentLoginUserAndReturnsEmitterDirectly() {
        User user = User.builder().id(99L).build();
        SseEmitter emitter = new SseEmitter();
        when(userService.getLoginUser(servletRequest)).thenReturn(user);
        when(articleGenerationService.subscribe("task-99", 99L)).thenReturn(emitter);

        SseEmitter result = controller.stream("task-99", servletRequest);

        assertSame(emitter, result);
        verify(articleGenerationService).subscribe("task-99", 99L);
    }

    @Test
    void streamMappingProducesTextEventStream() throws Exception {
        Method method = ArticleController.class.getMethod(
                "stream", String.class, HttpServletRequest.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertArrayEquals(new String[]{"/stream/{taskId}"}, mapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
        assertEquals(SseEmitter.class, method.getReturnType());
    }

    @Test
    void createMappingAcceptsPostJsonAndBindsDtoThroughSpringMvc() throws Exception {
        User user = User.builder().id(101L).build();
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);
        when(articleGenerationService.create("Spring MVC 主题", 101L))
                .thenReturn(new ArticleTaskVO("task-101"));

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic":"Spring MVC 主题"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskId").value("task-101"));

        verify(userService).getLoginUser(any(HttpServletRequest.class));
        verify(articleGenerationService).create("Spring MVC 主题", 101L);
    }

    @Test
    void createMethodIsMappedToPostEndpoint() throws Exception {
        Method method = ArticleController.class.getMethod(
                "create", ArticleCreateRequest.class, HttpServletRequest.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertArrayEquals(new String[]{"/create"}, mapping.value());
    }

    @Test
    void streamMappingBindsPathAndProducesEventStreamThroughSpringMvc() throws Exception {
        User user = User.builder().id(102L).build();
        SseEmitter emitter = new SseEmitter();
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);
        when(articleGenerationService.subscribe("task-102", 102L)).thenReturn(emitter);

        MvcResult result = mockMvc.perform(get("/article/stream/task-102")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        verify(userService).getLoginUser(any(HttpServletRequest.class));
        verify(articleGenerationService).subscribe("task-102", 102L);
        emitter.complete();
        result.getAsyncResult(1_000);

        mockMvc.perform(get("/article/stream/task-102")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotAcceptable());
    }
}
