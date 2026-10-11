package com.aiarticle.service.image;

import com.aiarticle.constant.PromptConstant;
import com.aiarticle.enums.ImageMethodEnum;
import com.aiarticle.util.AiModelClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 大模型 AI 生图：生成 SVG 概念示意图，返回 Base64，由 {@link ImageCloudStore} 上传。
 */
@Slf4j
@Service
public class SvgDiagramService implements ImageSearchService {

    private static final Pattern FENCED_SVG = Pattern.compile(
            "```(?:svg|xml)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final AiModelClient aiModelClient;

    public SvgDiagramService(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    @Override
    public ImagePayload searchImage(ImageSearchRequest request) {
        String requirement = request == null ? null : request.resolveQuery();
        if (!StringUtils.hasText(requirement)) {
            return null;
        }
        try {
            String raw = aiModelClient.callLlm(
                    PromptConstant.SVG_DIAGRAM_GENERATION_PROMPT.replace("{requirement}", requirement.trim()));
            String svg = extractSvg(raw);
            if (!StringUtils.hasText(svg)) {
                log.warn("大模型未返回可用 SVG");
                return null;
            }
            return ImagePayload.base64(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml", "svg", "svg-diagram");
        } catch (RuntimeException e) {
            log.warn("SVG 示意图生成失败, error={}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getSearchMethod() {
        return ImageMethodEnum.AI_GENERATE.getValue();
    }

    @Override
    public String getFallbackImageUrl(int position) {
        return null;
    }

    static String extractSvg(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String text = raw.trim();
        Matcher matcher = FENCED_SVG.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).trim();
        }
        int svgStart = text.toLowerCase().indexOf("<svg");
        if (svgStart < 0) {
            return null;
        }
        int xmlStart = text.indexOf("<?xml");
        if (xmlStart >= 0 && xmlStart < svgStart) {
            return text.substring(xmlStart).trim();
        }
        return text.substring(svgStart).trim();
    }
}
