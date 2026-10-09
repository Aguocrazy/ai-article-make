package com.aiarticle.service.image;

import com.aiarticle.config.MermaidConfig;
import com.aiarticle.enums.ImageMethodEnum;
import com.aiarticle.util.CosFileClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Mermaid 生图策略：调用本机 mmdc 渲染后上传 COS。
 * 尚未接入智能体分流，默认配图仍走 Pexels。
 */
@Slf4j
@Service
public class MermaidService implements ImageSearchService {

    private final MermaidConfig mermaidConfig;

    private final CosFileClient cosFileClient;

    public MermaidService(MermaidConfig mermaidConfig, CosFileClient cosFileClient) {
        this.mermaidConfig = mermaidConfig;
        this.cosFileClient = cosFileClient;
    }

    @Override
    public String searchImage(ImageSearchRequest request) {
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

            Process process = new ProcessBuilder(
                    mermaidConfig.getCliCommand(),
                    "-i", input.toAbsolutePath().toString(),
                    "-o", output.toAbsolutePath().toString(),
                    "-b", mermaidConfig.getBackgroundColor(),
                    "-w", String.valueOf(mermaidConfig.getWidth() == null ? 1200 : mermaidConfig.getWidth()))
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
            CosFileClient.CosUploadResult uploaded = cosFileClient.upload(
                    bytes, "mermaid/" + UUID.randomUUID() + "." + format, contentType(format));
            return uploaded == null ? null : uploaded.url();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Mermaid 生图被中断");
            return null;
        } catch (RuntimeException | IOException e) {
            log.warn("Mermaid 生图失败, error={}", e.getMessage());
            return null;
        } finally {
            deleteQuietly(dir);
        }
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
