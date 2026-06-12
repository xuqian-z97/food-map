package com.foodmap.common.logging;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 链路流水号生成器，生成适合写入日志、请求头和检索条件的安全 ID。
 */
public final class TraceIdGenerator {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int DEFAULT_LENGTH = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TraceIdGenerator() {
    }

    /**
     * 生成新的日志链路 ID。
     *
     * @return 由小写字母和数字组成的链路 ID。
     */
    public static String nextId() {
        char[] chars = new char[DEFAULT_LENGTH];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ALPHABET[SECURE_RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(chars).toLowerCase(Locale.ROOT);
    }
}
