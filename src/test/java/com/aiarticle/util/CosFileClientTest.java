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
