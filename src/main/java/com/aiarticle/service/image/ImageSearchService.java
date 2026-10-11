package com.aiarticle.service.image;

/**
 * 图片检索 / 生图策略抽象。
 * <p>
 * 智能体5只依赖本接口。策略返回 {@link ImagePayload}（URL 或 Base64），
 * 再由 {@link ImageCloudStore} 上传到云端得到可访问地址。
 */
public interface ImageSearchService {

    /**
     * 按通用配图请求生成或检索一张图。
     *
     * @param request 关键词、prompt、位置等
     * @return URL 或 Base64 产物；未生成时返回 {@code null}
     */
    ImagePayload searchImage(ImageSearchRequest request);

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
