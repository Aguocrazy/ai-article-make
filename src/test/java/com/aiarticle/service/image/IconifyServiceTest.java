package com.aiarticle.service.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IconifyServiceTest {

    private MockRestServiceServer server;
    private IconifyService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        IconifyProperties properties = new IconifyProperties();
        properties.setBaseUrl("https://api.iconify.design");
        properties.setLimit(32);
        service = new IconifyService(builder, properties);
    }

    @Test
    void searchImage_searchesThenReturnsBase64Svg() {
        server.expect(once(), method(HttpMethod.GET))
                .andExpect(queryParam("query", "home"))
                .andExpect(queryParam("limit", "32"))
                .andRespond(withSuccess("""
                        {"icons":["mdi:home"],"total":1}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.iconify.design/mdi/home.svg"))
                .andRespond(withSuccess("<svg/>", MediaType.valueOf("image/svg+xml")));

        ImagePayload payload = service.searchImage(ImageSearchRequest.builder().keywords("home").build());
        assertEquals(ImagePayloadType.BASE64, payload.getType());
        assertEquals("<svg/>", new String(Base64.getDecoder().decode(payload.getContent()), StandardCharsets.UTF_8));
        assertEquals("ICONIFY", service.getSearchMethod());
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
