package com.aiarticle.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试 Controller：用于验证 Knife4j 接口文档 与 Hutool 工具库
 */
@Tag(name = "测试接口", description = "用于验证接口文档与工具库的示例接口")
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * GET /api/test/hello
     */
    @Operation(summary = "打招呼接口", description = "返回问候语与当前时间（使用 Hutool 工具）")
    @GetMapping("/hello")
    public Map<String, Object> hello(
            @Parameter(description = "姓名", example = "张三")
            @RequestParam(required = false, defaultValue = "世界") String name) {

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("greeting", StrUtil.format("你好，{}！", name));
        result.put("time", DateUtil.now());
        result.put("requestId", IdUtil.fastSimpleUUID());

        return result;
    }

    /**
     * GET /api/test/user/{id}
     */
    @Operation(summary = "查询用户接口", description = "根据 ID 返回模拟用户信息（演示路径参数）")
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(
            @Parameter(description = "用户ID", example = "1001")
            @PathVariable Long id) {

        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", StrUtil.format("user_{}", id));
        user.put("nickname", "测试用户" + id);
        user.put("createdTime", DateUtil.now());

        return JSONUtil.parseObj(user);
    }
}