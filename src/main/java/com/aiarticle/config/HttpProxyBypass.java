package com.aiarticle.config;

/**
 * 避免本机系统代理（如 Clash 7897）劫持阿里云 HTTPS。
 * <p>
 * macOS 开启系统代理后，JDK 会自动设置 {@code https.proxyHost}。
 * 经该代理访问 DashScope 时，对端会在握手阶段断开（Remote host terminated the handshake）；
 * 直连同一域名则正常。启动时把阿里云域名加入 nonProxyHosts。
 */
public final class HttpProxyBypass {

    private static final String DIRECT_HOSTS =
            "*.aliyuncs.com|aliyuncs.com|dashscope.aliyuncs.com|*.myqcloud.com|myqcloud.com";

    private HttpProxyBypass() {
    }

    public static void applyAliyunDirect() {
        appendNonProxy("http.nonProxyHosts");
        appendNonProxy("https.nonProxyHosts");
        appendNonProxy("socksNonProxyHosts");
    }

    private static void appendNonProxy(String key) {
        String current = System.getProperty(key, "");
        if (current.contains("aliyuncs.com") && current.contains("myqcloud.com")) {
            return;
        }
        String extra = DIRECT_HOSTS;
        if (current.contains("aliyuncs.com")) {
            extra = "*.myqcloud.com|myqcloud.com";
        } else if (current.contains("myqcloud.com")) {
            extra = "*.aliyuncs.com|aliyuncs.com|dashscope.aliyuncs.com";
        }
        System.setProperty(key, current.isBlank() ? DIRECT_HOSTS : current + "|" + extra);
    }
}
