package com.aiarticle.util;

import com.aiarticle.config.CosProperties;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.utils.IOUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 腾讯云 COS 文件上传 / 下载。
 * <p>
 * 简单上传适用于不超过 5GB 的对象；下载可用本地文件或字节数组。
 */
public class CosFileClient {

    private final COSClient cosClient;
    private final CosProperties properties;

    public CosFileClient(COSClient cosClient, CosProperties properties) {
        this.cosClient = cosClient;
        this.properties = properties;
    }

    /**
     * 上传本地文件，返回对象键与访问 URL。
     */
    public CosUploadResult upload(File file, String key) {
        requireClient();
        if (file == null || !file.isFile()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不存在");
        }
        String objectKey = resolveKey(key);
        try {
            PutObjectResult result = cosClient.putObject(new PutObjectRequest(properties.getBucket(), objectKey, file));
            return new CosUploadResult(objectKey, publicUrl(objectKey), result.getETag());
        } catch (CosClientException e) {
            throw wrap(e, "上传文件失败");
        }
    }

    /**
     * 上传输入流。{@code contentLength} 必须大于 0。
     */
    public CosUploadResult upload(InputStream inputStream, String key, String contentType, long contentLength) {
        requireClient();
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传内容为空");
        }
        if (contentLength <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传长度必须大于 0");
        }
        String objectKey = resolveKey(key);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        try {
            PutObjectResult result = cosClient.putObject(
                    new PutObjectRequest(properties.getBucket(), objectKey, inputStream, metadata));
            return new CosUploadResult(objectKey, publicUrl(objectKey), result.getETag());
        } catch (CosClientException e) {
            throw wrap(e, "上传文件失败");
        }
    }

    /**
     * 上传字节数组。
     */
    public CosUploadResult upload(byte[] data, String key, String contentType) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传内容为空");
        }
        return upload(new ByteArrayInputStream(data), key, contentType, data.length);
    }

    /**
     * 下载对象到本地文件。
     */
    public void downloadToFile(String key, File destFile) {
        requireClient();
        if (destFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "下载目标文件不能为空");
        }
        String objectKey = resolveKey(key);
        try {
            cosClient.getObject(new GetObjectRequest(properties.getBucket(), objectKey), destFile);
        } catch (CosClientException e) {
            throw wrap(e, "下载文件失败");
        }
    }

    /**
     * 下载对象为字节数组。调用方无需再关闭 COS 流。
     */
    public byte[] downloadBytes(String key) {
        requireClient();
        String objectKey = resolveKey(key);
        COSObject cosObject = null;
        try {
            cosObject = cosClient.getObject(new GetObjectRequest(properties.getBucket(), objectKey));
            InputStream in = cosObject.getObjectContent();
            return IOUtils.toByteArray(in);
        } catch (CosClientException e) {
            throw wrap(e, "下载文件失败");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取 COS 对象流失败");
        } finally {
            if (cosObject != null) {
                try {
                    cosObject.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    public String publicUrl(String objectKey) {
        String key = stripLeadingSlash(objectKey);
        String encoded = encodeKey(key);
        if (StringUtils.hasText(properties.getCustomDomain())) {
            String domain = properties.getCustomDomain().replaceAll("/+$", "");
            if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                domain = "https://" + domain;
            }
            return domain + "/" + encoded;
        }
        return "https://" + properties.getBucket() + ".cos." + properties.getRegion() + ".myqcloud.com/" + encoded;
    }

    public String resolveKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对象键不能为空");
        }
        String objectKey = stripLeadingSlash(key.trim());
        String prefix = stripLeadingSlash(properties.getKeyPrefix() == null ? "" : properties.getKeyPrefix().trim());
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (!StringUtils.hasText(prefix) || objectKey.startsWith(prefix + "/") || objectKey.equals(prefix)) {
            return objectKey;
        }
        return prefix + "/" + objectKey;
    }

    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    private void requireClient() {
        if (cosClient == null || !properties.isConfigured()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "腾讯云 COS 未配置，请设置 COS_SECRET_ID、COS_SECRET_KEY、COS_BUCKET");
        }
    }

    private static String stripLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static String encodeKey(String key) {
        String[] parts = key.split("/", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return builder.toString();
    }

    private static BusinessException wrap(RuntimeException e, String message) {
        String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return new BusinessException(ErrorCode.OPERATION_ERROR, message + "：" + detail);
    }

    public record CosUploadResult(String key, String url, String etag) {
    }
}
