package com.aiarticle.service.image;

/**
 * 图片检索服务抽象。
 * <p>
 * 智能体5只依赖本接口，不关心图片来自 Pexels、Unsplash 或其他图库。
 * 后续切换图片来源时新增实现类即可。
 */
public interface ImageSearchService {

    /**
     * 根据关键词搜索一张图片。
     *
     * @param keywords 搜索关键词，通常为智能体4生成的英文关键词
     * @return 可直接访问的图片 URL；未搜索到时返回 {@code null}
     */
    String searchImage(String keywords);

    /**
     * 获取当前图片检索方式。
     * <p>
     * 用于记录 {@code ArticleState.ImageResult.method}，例如 {@code PEXELS}、
     * {@code UNSPLASH} 或 {@code FALLBACK}。
     *
     * @return 图片检索方式标识
     */
    String getSearchMethod();

    /**
     * 获取降级图片 URL。
     * <p>
     * 当远程图库不可用、请求失败或没有匹配结果时使用。
     *
     * @return 可直接访问的降级图片 URL
     */
    String getFallbackImageUrl(int position);
}
