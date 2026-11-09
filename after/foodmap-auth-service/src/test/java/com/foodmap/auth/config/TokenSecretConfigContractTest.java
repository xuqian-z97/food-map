package com.foodmap.auth.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证服务 Token 密钥配置契约测试，锁定 auth 与 gateway 共用的配置键、环境变量和 local 开发默认值。
 *
 * <p>网关侧的 {@code TokenSecretConfigContractTest} 会用同一组常量断言 gateway 配置，
 * 只有当两个服务的配置键、环境变量名和开发默认值完全一致时，本地登录链路才不会被签验不一致打断。</p>
 */
class TokenSecretConfigContractTest {

    private static final String TOKEN_SECRET_PROPERTY = "token-secret";

    private static final String PEPPER_PROPERTY = "pepper";

    private static final String EXPECTED_DEV_DEFAULT_SECRET = "foodmap-dev-only-token-secret-please-change";

    private static final String EXPECTED_DEV_DEFAULT_PEPPER = "foodmap-dev-only-auth-pepper";

    @Test
    void localAndOrbstackProfilesShouldReuseSharedDevDefaults() {
        for (String resource : new String[]{"application-local.yml", "application-orbstack.yml"}) {
            assertThat(tokenSecret(resource))
                    .as("%s 中的 %s", resource, TOKEN_SECRET_PROPERTY)
                    .isEqualTo("${FOODMAP_TOKEN_SECRET:" + EXPECTED_DEV_DEFAULT_SECRET + "}");
            assertThat(authPepper(resource))
                    .as("%s 中的 pepper", resource)
                    .isEqualTo("${AUTH_PEPPER:" + EXPECTED_DEV_DEFAULT_PEPPER + "}");
        }
    }

    @Test
    void prodProfileShouldNotProvideAnyDefaultTokenSecretOrPepper() {
        assertThat(tokenSecret("application-prod.yml"))
                .isEqualTo("${FOODMAP_TOKEN_SECRET}");
        assertThat(authPepper("application-prod.yml"))
                .isEqualTo("${AUTH_PEPPER}");
    }

    /**
     * 读取认证服务 profile 配置中的 Token 密钥配置值。
     *
     * @param resource classpath 下的 profile 配置文件名。
     * @return 配置值原文，保留 {@code ${ENV:default}} 形式。
     */
    private static String tokenSecret(String resource) {
        return nestedProperty(resource, "foodmap", "security", TOKEN_SECRET_PROPERTY);
    }

    /**
     * 读取认证服务 profile 配置中的密码 pepper 配置值。
     *
     * @param resource classpath 下的 profile 配置文件名。
     * @return 配置值原文，保留 {@code ${ENV:default}} 形式。
     */
    private static String authPepper(String resource) {
        return nestedProperty(resource, "foodmap", "auth", "password", PEPPER_PROPERTY);
    }

    /**
     * 按层级读取 YAML 配置项，避免直接做字符串匹配导致的假通过。
     *
     * @param resource classpath 下的配置文件名。
     * @param path 从配置根到目标配置项的层级路径。
     * @return 目标配置项原文。
     */
    private static String nestedProperty(String resource, String... path) {
        try (InputStream input = TokenSecretConfigContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as("classpath 配置 %s", resource).isNotNull();
            Object node = new Yaml().load(input);
            for (String key : path) {
                assertThat(node).as("%s 中的 %s", resource, key).isInstanceOf(Map.class);
                node = ((Map<?, ?>) node).get(key);
                assertThat(node).as("%s 中的 %s", resource, key).isNotNull();
            }
            return String.valueOf(node);
        } catch (Exception ex) {
            throw new IllegalStateException("读取认证服务配置 " + resource + " 失败", ex);
        }
    }
}
