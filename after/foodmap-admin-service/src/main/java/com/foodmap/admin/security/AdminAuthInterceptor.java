package com.foodmap.admin.security;

import com.foodmap.admin.service.AdminAuthorizationService;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台受保护接口认证拦截器，独立验签后台 Token 并装配已认证管理员请求上下文。
 *
 * <p>直连后台服务的请求与经网关转发的请求走同一套校验；网关剥头与显式路由只是第一层防线。</p>
 */
public class AdminAuthInterceptor implements HandlerInterceptor {
    /**
     * 已认证后台管理员请求属性名，Controller 通过该属性读取服务端装配的上下文。
     */
    public static final String ATTR_AUTHENTICATED_ADMIN = "authenticatedAdmin";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminTokenVerifier tokenVerifier;
    private final AdminAuthorizationService authorizationService;

    /**
     * 创建后台认证拦截器。
     *
     * @param tokenVerifier 后台 Token 验签组件。
     * @param authorizationService 后台认证授权应用服务。
     */
    public AdminAuthInterceptor(AdminTokenVerifier tokenVerifier, AdminAuthorizationService authorizationService) {
        this.tokenVerifier = tokenVerifier;
        this.authorizationService = authorizationService;
    }

    /**
     * 验签并装配后台管理员请求上下文，失败时抛出 401/503 业务异常交给统一异常处理。
     *
     * @param request 当前请求。
     * @param response 当前响应。
     * @param handler 目标处理器。
     * @return 校验通过返回 true 继续执行。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        VerifiedAdminToken verified = tokenVerifier.verify(extractBearerToken(request));
        AuthenticatedAdmin admin = authorizationService.authenticate(verified);
        request.setAttribute(ATTR_AUTHENTICATED_ADMIN, admin);
        return true;
    }

    /**
     * 提取唯一 Bearer 凭证，缺失、非 Bearer、多值凭证或空值都按缺少凭证处理。
     *
     * @param request 当前请求。
     * @return Bearer 凭证文本。
     * @throws FoodMapException 凭证缺失或形态不合规时抛出 401。
     */
    private String extractBearerToken(HttpServletRequest request) {
        var headers = request.getHeaders(HttpHeaders.AUTHORIZATION);
        var values = new java.util.ArrayList<String>();
        while (headers.hasMoreElements()) {
            values.add(headers.nextElement());
        }
        if (values.size() != 1) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少后台访问令牌");
        }
        String authorization = values.get(0);
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少后台访问令牌");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少后台访问令牌");
        }
        return token;
    }
}
