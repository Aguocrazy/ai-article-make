package com.aiarticle.agent;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleMergeAgentTest {

    private final ArticleMergeAgent articleMergeAgent = new ArticleMergeAgent();

    @Test
    void merge_insertsCoverAndMatchingSectionImage() {
        ArticleState state = new ArticleState();
        state.setCoverImage("https://img/cover.jpg");
        state.setContent("""
                开场文字
                
                ## 第一章
                第一章正文
                
                ## 第二章
                第二章正文
                """);
        state.setImages(List.of(
                image(1, "", "https://img/cover.jpg", "封面"),
                image(2, "第一章", "https://img/chapter-1.jpg", "第一章配图"),
                image(3, "不存在的章节", "https://img/unmatched.jpg", "未匹配配图")
        ));

        ArticleState result = articleMergeAgent.merge(state);

        assertSame(state, result);
        String full = state.getFullContent();
        assertTrue(full.startsWith("\n![封面](https://img/cover.jpg)\n"));
        assertTrue(full.contains("## 第一章\n\n![第一章配图](https://img/chapter-1.jpg)\n"));
        assertTrue(full.contains("## 第二章\n\n![未匹配配图](https://img/unmatched.jpg)\n"));
    }

    @Test
    void merge_acceptsSectionTitleWithMarkdownPrefix() {
        ArticleState state = new ArticleState();
        state.setContent("## 第一章\n正文");
        state.setImages(List.of(image(2, "## 第一章", "https://img/1.jpg", "")));

        articleMergeAgent.merge(state);

        assertEquals("## 第一章\n\n![第一章](https://img/1.jpg)\n\n正文", state.getFullContent());
    }

    @Test
    void merge_usesOnlyFirstImageForSameSection() {
        ArticleState state = new ArticleState();
        state.setContent("## 第一章\n正文");
        state.setImages(List.of(
                image(2, "第一章", "https://img/1.jpg", "图1"),
                image(3, "第一章", "https://img/2.jpg", "图2")
        ));

        articleMergeAgent.merge(state);

        assertEquals("## 第一章\n\n![图1](https://img/1.jpg)\n\n正文", state.getFullContent());
    }

    @Test
    void merge_fallsBackByHeadingOrderWhenTitlesMismatch() {
        ArticleState state = new ArticleState();
        state.setContent("""
                ## 真实标题A
                a
                
                ## 真实标题B
                b
                """);
        state.setImages(List.of(
                image(2, "模型写错的标题", "https://img/a.jpg", "A图"),
                image(3, "另一个错标题", "https://img/b.jpg", "B图")
        ));

        articleMergeAgent.merge(state);

        String full = state.getFullContent();
        assertTrue(full.contains("## 真实标题A\n\n![A图](https://img/a.jpg)\n"));
        assertTrue(full.contains("## 真实标题B\n\n![B图](https://img/b.jpg)\n"));
    }

    @Test
    void merge_rejectsBlankContent() {
        ArticleState state = new ArticleState();
        state.setContent(" ");
        assertThrows(BusinessException.class, () -> articleMergeAgent.merge(state));
    }

    private ImageResult image(int position, String sectionTitle, String url, String description) {
        ImageResult image = new ImageResult();
        image.setPosition(position);
        image.setSectionTitle(sectionTitle);
        image.setUrl(url);
        image.setDescription(description);
        return image;
    }
}
