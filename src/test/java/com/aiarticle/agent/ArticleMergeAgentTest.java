package com.aiarticle.agent;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.model.state.ArticleState;
import com.aiarticle.model.state.ArticleState.ImageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArticleMergeAgentTest {

    private final ArticleMergeAgent articleMergeAgent = new ArticleMergeAgent();

    @Test
    void merge_insertsMatchingImageAfterLevelTwoHeading() {
        ArticleState state = new ArticleState();
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
        assertEquals("""
                开场文字
                
                ## 第一章
                
                ![第一章配图](https://img/chapter-1.jpg)
                
                第一章正文
                
                ## 第二章
                第二章正文
                """, state.getFullContent());
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
