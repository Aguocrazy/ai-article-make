package com.aiarticle.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Gson 封装，供模型 JSON 响应反序列化。
 */
public final class GsonUtils {

    private static final Gson GSON = new Gson();

    private GsonUtils() {
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        return GSON.fromJson(json, typeToken.getType());
    }

    /**
     * 将对象序列化为 JSON。
     */
    public static String toJson(Object value) {
        return GSON.toJson(value);
    }
}
