package com.foodmap.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.common.api.ApiResponse;
import com.foodmap.common.exception.FoodMapException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 内部服务签名校验过滤器，只拦截非健康检查的 {@code /internal/**} 请求。
 *
 * <p>请求体需要参与签名，因此过滤器先完整读取请求体，再交给下游；读取后通过
 * {@link CachedBodyRequest} 重放，保证 Controller 仍能正常反序列化。校验失败直接写统一
 * {@link ApiResponse} 并终止链路，不进入业务代码。</p>
 */
public class InternalServiceSignatureFilter extends OncePerRequestFilter {

    /**
     * 过滤器执行顺序，位于日志与链路过滤器之后、业务过滤器之前。
     */
    public static final int FILTER_ORDER = 30;

    private final InternalServiceRequestVerifier verifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建内部服务签名校验过滤器。
     *
     * @param verifier 内部服务请求校验器。
     */
    public InternalServiceSignatureFilter(InternalServiceRequestVerifier verifier) {
        if (verifier == null) {
            throw new IllegalArgumentException("verifier must not be null");
        }
        this.verifier = verifier;
    }

    /**
     * 对内部接口执行签名校验，其余请求直接放行。
     *
     * @param request HTTP 请求。
     * @param response HTTP 响应。
     * @param filterChain 过滤器链。
     * @throws ServletException Servlet 处理异常。
     * @throws IOException IO 异常。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = pathWithinApplication(request);
        if (!InternalServiceAccessPolicy.isInternalPath(path)
                || InternalServiceAccessPolicy.isHealthPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        try {
            verifier.verify(
                    request.getHeader(InternalServiceHeaders.SERVICE),
                    request.getHeader(InternalServiceHeaders.TIMESTAMP),
                    request.getHeader(InternalServiceHeaders.NONCE),
                    request.getHeader(InternalServiceHeaders.SIGNATURE),
                    request.getMethod(),
                    path,
                    body
            );
        } catch (FoodMapException ex) {
            writeFailure(response, ex);
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /**
     * 提取去掉 contextPath 的请求路径，签名只覆盖应用内路径。
     *
     * @param request HTTP 请求。
     * @return 应用内请求路径。
     */
    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * 写出统一失败响应，不暴露签名、密钥或内部堆栈。
     *
     * @param response HTTP 响应。
     * @param ex 业务异常。
     * @throws IOException 写响应失败。
     */
    private void writeFailure(HttpServletResponse response, FoodMapException ex) throws IOException {
        byte[] body = objectMapper.writeValueAsString(ApiResponse.fail(
                ex.status(), ex.errorCode().code(), ex.getMessage())).getBytes(StandardCharsets.UTF_8);
        response.setStatus(ex.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(body);
    }

    /**
     * 可重放请求体的请求包装器，保证过滤器读取后 Controller 仍能读取。
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        /**
         * 创建可重放请求体的包装器。
         *
         * @param request 原始请求。
         * @param body 已读取的请求体。
         */
        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        /**
         * 返回每次从头读取的请求体输入流。
         *
         * @return 请求体输入流。
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("异步读取不受支持");
                }

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return input.read(buffer, offset, length);
                }
            };
        }

        /**
         * 返回按请求编码读取的字符流。
         *
         * @return 请求体字符流。
         * @throws IOException 读取失败。
         */
        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
