package com.foodmap.admin.service;

import com.foodmap.common.validation.Check;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 后台管理员 PBKDF2 密码哈希服务，负责把原始密码转换为带盐哈希字符串。
 */
@Component
public class Pbkdf2AdminPasswordHashService implements AdminPasswordHashService {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final String pepper;
    private final SecureRandom secureRandom = new SecureRandom();

    public Pbkdf2AdminPasswordHashService(@Value("${foodmap.admin.password.pepper:foodmap-admin-local-pepper}") String pepper) {
        this.pepper = Check.notBlank("pepper", pepper);
    }

    /**
     * 将后台管理员原始密码转换为可持久化 PBKDF2 哈希。
     *
     * @param rawPassword 原始密码。
     * @return 格式化后的密码哈希。
     */
    @Override
    public String hash(String rawPassword) {
        String checkedPassword = Check.notBlank("rawPassword", rawPassword);
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = hash(checkedPassword, salt);
        return ALGORITHM + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 执行 PBKDF2 哈希计算。
     *
     * @param rawPassword 原始密码。
     * @param salt 随机盐。
     * @return PBKDF2 哈希字节数组。
     */
    private byte[] hash(String rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec((rawPassword + pepper).toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to hash admin password", e);
        }
    }
}
