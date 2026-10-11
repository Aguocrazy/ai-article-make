package com.aiarticle.service.image;

import com.aiarticle.config.MermaidConfig;
import com.aiarticle.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mermaid 生图策略：调用本机 mmdc 渲染，返回 Base64，由 {@link ImageCloudStore} 上传。
 * <p>
 * 兼容 mmdc v10+：尺寸参数为 {@code --size}（旧版 {@code -w} 已移除）。
 */
@Slf4j
@Service
public class MermaidService implements ImageSearchService {

    private final MermaidConfig mermaidConfig;

    public MermaidService(MermaidConfig mermaidConfig) {
        this.mermaidConfig = mermaidConfig;
    }

    @Override
    public ImagePayload searchImage(ImageSearchRequest request) {
        String source = request == null ? null : request.resolveQuery();
        if (!StringUtils.hasText(source)) {
            return null;
        }
        Path dir = null;
        try {
            String format = StringUtils.hasText(mermaidConfig.getOutputFormat())
                    ? mermaidConfig.getOutputFormat()
                    : "svg";
            dir = Files.createTempDirectory("mermaid-");
            Path input = dir.resolve("diagram.mmd");
            Path output = dir.resolve("diagram." + format);
            Files.writeString(input, source, StandardCharsets.UTF_8);

            List<String> command = buildCommand(input, output, format);
            log.info("Mermaid 渲染命令: {}", String.join(" ", command));
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            long timeout = mermaidConfig.getTimeout() == null ? 30000L : mermaidConfig.getTimeout();
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            String logs = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Mermaid 渲染超时");
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
                throw new IllegalStateException("Mermaid 渲染失败: " + logs);
            }

            byte[] bytes = Files.readAllBytes(output);
            return ImagePayload.base64(bytes, contentType(format), format, "mermaid");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Mermaid 生图被中断, sourceLen={}", source.length());
            return null;
        } catch (RuntimeException | IOException e) {
            log.error("Mermaid 生图失败, sourceLen={}, error={}", source.length(), e.toString(), e);
            return null;
        } finally {
            deleteQuietly(dir);
        }
    }

    /**
     * 组装 mmdc 命令。新版 CLI 使用 {@code --size}，不再支持 {@code -w}。
     */
    List<String> buildCommand(Path input, Path output, String format) {
        List<String> command = new ArrayList<>();
        command.add(StringUtils.hasText(mermaidConfig.getCliCommand()) ? mermaidConfig.getCliCommand() : "mmdc");
        command.add("-i");
        command.add(input.toAbsolutePath().toString());
        command.add("-o");
        command.add(output.toAbsolutePath().toString());
        command.add("-e");
        command.add(format);
        if (StringUtils.hasText(mermaidConfig.getBackgroundColor())) {
            command.add("-b");
            command.add(mermaidConfig.getBackgroundColor());
        }
        Integer width = mermaidConfig.getWidth();
        if (width != null && width > 0) {
            command.add("--size");
            command.add(String.valueOf(width));
        }
        resolvePuppeteerConfig().ifPresent(configPath -> {
            command.add("-p");
            command.add(configPath);
        });
        return command;
    }

    private java.util.Optional<String> resolvePuppeteerConfig() {
        if (!StringUtils.hasText(mermaidConfig.getPuppeteerConfigFile())) {
            return java.util.Optional.empty();
        }
        Path configured = Path.of(mermaidConfig.getPuppeteerConfigFile().trim());
        Path path = configured.isAbsolute()
                ? configured
                : Path.of(System.getProperty("user.dir")).resolve(configured).normalize();
        if (!Files.isRegularFile(path)) {
            log.warn("Mermaid Puppeteer 配置文件不存在: {}", path.toAbsolutePath());
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(path.toAbsolutePath().toString());
    }

    @Override
    public String getSearchMethod() {
        return ImageMethodEnum.MERMAID.getValue();
    }

    @Override
    public String getFallbackImageUrl(int position) {
        return null;
    }

    private static String contentType(String format) {
        if ("png".equalsIgnoreCase(format)) {
            return "image/png";
        }
        if ("pdf".equalsIgnoreCase(format)) {
            return "application/pdf";
        }
        return "image/svg+xml";
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // ignore
                }
            });
        } catch (IOException ignored) {
            // ignore
        }
    }
}
