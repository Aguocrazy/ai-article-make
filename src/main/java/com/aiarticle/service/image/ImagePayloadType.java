package com.aiarticle.service.image;

/**
 * 生图策略返回的内容形态。上传到云端前由 {@link ImageCloudStore} 按类型处理。
 */
public enum ImagePayloadType {

    /**
     * 已可访问的图片地址，无需再编码。
     */
    URL,

    /**
     * Base64 编码的文件内容（可带 {@code data:image/...;base64,} 前缀）。
     */
    BASE64
}
