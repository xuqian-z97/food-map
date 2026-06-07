package com.foodmap.common.logging;

import java.util.Locale;

public final class LogMasker {

    private static final String MASKED = "***";

    private LogMasker() {
    }

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

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return MASKED;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

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

    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return MASKED;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return "Bearer " + MASKED;
        }
        return MASKED;
    }

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
