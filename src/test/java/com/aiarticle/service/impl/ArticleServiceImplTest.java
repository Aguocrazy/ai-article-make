package com.aiarticle.service.impl;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.ArticleMapper;
import com.aiarticle.model.dto.article.ArticleQueryRequest;
import com.aiarticle.model.entity.Article;
import com.aiarticle.model.vo.ArticleDetailVO;
import com.aiarticle.model.vo.ArticleVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    private ArticleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleServiceImpl(articleMapper);
    }

    @Test
    void listMine_returnsOnlyMappedRecords() {
        Article article = Article.builder()
                .id(8L)
                .taskId("t1")
                .userId(9L)
                .topic("AI")
                .status("COMPLETED")
                .build();
        when(articleMapper.paginate(eq(1L), eq(10L), any(QueryWrapper.class)))
                .thenReturn(new Page<>(List.of(article), 1, 10, 1));

        ArticleQueryRequest query = new ArticleQueryRequest();
        Page<ArticleVO> page = service.listMine(query, 9L);

        assertEquals(1, page.getRecords().size());
        assertEquals(8L, page.getRecords().getFirst().getId());
        assertEquals("AI", page.getRecords().getFirst().getTopic());
        verify(articleMapper).paginate(eq(1L), eq(10L), any(QueryWrapper.class));
    }

    @Test
    void getMine_rejectsAnotherUsersArticle() {
        when(articleMapper.selectOneById(8L)).thenReturn(
                Article.builder().id(8L).userId(10L).topic("AI").build());

        BusinessException error = assertThrows(BusinessException.class, () -> service.getMine(8L, 9L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), error.getCode());
    }

    @Test
    void getMine_returnsDetailForOwner() {
        when(articleMapper.selectOneById(8L)).thenReturn(Article.builder()
                .id(8L)
                .userId(9L)
                .topic("AI")
                .fullContent("# hi")
                .status("COMPLETED")
                .build());

        ArticleDetailVO detail = service.getMine(8L, 9L);
        assertEquals("# hi", detail.getFullContent());
        assertEquals("AI", detail.getTopic());
    }

    @Test
    void deleteMine_removesOwnedArticle() {
        when(articleMapper.selectOneById(8L)).thenReturn(
                Article.builder().id(8L).userId(9L).build());
        when(articleMapper.deleteById(8L)).thenReturn(1);

        service.deleteMine(8L, 9L);

        verify(articleMapper).deleteById(8L);
    }

    @Test
    void deleteMine_rejectsAnotherUsersArticle() {
        when(articleMapper.selectOneById(8L)).thenReturn(
                Article.builder().id(8L).userId(10L).build());

        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteMine(8L, 9L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), error.getCode());
        verify(articleMapper, never()).deleteById(any());
    }
}
