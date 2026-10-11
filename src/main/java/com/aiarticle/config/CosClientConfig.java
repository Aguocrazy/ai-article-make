package com.aiarticle.config;

import com.aiarticle.util.CosFileClient;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 按官方文档初始化线程安全的 {@link COSClient}，进程内只保留一个实例。
 *
 * @see <a href="https://cloud.tencent.com/document/product/436/10199">COS Java SDK 快速入门</a>
 */
@Slf4j
@Configuration
public class CosClientConfig {

    @Bean(destroyMethod = "shutdown")
    public CosFileClient cosFileClient(CosProperties properties) {
        COSClient cosClient = null;
        if (properties.isConfigured()) {
            COSCredentials cred = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
            ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
            clientConfig.setHttpProtocol(HttpProtocol.https);
            cosClient = new COSClient(cred, clientConfig);
            log.info("腾讯云 COS 已初始化, bucket={}, region={}, keyPrefix={}, customDomain={}",
                    properties.getBucket(),
                    properties.getRegion(),
                    StringUtils.hasText(properties.getKeyPrefix()) ? properties.getKeyPrefix() : "(empty)",
                    StringUtils.hasText(properties.getCustomDomain()) ? properties.getCustomDomain() : "(empty)");
        } else {
            log.warn("腾讯云 COS 未配置完整（需 COS_SECRET_ID / COS_SECRET_KEY / COS_BUCKET / COS_REGION），上传将失败");
        }
        return new CosFileClient(cosClient, properties);
    }
}
