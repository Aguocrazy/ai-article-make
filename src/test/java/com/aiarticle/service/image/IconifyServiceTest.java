package com.aiarticle.service.image;

import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.util.CosFileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IconifyServiceTest {

    private MockRestServiceServer server;
    private CosFileClient cosFileClient;
    private IconifyService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        IconifyProperties properties = new IconifyProperties();
        properties.setBaseUrl("https://api.iconify.design");
        properties.setLimit(32);
        cosFileClient = mock(CosFileClient.class);
        service = new IconifyService(builder, properties, cosFileClient);
    }

    @Test
    void searchImage_searchesThenUploadsSvg() {
        server.expect(once(), method(HttpMethod.GET))
                .andExpect(queryParam("query", "home"))
                .andExpect(queryParam("limit", "32"))
                .andRespond(withSuccess("""
                        {"icons":["mdi:home"],"total":1}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.iconify.design/mdi/home.svg"))
                .andRespond(withSuccess("<svg/>", MediaType.valueOf("image/svg+xml")));
        when(cosFileClient.upload(any(byte[].class), any(), eq("image/svg+xml")))
                .thenReturn(new CosFileClient.CosUploadResult("iconify/a.svg", "https://cos/a.svg", "etag"));

        assertEquals("https://cos/a.svg",
                service.searchImage(ImageSearchRequest.builder().keywords("home").build()));
        assertEquals("ICONIFY", service.getSearchMethod());
        server.verify();
    }

    @Test
    void searchImage_fallsBackToPublicSvgWhenCosUnavailable() {
        server.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"icons":["mdi:home"],"total":1}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.iconify.design/mdi/home.svg"))
                .andRespond(withSuccess("<svg/>", MediaType.valueOf("image/svg+xml")));
        when(cosFileClient.upload(any(byte[].class), any(), eq("image/svg+xml")))
                .thenThrow(new BusinessException(ErrorCode.OPERATION_ERROR, "腾讯云 COS 未配置"));

        assertEquals("https://api.iconify.design/mdi/home.svg",
                service.searchImage(ImageSearchRequest.builder().keywords("home").build()));
        server.verify();
    }

    @Test
    void searchImage_returnsNullWhenNoIcons() {
        server.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"icons":[],"total":0}
                        """, MediaType.APPLICATION_JSON));

        assertNull(service.searchImage(ImageSearchRequest.builder().keywords("zzzz").build()));
        server.verify();
    }
}
