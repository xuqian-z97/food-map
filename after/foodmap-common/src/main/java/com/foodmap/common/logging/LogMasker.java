package com.foodmap.common.logging;

import java.util.Locale;

/**
 * 日志脱敏工具，统一处理手机号、邮箱、Token、对象存储 Key 和私密业务正文。
 *
 * <p>该类是日志安全底线：新增敏感字段时应优先扩展这里，而不是在业务代码中手工脱敏。</p>
 */
public final class LogMasker {

    /**
     * 通用掩码值，用于完全不可输出的敏感内容，例如密码、Token 和评论正文。
     */
    private static final String MASKED = "***";

    private LogMasker() {
    }

    /**
     * 根据字段名选择脱敏策略，适合 SafeLog 统一处理结构化日志字段。
     *
     * <p>排查日志泄露时应优先检查字段名是否命中该方法中的敏感规则。</p>
     *
     * @param fieldName 结构化日志字段名。
     * @param value 待输出的原始字段值。
     * @return 已按字段名规则脱敏后的安全文本。
     */
    public static String maskByFieldName(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        if (normalized.contains("phone") || normalized.contains("mobile")) {
            return maskPhone(text);
        }
        if (normalized.contains("email")) {
            return maskEmail(text);
        }
        if (normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("credential")) {
            return maskToken(text);
        }
        if (normalized.contains("objectkey") || normalized.equals("object_key")) {
            return maskObjectKey(text);
        }
        if (normalized.contains("commentcontent")
                || normalized.contains("recommendationcontent")
                || normalized.contains("privatecontent")
                || normalized.contains("reason")) {
            return MASKED;
        }
        return text;
    }

    /**
     * 手机号保留前三位和后四位，便于排查用户反馈，同时避免完整手机号泄露。
     *
     * @param phone 待脱敏的手机号。
     * @return 脱敏后的手机号文本。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return MASKED;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱保留首字母和域名，便于识别邮箱来源，但隐藏本地部分主体。
     *
     * @param email 待脱敏的邮箱。
     * @return 脱敏后的邮箱文本。
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return MASKED;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return MASKED;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Token 只保留认证类型，例如 Bearer，真实凭证永远不进入日志。
     *
     * @param token 待脱敏的认证令牌。
     * @return 脱敏后的令牌文本。
     */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return MASKED;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return "Bearer " + MASKED;
        }
        return MASKED;
    }

    /**
     * 对象存储 Key 只保留前两级业务目录和文件名，中间路径可能包含用户或业务隐私。
     *
     * @param objectKey 待脱敏的对象存储 Key。
     * @return 脱敏后的对象存储 Key。
     */
    public static String maskObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return MASKED;
        }
        int lastSlash = objectKey.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == objectKey.length() - 1) {
            return MASKED;
        }
        int firstSlash = objectKey.indexOf('/');
        int secondSlash = objectKey.indexOf('/', firstSlash + 1);
        if (firstSlash < 0 || secondSlash < 0) {
            return "***/" + objectKey.substring(lastSlash + 1);
        }
        return objectKey.substring(0, secondSlash) + "/***/" + objectKey.substring(lastSlash + 1);
    }
}
