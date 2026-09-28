package com.aiarticle.config;

import com.aiarticle.util.CosFileClient;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按官方文档初始化线程安全的 {@link COSClient}，进程内只保留一个实例。
 *
 * @see <a href="https://cloud.tencent.com/document/product/436/10199">COS Java SDK 快速入门</a>
 */
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
        }
        return new CosFileClient(cosClient, properties);
    }
}
