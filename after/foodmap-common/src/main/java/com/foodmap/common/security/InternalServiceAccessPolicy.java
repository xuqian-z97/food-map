package com.foodmap.common.security;

import java.util.Map;
import java.util.Optional;

/**
 * 内部接口调用方访问策略，按请求路径限定允许调用该接口的服务。
 *
 * <p>默认拒绝：没有命中规则的内部路径一律返回 403，而不是回落到「只要签名有效就放行」。
 * 新增内部接口时必须同时在这里登记调用方，否则调用会被拒绝。</p>
 *
 * <p>{@code /internal/{service}/health} 健康检查不参与内部签名，网关和 verify 预检依赖它保持开放。</p>
 */
public final class InternalServiceAccessPolicy {

    /**
     * 精确匹配的路径规则，适用于只有单个固定端点的内部接口。
     */
    private static final Map<String, String> EXACT_RULES = Map.of(
            "/internal/users/provision", "foodmap-auth-service"
    );

    /**
     * 前缀匹配的路径规则，适用于同一服务下的一组内部接口。
     */
    private static final Map<String, String> PREFIX_RULES = Map.of(
            "/internal/logs/", "foodmap-admin-service"
    );

    /**
     * 内部健康检查路径前缀，格式为 {@code /internal/{service}/health}。
     */
    private static final String HEALTH_SUFFIX = "/health";

    private InternalServiceAccessPolicy() {
    }

    /**
     * 判断路径是否属于内部接口。
     *
     * @param path 请求路径，不包含查询字符串。
     * @return 以 {@code /internal/} 开头时返回 true。
     */
    public static boolean isInternalPath(String path) {
        return path != null && path.startsWith("/internal/");
    }

    /**
     * 判断路径是否为内部健康检查，健康检查不参与签名校验。
     *
     * @param path 请求路径，不包含查询字符串。
     * @return 形如 {@code /internal/{service}/health} 时返回 true。
     */
    public static boolean isHealthPath(String path) {
        if (path == null || !path.startsWith("/internal/") || !path.endsWith(HEALTH_SUFFIX)) {
            return false;
        }
        int healthSuffixStart = path.length() - HEALTH_SUFFIX.length();
        int firstSeparatorAfterPrefix = path.indexOf('/', "/internal/".length());
        return healthSuffixStart > "/internal/".length()
                && firstSeparatorAfterPrefix == healthSuffixStart;
    }

    /**
     * 查询允许调用指定内部路径的服务名。
     *
     * @param path 请求路径，不包含查询字符串。
     * @return 允许的服务名；没有登记规则时返回空。
     */
    public static Optional<String> allowedService(String path) {
        if (path == null) {
            return Optional.empty();
        }
        String exact = EXACT_RULES.get(path);
        if (exact != null) {
            return Optional.of(exact);
        }
        return PREFIX_RULES.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
