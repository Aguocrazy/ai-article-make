package com.aiarticle.service.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PexelsImageSearchServiceTest {

    private MockRestServiceServer server;
    private PexelsImageSearchService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        PexelsProperties properties = new PexelsProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.pexels.com");
        properties.setPerPage(1);
        properties.setFallbackUrls(List.of("https://fallback/1.jpg", "https://fallback/2.jpg"));
        service = new PexelsImageSearchService(builder, properties);
    }

    @Test
    void searchImage_callsPexelsAndReturnsLandscapeUrl() {
        server.expect(once(), method(HttpMethod.GET))
                .andExpect(header("Authorization", "test-key"))
                .andExpect(queryParam("query", "AI%20office"))
                .andExpect(queryParam("orientation", "landscape"))
                .andExpect(queryParam("per_page", "1"))
                .andRespond(withSuccess("""
                        {
                          "photos": [
                            {
                              "src": {
                                "original": "https://images/original.jpg",
                                "landscape": "https://images/landscape.jpg"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertEquals("https://images/landscape.jpg", service.searchImage("AI office"));
        assertEquals("PEXELS", service.getSearchMethod());
        server.verify();
    }

    @Test
    void searchImage_returnsNullWhenPexelsFails() {
        server.expect(once(), method(HttpMethod.GET))
                .andRespond(withServerError());

        assertNull(service.searchImage("broken"));
        server.verify();
    }

    @Test
    void getFallbackImageUrl_cyclesConfiguredUrlsByPosition() {
        assertEquals("https://fallback/1.jpg", service.getFallbackImageUrl(1));
        assertEquals("https://fallback/2.jpg", service.getFallbackImageUrl(2));
        assertEquals("https://fallback/1.jpg", service.getFallbackImageUrl(3));
    }
}
