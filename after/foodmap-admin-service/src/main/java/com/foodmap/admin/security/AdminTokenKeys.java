package com.foodmap.admin.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 后台 Token RSA 密钥解析工具，把 PEM 密钥文件转换为 JDK RSA Key。
 *
 * <p>只支持后台密钥约定的两种 PEM 形态：公钥为 X.509 SubjectPublicKeyInfo（`BEGIN PUBLIC KEY`），
 * 私钥为 PKCS#8（`BEGIN PRIVATE KEY`）。解析失败时抛出带配置键信息的异常，阻止服务带错误密钥启动；
 * 使用 JDK KeyFactory 而不是 BouncyCastle，避免引入额外加密Provider依赖。</p>
 */
public final class AdminTokenKeys {
    /**
     * 后台 Token 公钥位置配置键，签发与验签两端共用。
     */
    public static final String PUBLIC_KEY_LOCATION_KEY = "foodmap.admin.token.public-key-location";
    /**
     * 后台 Token 私钥位置配置键，只有管理后台服务允许配置。
     */
    public static final String PRIVATE_KEY_LOCATION_KEY = "foodmap.admin.token.private-key-location";

    private AdminTokenKeys() {
    }

    /**
     * 解析 X.509 PEM 公钥为 RSA 公钥。
     *
     * @param location 密钥资源位置描述，用于异常信息定位。
     * @param pemContent PEM 文件内容。
     * @return RSA 公钥。
     * @throws IllegalStateException 内容缺失、格式不正确或不是 RSA 密钥时抛出，阻止服务启动。
     */
    public static RSAPublicKey parseRsaPublicKey(String location, String pemContent) {
        byte[] der = decodePemBody(location, pemContent, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("后台 Token 公钥无法解析为 RSA 密钥：" + location, ex);
        }
    }

    /**
     * 解析 PKCS#8 PEM 私钥为 RSA 私钥。
     *
     * @param location 密钥资源位置描述，用于异常信息定位。
     * @param pemContent PEM 文件内容。
     * @return RSA 私钥。
     * @throws IllegalStateException 内容缺失、格式不正确或不是 RSA 密钥时抛出，阻止服务启动。
     */
    public static RSAPrivateKey parseRsaPrivateKey(String location, String pemContent) {
        byte[] der = decodePemBody(location, pemContent, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("后台 Token 私钥无法解析为 RSA 密钥：" + location, ex);
        }
    }

    /**
     * 提取 PEM 中指定类型的单一密钥块并做 base64 解码。
     *
     * @param location 密钥资源位置描述。
     * @param pemContent PEM 文件内容。
     * @param type PEM 块类型，如 PUBLIC KEY、PRIVATE KEY。
     * @return DER 字节。
     * @throws IllegalStateException 内容为空、块缺失或 base64 不合法时抛出。
     */
    private static byte[] decodePemBody(String location, String pemContent, String type) {
        if (pemContent == null || pemContent.isBlank()) {
            throw new IllegalStateException("后台 Token 密钥文件为空：" + location);
        }
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int beginIndex = pemContent.indexOf(begin);
        int endIndex = pemContent.indexOf(end);
        if (beginIndex < 0 || endIndex < 0 || endIndex < beginIndex) {
            throw new IllegalStateException("后台 Token 密钥 PEM 缺少 " + type + " 块：" + location);
        }
        String body = pemContent.substring(beginIndex + begin.length(), endIndex).replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("后台 Token 密钥 base64 无法解析：" + location, ex);
        }
    }
}
