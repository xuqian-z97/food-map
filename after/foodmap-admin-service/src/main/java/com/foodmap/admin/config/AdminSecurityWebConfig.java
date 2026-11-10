package com.foodmap.admin.config;

import com.foodmap.admin.security.AdminAuthInterceptor;
import com.foodmap.admin.service.AdminAuthorizationService;
import com.foodmap.admin.security.AdminTokenVerifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 后台 Web MVC 安全配置，把后台认证拦截器挂载到全部 `/api/admin/**` 受保护路径。
 *
 * <p>只有后台登录入口豁免；健康检查等内部接口不经过该拦截器，仍由内部签名过滤器保护。</p>
 */
@Configuration
public class AdminSecurityWebConfig implements WebMvcConfigurer {
    private final AdminTokenVerifier tokenVerifier;
    private final AdminAuthorizationService authorizationService;

    /**
     * 创建后台 Web MVC 安全配置。
     *
     * @param tokenVerifier 后台 Token 验签组件。
     * @param authorizationService 后台认证授权应用服务。
     */
    public AdminSecurityWebConfig(AdminTokenVerifier tokenVerifier, AdminAuthorizationService authorizationService) {
        this.tokenVerifier = tokenVerifier;
        this.authorizationService = authorizationService;
    }

    /**
     * 注册后台认证拦截器，登录入口以外的全部后台接口都必须先完成验签与授权装配。
     *
     * @param registry 拦截器注册表。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(tokenVerifier, authorizationService))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }
}
