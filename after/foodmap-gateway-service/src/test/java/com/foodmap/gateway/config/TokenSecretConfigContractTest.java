package com.foodmap.gateway.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证服务与网关的 Token 密钥配置契约测试。
 *
 * <p>认证服务负责签发、网关负责校验，两边必须引用同一个配置键 {@code foodmap.security.token-secret}、
 * 同一个环境变量 {@code FOODMAP_TOKEN_SECRET}，并且在 local/orbstack 下使用同一个开发默认值。
 * 任一环节漂移都会导致本地或生产登录链路出现「签发通过、校验 401」的问题。</p>
 *
 * <p>本测试跨模块读取 auth 服务配置，只用于比对部署契约，不加载任何 Spring 容器。</p>
 */
class TokenSecretConfigContractTest {

    /**
     * 认证服务模块目录名，用于跨模块定位 auth 配置。
     */
    private static final String AUTH_MODULE = "foodmap-auth-service";

    private static final String TOKEN_SECRET_PROPERTY = "token-secret";

    private static final String EXPECTED_DEV_DEFAULT_SECRET = "foodmap-dev-only-token-secret-please-change";

    private static final String EXPECTED_NO_DEFAULT_SECRET = "${FOODMAP_TOKEN_SECRET}";

    @Test
    void authAndGatewayShouldShareSameTokenSecretKeyAndDevDefault() {
        String gatewayBase = gatewayTokenSecret("application.yml");
        String authLocal = authTokenSecret("application-local.yml");
        String authOrbstack = authTokenSecret("application-orbstack.yml");

        assertThat(gatewayBase).isEqualTo("${FOODMAP_TOKEN_SECRET:" + EXPECTED_DEV_DEFAULT_SECRET + "}");
        assertThat(authLocal).isEqualTo(gatewayBase);
        assertThat(authOrbstack).isEqualTo(gatewayBase);
    }

    @Test
    void prodProfilesShouldRequireTokenSecretWithoutAnyDefault() {
        assertThat(gatewayTokenSecret("application-prod.yml")).isEqualTo(EXPECTED_NO_DEFAULT_SECRET);
        assertThat(authTokenSecret("application-prod.yml")).isEqualTo(EXPECTED_NO_DEFAULT_SECRET);
    }

    /**
     * 读取网关自身 classpath 配置中的 Token 密钥值。
     *
     * @param fileName classpath 下的配置文件名。
     * @return 配置值原文。
     */
    private static String gatewayTokenSecret(String fileName) {
        InputStream input = TokenSecretConfigContractTest.class.getClassLoader().getResourceAsStream(fileName);
        assertThat(input).as("classpath 配置 %s", fileName).isNotNull();
        try (InputStream resource = input) {
            return tokenSecret(resource);
        } catch (Exception ex) {
            throw new IllegalStateException("读取网关配置失败：" + fileName, ex);
        }
    }

    /**
     * 读取认证服务模块内的 profile 配置中的 Token 密钥值，兼容不同的 Maven 执行目录。
     *
     * @param fileName 配置文件名。
     * @return 配置值原文。
     */
    private static String authTokenSecret(String fileName) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("..", AUTH_MODULE, "src", "main", "resources", fileName));
        String multiModuleRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (multiModuleRoot != null && !multiModuleRoot.isBlank()) {
            candidates.add(Path.of(multiModuleRoot, AUTH_MODULE, "src", "main", "resources", fileName));
            candidates.add(Path.of(multiModuleRoot, "after", AUTH_MODULE, "src", "main", "resources", fileName));
        }
        Path resolved = candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "无法定位认证服务配置 " + fileName + "，候选路径：" + candidates));
        try (InputStream input = Files.newInputStream(resolved)) {
            return tokenSecret(input);
        } catch (Exception ex) {
            throw new IllegalStateException("读取认证服务配置失败：" + resolved, ex);
        }
    }

    /**
     * 按层级读取 YAML 配置项，避免字符串匹配带来的假通过。
     *
     * @param input 配置文件输入流。
     * @return Token 密钥配置值原文。
     */
    private static String tokenSecret(InputStream input) {
        Object node = new Yaml().load(input);
        for (String key : new String[]{"foodmap", "security", TOKEN_SECRET_PROPERTY}) {
            assertThat(node).as("配置层级 %s", key).isInstanceOf(Map.class);
            node = ((Map<?, ?>) node).get(key);
            assertThat(node).as("配置项 %s", key).isNotNull();
        }
        return String.valueOf(node);
    }
}
