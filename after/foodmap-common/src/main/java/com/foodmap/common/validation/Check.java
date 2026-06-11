package com.foodmap.common.validation;

/**
 * FoodMap 项目级基础校验工具，集中处理 record、Command、事件信封和中间件命令中的轻量参数校验。
 *
 * <p>该类只承载跨模块复用的基础校验，不承载业务规则；业务状态、权限和可见范围仍应放在对应领域服务中。</p>
 */
public final class Check {

    private Check() {
    }

    /**
     * 校验文本不为空并返回 trim 后的值，适合对象存储 Key、事件类型、缓存片段等基础字段。
     *
     * @param fieldName 字段名，用于异常信息和排查定位。
     * @param value 待校验文本。
     * @return 去除首尾空白后的有效文本。
     */
    public static String notBlank(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    /**
     * 校验文本不包含冒号，主要用于 Redis Key 分段，避免调用方污染统一 Key 层级。
     *
     * @param fieldName 字段名，用于异常信息和排查定位。
     * @param value 待校验文本。
     * @return 去除首尾空白且不包含冒号的有效文本。
     */
    public static String noColon(String fieldName, String value) {
        String checked = notBlank(fieldName, value);
        if (checked.contains(":")) {
            throw new IllegalArgumentException(fieldName + " must not contain ':'");
        }
        return checked;
    }

    /**
     * 校验 long 数值为正数，适合文件大小等原始数值字段。
     *
     * @param fieldName 字段名，用于异常信息和排查定位。
     * @param value 待校验数值。
     * @return 原始正数值。
     */
    public static long positive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 校验 Long 数值不为空且为正数，适合用户业务主键、账号业务主键等可空包装类型字段。
     *
     * @param fieldName 字段名，用于异常信息和排查定位。
     * @param value 待校验包装数值。
     * @return 原始非空正数值。
     */
    public static Long positive(String fieldName, Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
