package com.aiarticle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mermaid 图表生成配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mermaid")
public class MermaidConfig {

    /**
     * CLI 命令（Windows 下为 mmdc.cmd，Linux/Mac 下为 mmdc）
     */
    private String cliCommand = "mmdc";

    /**
     * 背景颜色（transparent 为透明背景）
     */
    private String backgroundColor = "transparent";

    /**
     * 输出格式（svg/png/pdf）
     */
    private String outputFormat = "svg";

    /**
     * 图片最大宽/高（像素），对应 mmdc {@code --size}。
     */
    private Integer width = 1200;

    /**
     * 命令执行超时时间（毫秒）
     */
    private Long timeout = 30000L;

    /**
     * Puppeteer 配置文件路径（mmdc {@code -p}）。
     * 用于指定本机 Chrome，避免缺失 chrome-headless-shell。
     */
    private String puppeteerConfigFile = "config/mermaid-puppeteer.json";
}
