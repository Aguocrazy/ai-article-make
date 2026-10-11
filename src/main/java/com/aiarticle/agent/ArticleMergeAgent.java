package com.aiarticle.agent;

import com.aiarticle.exception.ErrorCode;
import com.aiarticle.exception.ThrowUtils;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图文合成：逐行扫描 Markdown 正文，在匹配的二级标题后插入章节配图；
 * 封面图插在正文最前。标题对不上时按章节顺序兜底插入。
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

        List<ImageResult> usableImages = usableImages(state.getImages());
        Map<String, ImageResult> imageBySection = indexBySectionTitle(usableImages);
        Set<ImageResult> used = new HashSet<>();

        String[] lines = state.getContent().split("\\R", -1);
        List<String> headingOrder = new ArrayList<>();
        for (String line : lines) {
            String heading = extractLevelTwoHeading(line);
            if (heading != null) {
                headingOrder.add(heading);
            }
        }

        List<String> mergedLines = new ArrayList<>(lines.length + usableImages.size() * 3);
        int titleMatched = 0;
        for (String line : lines) {
            mergedLines.add(line);
            String heading = extractLevelTwoHeading(line);
            if (heading == null) {
                continue;
            }
            ImageResult image = imageBySection.get(heading);
            if (image != null && used.add(image)) {
                appendMarkdownImage(mergedLines, image, heading);
                titleMatched++;
            }
        }

        int ordinalInserted = insertByHeadingOrder(mergedLines, headingOrder, usableImages, used);

        ImageResult cover = resolveCover(state, usableImages);
        if (cover != null) {
            List<String> withCover = new ArrayList<>(mergedLines.size() + 3);
            appendMarkdownImage(withCover, cover, "封面");
            withCover.add("");
            withCover.addAll(mergedLines);
            mergedLines = withCover;
            used.add(cover);
        }

        state.setFullContent(String.join("\n", mergedLines));
        log.info("图文合成完成, taskId={}, titleMatched={}, ordinalInserted={}, coverInserted={}, totalUsable={}, unused={}",
                state.getTaskId(),
                titleMatched,
                ordinalInserted,
                cover != null ? 1 : 0,
                usableImages.size(),
                usableImages.size() - used.size());
        if (usableImages.size() > used.size()) {
            for (ImageResult image : usableImages) {
                if (!used.contains(image)) {
                    log.warn("配图未插入正文, taskId={}, position={}, sectionTitle={}, url={}",
                            state.getTaskId(), image.getPosition(), image.getSectionTitle(), image.getUrl());
                }
            }
        }
        return state;
    }

    /**
     * 标题未命中的章节图，按 position 顺序依次贴到尚未配图的二级标题下。
     */
    private int insertByHeadingOrder(
            List<String> mergedLines,
            List<String> headingOrder,
            List<ImageResult> usableImages,
            Set<ImageResult> used) {
        List<ImageResult> leftovers = usableImages.stream()
                .filter(image -> !used.contains(image))
                .filter(image -> !isCover(image))
                .sorted(Comparator.comparing(image -> image.getPosition() == null ? Integer.MAX_VALUE : image.getPosition()))
                .toList();
        if (leftovers.isEmpty() || headingOrder.isEmpty()) {
            return 0;
        }

        Set<String> headingsAlreadyImaged = new HashSet<>();
        for (int i = 0; i < mergedLines.size(); i++) {
            String heading = extractLevelTwoHeading(mergedLines.get(i));
            if (heading != null && i + 2 < mergedLines.size() && isMarkdownImageLine(mergedLines.get(i + 2))) {
                headingsAlreadyImaged.add(heading);
            }
        }

        int inserted = 0;
        int leftoverIndex = 0;
        for (String heading : headingOrder) {
            if (leftoverIndex >= leftovers.size()) {
                break;
            }
            if (!headingsAlreadyImaged.add(heading)) {
                continue;
            }
            ImageResult image = leftovers.get(leftoverIndex++);
            if (!used.add(image)) {
                continue;
            }
            insertImageAfterHeading(mergedLines, heading, image);
            inserted++;
            log.info("章节标题未精确匹配，按顺序兜底插入, position={}, heading={}, sectionTitle={}",
                    image.getPosition(), heading, image.getSectionTitle());
        }
        return inserted;
    }

    private void insertImageAfterHeading(List<String> mergedLines, String heading, ImageResult image) {
        for (int i = 0; i < mergedLines.size(); i++) {
            if (!heading.equals(extractLevelTwoHeading(mergedLines.get(i)))) {
                continue;
            }
            List<String> block = new ArrayList<>(3);
            appendMarkdownImage(block, image, heading);
            mergedLines.addAll(i + 1, block);
            return;
        }
    }

    private static List<ImageResult> usableImages(List<ImageResult> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<ImageResult> result = new ArrayList<>();
        for (ImageResult image : images) {
            if (image != null && StringUtils.hasText(image.getUrl())) {
                result.add(image);
            }
        }
        return result;
    }

    private Map<String, ImageResult> indexBySectionTitle(List<ImageResult> images) {
        Map<String, ImageResult> result = new LinkedHashMap<>();
        for (ImageResult image : images) {
            if (isCover(image)) {
                continue;
            }
            String sectionTitle = normalizeSectionTitle(image.getSectionTitle());
            if (StringUtils.hasText(sectionTitle)) {
                result.putIfAbsent(sectionTitle, image);
            }
        }
        return result;
    }

    private ImageResult resolveCover(ArticleState state, List<ImageResult> usableImages) {
        if (StringUtils.hasText(state.getCoverImage())) {
            ImageResult cover = new ImageResult();
            cover.setPosition(1);
            cover.setUrl(state.getCoverImage().trim());
            cover.setDescription("封面");
            for (ImageResult image : usableImages) {
                if (isCover(image) && state.getCoverImage().trim().equals(image.getUrl())) {
                    return image;
                }
            }
            return cover;
        }
        return usableImages.stream().filter(this::isCover).findFirst().orElse(null);
    }

    private boolean isCover(ImageResult image) {
        if (image == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(image.getPosition())) {
            return true;
        }
        return !StringUtils.hasText(normalizeSectionTitle(image.getSectionTitle()));
    }

    private String extractLevelTwoHeading(String line) {
        String trimmed = line.trim();
        if (trimmed.length() <= 3
                || !trimmed.startsWith("##")
                || !Character.isWhitespace(trimmed.charAt(2))) {
            return null;
        }
        // 排除 ### 及以上
        if (trimmed.startsWith("###")) {
            return null;
        }
        return normalizeSectionTitle(trimmed);
    }

    private String normalizeSectionTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String normalized = title.trim()
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ");
        if (normalized.length() > 2
                && normalized.startsWith("##")
                && !normalized.startsWith("###")
                && Character.isWhitespace(normalized.charAt(2))) {
            normalized = normalized.substring(3).trim();
        }
        return normalized;
    }

    private static boolean isMarkdownImageLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        return trimmed.startsWith("![") && trimmed.contains("](");
    }

    private void appendMarkdownImage(List<String> lines, ImageResult image, String fallbackAlt) {
        lines.add("");
        lines.add(toMarkdownImage(image, fallbackAlt));
        lines.add("");
    }

    private String toMarkdownImage(ImageResult image, String heading) {
        String alt = StringUtils.hasText(image.getDescription())
                ? image.getDescription().trim()
                : heading;
        alt = alt.replace("[", "").replace("]", "");
        return "![" + alt + "](" + image.getUrl() + ")";
    }
}
