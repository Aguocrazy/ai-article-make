package com.aiarticle.controller;

import com.aiarticle.common.BaseResponse;
import com.aiarticle.common.ResultUtils;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.dto.article.ArticleCreateRequest;
import com.aiarticle.model.entity.User;
import com.aiarticle.model.vo.ArticleTaskVO;
import com.aiarticle.service.ArticleGenerationService;
import com.aiarticle.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 文章生成接口。
 */
@Tag(name = "文章接口", description = "创建文章生成任务及订阅生成进度")
@RestController
@RequestMapping("/article")
public class ArticleController {

    private final ArticleGenerationService articleGenerationService;
    private final UserService userService;

    public ArticleController(ArticleGenerationService articleGenerationService, UserService userService) {
        this.articleGenerationService = articleGenerationService;
        this.userService = userService;
    }

    /**
     * 创建文章生成任务。
     */
    @Operation(summary = "创建文章生成任务", description = "异步创建文章，并返回任务 ID")
    @PostMapping("/create")
    public BaseResponse<ArticleTaskVO> create(
            @RequestBody ArticleCreateRequest articleCreateRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(articleCreateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ArticleTaskVO task = articleGenerationService.create(
                articleCreateRequest.topic(), loginUser.getId());
        return ResultUtils.success(task);
    }

    /**
     * 订阅文章生成进度。
     */
    @Operation(summary = "订阅文章生成进度", description = "通过 SSE 实时接收文章生成事件")
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String taskId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return articleGenerationService.subscribe(taskId, loginUser.getId());
    }
}
