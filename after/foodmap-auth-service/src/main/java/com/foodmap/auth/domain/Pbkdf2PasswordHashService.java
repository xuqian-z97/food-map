package com.foodmap.auth.domain;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.validation.Check;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBKDF2 密码哈希服务，负责把明文密码转换为不可逆哈希并验证登录密码。
 *
 * <p>该类不打印密码和哈希内容。排查登录失败时应查看业务错误码和登录日志，不应输出敏感字段。</p>
 */
@Component
public class Pbkdf2PasswordHashService {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String pepper;

    /**
     * 使用本地默认 pepper 创建服务实例，生产环境后续应改为从配置中心注入。
     */
    public Pbkdf2PasswordHashService() {
        this("foodmap-local-pepper");
    }

    /**
     * 使用指定 pepper 创建服务实例，测试可传入固定值以降低排查复杂度。
     */
    public Pbkdf2PasswordHashService(String pepper) {
        this.pepper = Check.notBlank("pepper", pepper);
    }

    /**
     * 对明文密码进行 PBKDF2 哈希，返回包含算法、迭代次数、盐和哈希值的稳定格式。
     */
    public String hash(String plainPassword) {
        String password = Check.notBlank("plainPassword", plainPassword);
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        return "PBKDF2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 校验明文密码是否匹配已保存哈希，校验失败只返回 false，不泄露具体差异。
     */
    public boolean matches(String plainPassword, String storedHash) {
        String password = Check.notBlank("plainPassword", plainPassword);
        String hash = Check.notBlank("storedHash", storedHash);
        String[] parts = hash.split("\\$");
        if (parts.length != 4 || !"PBKDF2".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(password, salt, iterations);
        return constantTimeEquals(expected, actual);
    }

    private byte[] pbkdf2(String plainPassword, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec((plainPassword + pepper).toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new FoodMapException(CommonErrorCode.INTERNAL_ERROR, "密码哈希服务不可用");
        }
    }

    private boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }
        return result == 0;
    }
}
