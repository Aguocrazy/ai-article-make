package com.aiarticle.service.image;

/**
 * 图片检索服务抽象。
 * <p>
 * 智能体5只依赖本接口，不关心图片来自 Pexels、Unsplash 或其他图库。
 * 后续切换图片来源时新增实现类即可。
 */
public interface ImageSearchService {

    /**
     * 按通用配图请求取一张图。
     *
     * @param request 关键词、prompt、位置等
     * @return 可直接访问的图片 URL；未搜索到时返回 {@code null}
     */
    String searchImage(ImageSearchRequest request);

    /**
     * 获取当前图片检索方式。
     * <p>
     * 用于记录 {@code ArticleState.ImageResult.method}，对应 {@code ImageMethodEnum}，
     * 例如 {@code PEXELS}、{@code NANO_BANANA} 或降级 {@code PICSUM}。
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
