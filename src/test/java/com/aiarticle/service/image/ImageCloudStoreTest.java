package com.aiarticle.service.image;

import com.aiarticle.util.CosFileClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageCloudStoreTest {

    @Mock
    private CosFileClient cosFileClient;

    @Test
    void toPublicUrl_uploadsBase64() {
        byte[] svg = "<svg/>".getBytes(StandardCharsets.UTF_8);
        ImagePayload payload = ImagePayload.base64(svg, "image/svg+xml", "svg", "mermaid");
        when(cosFileClient.upload(any(byte[].class), any(), eq("image/svg+xml")))
                .thenReturn(new CosFileClient.CosUploadResult("mermaid/a.svg", "https://cos/a.svg", "etag"));

        ImageCloudStore store = new ImageCloudStore(cosFileClient, RestClient.builder());
        assertEquals("https://cos/a.svg", store.toPublicUrl(payload));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cosFileClient).upload(dataCaptor.capture(), keyCaptor.capture(), eq("image/svg+xml"));
        assertArrayEquals(svg, dataCaptor.getValue());
        assertTrue(keyCaptor.getValue().startsWith("mermaid/"));
        assertTrue(keyCaptor.getValue().endsWith(".svg"));
    }

    @Test
    void toPublicUrl_keepsUrlWhenDownloadFails() {
        ImageCloudStore store = new ImageCloudStore(cosFileClient, RestClient.builder());
        assertEquals("http://127.0.0.1:1/missing.jpg",
                store.toPublicUrl(ImagePayload.url("http://127.0.0.1:1/missing.jpg")));
    }

    @Test
    void decodeBase64_stripsDataUrlPrefix() {
        String encoded = Base64.getEncoder().encodeToString("hi".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8),
                ImageCloudStore.decodeBase64("data:image/png;base64," + encoded));
    }

    @Test
    void toPublicUrl_returnsNullWhenEmpty() {
        ImageCloudStore store = new ImageCloudStore(cosFileClient, RestClient.builder());
        assertNull(store.toPublicUrl(null));
        assertNull(store.toPublicUrl(new ImagePayload()));
    }
}
