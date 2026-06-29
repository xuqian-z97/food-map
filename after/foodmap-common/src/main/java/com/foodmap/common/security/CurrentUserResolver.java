package com.foodmap.common.security;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;

/**
 * 当前用户解析工具，负责把网关透传的可信身份请求头转换为 CurrentUser。
 *
 * <p>业务服务应通过本类读取用户身份，不直接信任客户端传入的 userId 参数。排查写接口身份问题时，
 * 优先查看网关是否写入了 FoodMapAuthHeaders 中定义的请求头。</p>
 */
public final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    /**
     * 根据网关透传的用户业务主键创建当前用户上下文。
     *
     * @param userIdHeader 网关透传的用户业务主键请求头。
     * @return 当前登录用户上下文。
     */
    public static CurrentUser fromTrustedHeaders(String userIdHeader) {
        return fromTrustedHeaders(userIdHeader, null);
    }

    /**
     * 根据网关透传的用户业务主键创建当前用户上下文，并兼容旧链路传入的账号业务主键。
     *
     * @param userIdHeader 网关透传的用户业务主键请求头。
     * @param accountIdHeader B1 旧身份模型账号业务主键请求头，可为空。
     * @return 当前登录用户上下文。
     */
    public static CurrentUser fromTrustedHeaders(String userIdHeader, String accountIdHeader) {
        Long userId = parsePositiveLong(FoodMapAuthHeaders.USER_ID, userIdHeader);
        Long accountId = parseOptionalPositiveLong(FoodMapAuthHeaders.ACCOUNT_ID, accountIdHeader);
        return new CurrentUser(userId, accountId, null);
    }

    private static Long parsePositiveLong(String headerName, String value) {
        if (value == null || value.isBlank()) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少登录用户身份");
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, headerName + "格式错误");
        }
    }

    private static Long parseOptionalPositiveLong(String headerName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parsePositiveLong(headerName, value);
    }
}
