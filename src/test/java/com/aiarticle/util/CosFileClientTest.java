package com.aiarticle.util;

import com.aiarticle.config.CosProperties;
import com.aiarticle.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosFileClientTest {

    @Test
    void resolveKey_joinsPrefix() {
        CosFileClient client = new CosFileClient(null, properties("articles"));
        assertEquals("articles/a.png", client.resolveKey("a.png"));
        assertEquals("articles/a.png", client.resolveKey("/a.png"));
        assertEquals("articles/a.png", client.resolveKey("articles/a.png"));
    }

    @Test
    void publicUrl_usesDefaultDomain() {
        CosFileClient client = new CosFileClient(null, properties(""));
        assertEquals(
                "https://demo-1250000000.cos.ap-guangzhou.myqcloud.com/folder/a.png",
                client.publicUrl("folder/a.png"));
    }

    @Test
    void publicUrl_usesCustomDomain() {
        CosProperties props = properties("");
        props.setCustomDomain("cdn.example.com");
        CosFileClient client = new CosFileClient(null, props);
        assertEquals("https://cdn.example.com/a.png", client.publicUrl("a.png"));
    }

    @Test
    void upload_throwsWhenNotConfigured() {
        CosFileClient client = new CosFileClient(null, new CosProperties());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.upload(new byte[]{1}, "a.bin", "application/octet-stream"));
        assertTrue(ex.getMessage().contains("未配置"));
        assertTrue(client.describeConfig().contains("configured=false"));
    }

    @Test
    void upload_throwsWhenBucketNameInvalid() {
        CosProperties props = properties("");
        props.setBucket("short");
        CosFileClient client = new CosFileClient(
                new com.qcloud.cos.COSClient(
                        new com.qcloud.cos.auth.BasicCOSCredentials("id", "key"),
                        new com.qcloud.cos.ClientConfig(new com.qcloud.cos.region.Region("ap-guangzhou"))),
                props);
        try {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> client.upload(new byte[]{1}, "a.bin", "application/octet-stream"));
            assertTrue(ex.getMessage().contains("COS_BUCKET"));
            assertTrue(ex.getMessage().contains("bucket=short"));
        } finally {
            client.shutdown();
        }
    }

    @Test
    void describeConfig_includesBucketRegion() {
        CosFileClient client = new CosFileClient(null, properties("articles"));
        String summary = client.describeConfig();
        assertTrue(summary.contains("bucket=demo-1250000000"));
        assertTrue(summary.contains("region=ap-guangzhou"));
        assertTrue(summary.contains("keyPrefix=articles"));
        assertTrue(summary.contains("clientReady=false"));
    }

    @Test
    void resolveKey_rejectsBlank() {
        CosFileClient client = new CosFileClient(null, properties(""));
        assertThrows(BusinessException.class, () -> client.resolveKey("  "));
    }

    private static CosProperties properties(String prefix) {
        CosProperties props = new CosProperties();
        props.setSecretId("id");
        props.setSecretKey("key");
        props.setRegion("ap-guangzhou");
        props.setBucket("demo-1250000000");
        props.setKeyPrefix(prefix);
        return props;
    }
}
