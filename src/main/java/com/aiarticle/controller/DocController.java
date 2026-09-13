package com.aiarticle.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 接口文档入口：将 /api/doc.html 重定向到 Knife4j 文档页 /doc.html
 */
@Controller
@RequestMapping("/api")
public class DocController {

    @GetMapping("/doc.html")
    public String doc() {
        return "redirect:/doc.html";
    }
}