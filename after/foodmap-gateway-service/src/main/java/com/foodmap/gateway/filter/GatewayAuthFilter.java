package com.foodmap.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.security.FoodMapAuthHeaders;
import com.foodmap.common.security.HmacTokenCodec;
import com.foodmap.common.security.NoopAccessTokenDenylistClient;
import com.foodmap.common.security.TokenClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 网关认证过滤器，负责校验外部 Access Token 并向下游服务透传可信用户身份。
 *
 * <p>过滤器会覆盖客户端传入的 FoodMap 内部身份请求头，避免外部伪造用户身份。排查下游用户身份异常时，
 * 应先确认网关是否执行了本过滤器以及 Token 中的 userId 是否正确。</p>
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int AUTH_FILTER_ORDER = -100;

    /**
     * Token 密钥最小长度，与认证服务 TokenSecretProperties 保持一致，避免网关接受认证服务拒绝的弱密钥。
     */
    private static final int MIN_TOKEN_SECRET_LENGTH = 32;

    /**
     * Token 密钥配置键，必须与认证服务使用同一个键和同一个环境变量。
     */
    private static final String TOKEN_SECRET_KEY = "foodmap.security.token-secret";

    /**
     * Token 密钥环境变量名，用于启动失败时给出可执行的修复提示。
     */
    private static final String TOKEN_SECRET_ENV = "FOODMAP_TOKEN_SECRET";
    private static final List<String> PUBLIC_API_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final HmacTokenCodec tokenCodec;
    private final AccessTokenDenylistClient accessTokenDenylistClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用配置中的 Token 密钥创建网关认证过滤器，测试未注入拒绝名单时使用空操作客户端。
     *
     * <p>该构造器保留给单元测试直接实例化使用，Spring 容器注入使用带拒绝名单客户端的构造器。</p>
     *
     * @param tokenSecret 网关用于校验 Access Token 的 HMAC 密钥。
     */
    public GatewayAuthFilter(String tokenSecret) {
        this(tokenSecret, new NoopAccessTokenDenylistClient());
    }

    /**
     * 使用配置中的 Token 密钥和拒绝名单客户端创建网关认证过滤器。
     *
     * <p>密钥只从 {@code foodmap.security.token-secret} 注入，不提供任何默认值：缺失或长度不足时
     * 在 Bean 创建阶段直接抛出异常阻止网关启动，避免与认证服务签验不一致。</p>
     *
     * @param tokenSecret 网关用于校验 Access Token 的 HMAC 密钥，生产环境由 {@code FOODMAP_TOKEN_SECRET} 注入。
     * @param accessTokenDenylistClient Access Token 拒绝名单客户端。
     */
    @Autowired
    public GatewayAuthFilter(
            @Value("${foodmap.security.token-secret}") String tokenSecret,
            AccessTokenDenylistClient accessTokenDenylistClient
    ) {
        this.tokenCodec = new HmacTokenCodec(requireUsableSecret(tokenSecret));
        this.accessTokenDenylistClient = accessTokenDenylistClient;
    }

    /**
     * 校验 Token 密钥可用性，缺失或过短时直接阻止网关启动。
     *
     * <p>异常信息不输出密钥内容，避免密钥进入启动日志。</p>
     *
     * @param tokenSecret 配置解析得到的 Token 密钥，可能为空或过短。
     * @return 通过校验的 Token 密钥原文。
     */
    private static String requireUsableSecret(String tokenSecret) {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException(TOKEN_SECRET_KEY + " 未配置：必须通过环境变量 " + TOKEN_SECRET_ENV
                    + " 注入至少 " + MIN_TOKEN_SECRET_LENGTH + " 字符的 HMAC 签名密钥");
        }
        if (tokenSecret.length() < MIN_TOKEN_SECRET_LENGTH) {
            throw new IllegalStateException(TOKEN_SECRET_KEY + " 长度不足：当前 " + tokenSecret.length()
                    + " 字符，至少需要 " + MIN_TOKEN_SECRET_LENGTH + " 字符；请检查环境变量 " + TOKEN_SECRET_ENV);
        }
        return tokenSecret;
    }

    /**
     * 过滤外部 API 请求，公开认证接口放行，其它 `/api/**` 请求必须携带有效 Access Token。
     *
     * @param exchange 当前网关请求上下文。
     * @param chain 后续网关过滤器链。
     * @return 当前请求的响应完成信号。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        ServerWebExchange sanitizedExchange = stripTrustedIdentityHeaders(exchange);
        if (isInternalPath(path) && !isInternalHealthPath(path)) {
            return forbidden(sanitizedExchange, "内部接口不允许通过外部网关访问");
        }
        if (!path.startsWith("/api/") || isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        String token = extractBearerToken(sanitizedExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return unauthorized(sanitizedExchange, "缺少Access Token");
        }

        try {
            TokenClaims claims = tokenCodec.parseAccessToken(token);
            if (claims.isExpiredAt(OffsetDateTime.now())) {
                return unauthorized(sanitizedExchange, "Access Token已过期");
            }
            if (accessTokenDenylistClient.contains(tokenCodec.tokenHash(token))) {
                return unauthorized(sanitizedExchange, "Access Token已失效");
            }
            ServerHttpRequest requestWithIdentity = sanitizedExchange.getRequest().mutate()
                    .header(FoodMapAuthHeaders.USER_ID, String.valueOf(claims.userId()))
                    .build();
            return chain.filter(sanitizedExchange.mutate().request(requestWithIdentity).build());
        } catch (FoodMapException ex) {
            return unauthorized(sanitizedExchange, ex.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return AUTH_FILTER_ORDER;
    }

    @SuppressWarnings("deprecation")
    private ServerWebExchange stripTrustedIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(FoodMapAuthHeaders.ACCOUNT_ID);
                    headers.remove(FoodMapAuthHeaders.USER_ID);
                })
                .build();
        return exchange.mutate().request(sanitizedRequest).build();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_API_PATHS.stream().anyMatch(path::equals);
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/internal/");
    }

    private boolean isInternalHealthPath(String path) {
        return path.startsWith("/internal/") && path.endsWith("/health");
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? null : token;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        try {
            byte[] body = objectMapper.writeValueAsString(ApiResponse.fail(
                            CommonErrorCode.UNAUTHORIZED.status(),
                            CommonErrorCode.UNAUTHORIZED.code(),
                            message))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        try {
            byte[] body = objectMapper.writeValueAsString(ApiResponse.fail(
                            CommonErrorCode.FORBIDDEN.status(),
                            CommonErrorCode.FORBIDDEN.code(),
                            message))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
    }
}
