package com.aiarticle.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpProxyBypassTest {

    private static final String[] KEYS = {
            "http.nonProxyHosts", "https.nonProxyHosts", "socksNonProxyHosts"
    };

    private final String[] previous = new String[KEYS.length];

    @AfterEach
    void restore() {
        for (int i = 0; i < KEYS.length; i++) {
            if (previous[i] == null) {
                System.clearProperty(KEYS[i]);
            } else {
                System.setProperty(KEYS[i], previous[i]);
            }
        }
    }

    @Test
    void appendsAliyunHostsWithoutDroppingExistingEntries() {
        snapshot();
        System.setProperty("https.nonProxyHosts", "localhost");

        HttpProxyBypass.applyAliyunDirect();

        String hosts = System.getProperty("https.nonProxyHosts");
        assertTrue(hosts.contains("localhost"));
        assertTrue(hosts.contains("aliyuncs.com"));
        assertTrue(hosts.contains("myqcloud.com"));
    }

    private void snapshot() {
        for (int i = 0; i < KEYS.length; i++) {
            previous[i] = System.getProperty(KEYS[i]);
        }
    }
}
