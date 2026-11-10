package com.foodmap.admin.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodmap.admin.application.port.LogApiAccessLogClient;
import com.foodmap.common.security.InternalServiceSigner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * 管理后台日志平台客户端配置，注册 HTTP 客户端和日志查询适配器。
 */
@Configuration
@EnableConfigurationProperties(AdminLogClientProperties.class)
public class AdminLogClientConfiguration {

    /**
     * 创建日志平台接口访问摘要客户端。
     *
     * <p>客户端需要内部服务签名器为查询请求补充身份头；管理后台必须显式开启
     * {@code foodmap.internal.enabled} 并提供共享密钥，否则启动阶段即失败。</p>
     *
     * @param objectMapper JSON 解析组件。
     * @param properties 日志平台客户端配置。
     * @param signer 内部服务请求签名器。
     * @return 接口访问摘要客户端端口实现。
     */
    @Bean
    public LogApiAccessLogClient logApiAccessLogClient(
            ObjectMapper objectMapper,
            AdminLogClientProperties properties,
            InternalServiceSigner signer
    ) {
        return new HttpLogApiAccessLogClient(HttpClient.newHttpClient(), objectMapper, properties, signer);
    }
}
