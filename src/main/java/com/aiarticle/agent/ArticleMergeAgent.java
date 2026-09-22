package com.aiarticle.agent;

import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图文合成：逐行扫描 Markdown 正文，在匹配的二级标题后插入章节配图。
 * <p>
 * 本步骤不调用大模型。封面图单独保存在 coverImage，不插入正文。
 */
@Slf4j
@Component
public class ArticleMergeAgent {

    /**
     * @param state 至少包含 content；images 可为空
     * @return 同一份 state，已填入 fullContent
     */
    public ArticleState merge(ArticleState state) {
        ThrowUtils.throwIf(state == null || !StringUtils.hasText(state.getContent()),
                ErrorCode.PARAMS_ERROR, "正文内容不能为空");

        Map<String, ImageResult> imageBySection = indexImages(state.getImages());
        String[] lines = state.getContent().split("\\R", -1);
        List<String> mergedLines = new ArrayList<>(lines.length + imageBySection.size() * 3);

        int insertedCount = 0;
        for (String line : lines) {
            mergedLines.add(line);
            String heading = extractLevelTwoHeading(line);
            ImageResult image = heading == null ? null : imageBySection.get(heading);
            if (image != null) {
                mergedLines.add("");
                mergedLines.add(toMarkdownImage(image, heading));
                mergedLines.add("");
                insertedCount++;
            }
        }

        state.setFullContent(String.join("\n", mergedLines));
        log.info("图文合成完成, taskId={}, insertedImageCount={}", state.getTaskId(), insertedCount);
        return state;
    }

    private Map<String, ImageResult> indexImages(List<ImageResult> images) {
        Map<String, ImageResult> result = new LinkedHashMap<>();
        if (images == null) {
            return result;
        }
        for (ImageResult image : images) {
            if (image == null || !StringUtils.hasText(image.getUrl())) {
                continue;
            }
            String sectionTitle = normalizeSectionTitle(image.getSectionTitle());
            if (StringUtils.hasText(sectionTitle)) {
                // 同一章节只插入第一张图。
                result.putIfAbsent(sectionTitle, image);
            }
        }
        return result;
    }

    private String extractLevelTwoHeading(String line) {
        String trimmed = line.trim();
        if (trimmed.length() <= 3
                || !trimmed.startsWith("##")
                || !Character.isWhitespace(trimmed.charAt(2))) {
            return null;
        }
        return normalizeSectionTitle(trimmed);
    }

    private String normalizeSectionTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String normalized = title.trim();
        if (normalized.length() > 2
                && normalized.startsWith("##")
                && Character.isWhitespace(normalized.charAt(2))) {
            normalized = normalized.substring(3).trim();
        }
        return normalized;
    }

    private String toMarkdownImage(ImageResult image, String heading) {
        String alt = StringUtils.hasText(image.getDescription())
                ? image.getDescription().trim()
                : heading;
        alt = alt.replace("[", "").replace("]", "");
        return "![" + alt + "](" + image.getUrl() + ")";
    }
}
