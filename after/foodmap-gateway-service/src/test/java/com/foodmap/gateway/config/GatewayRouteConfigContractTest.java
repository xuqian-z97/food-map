package com.foodmap.gateway.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关路由配置契约测试，锁定「只暴露显式路由」这一安全边界。
 *
 * <p>Spring Cloud Gateway 的 discovery locator 会为每个 Nacos 服务自动生成 {@code /{serviceId}/**}
 * 路由，而 {@code GatewayAuthFilter} 只按 {@code /api/} 和 {@code /internal/} 前缀做判断，
 * 自动路由会让 {@code /foodmap-log-service/internal/...} 这类路径绕过认证与内部接口拦截。
 * 因此这里直接校验配置文件：locator 必须关闭，路由必须全部显式声明。</p>
 *
 * <p>本测试不启动 Spring 容器，只读取 classpath 下的 {@code application.yml}，
 * 避免依赖 Nacos 和真实服务。运行期返回 404 的验证由 verify 场景负责。</p>
 */
class GatewayRouteConfigContractTest {

    /**
     * 明确允许的路由 id，新增路由必须同步修改本测试，防止误开自动路由。
     */
    private static final List<String> EXPECTED_ROUTE_IDS = List.of(
            "auth-service",
            "user-service",
            "relation-service",
            "store-service",
            "recommendation-service",
            "community-service",
            "media-service",
            "admin-service"
    );

    @Test
    void discoveryLocatorShouldBeDisabled() {
        Map<String, Object> locator = locatorConfig();

        assertThat(locator.get("enabled")).isEqualTo(false);
        assertThat(locator).doesNotContainKey("lower-case-service-id");
    }

    @Test
    void routesShouldBeExplicitAndCoverApiAndInternalPaths() {
        List<Map<String, Object>> routes = routes();

        assertThat(routes).extracting(route -> route.get("id"))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_ROUTE_IDS);
        assertThat(routes).allSatisfy(route -> assertThat(pathPredicate(route))
                .as("路由 %s 必须显式声明 Path 断言", route.get("id"))
                .isNotBlank());
        assertThat(pathPredicate(routeById("auth-service")))
                .contains("/api/auth/**", "/internal/auth/**");
        assertThat(pathPredicate(routeById("user-service")))
                .contains("/api/users/**", "/internal/users/**");
        assertThat(pathPredicate(routeById("admin-service")))
                .contains("/api/admin/**", "/internal/admin/**");
    }

    @Test
    void noRouteShouldExposeServiceIdPrefix() {
        assertThat(routes()).allSatisfy(route -> {
            String id = String.valueOf(route.get("id"));
            assertThat(id).doesNotStartWith("foodmap-");
            assertThat(pathPredicate(route)).doesNotContain("-service/**");
        });
    }

    private static Map<String, Object> locatorConfig() {
        return mapAt(mapAt(mapAt(mapAt(mapAt(root(), "spring"), "cloud"), "gateway"), "discovery"), "locator");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> routes() {
        Object routes = mapAt(mapAt(mapAt(root(), "spring"), "cloud"), "gateway").get("routes");
        assertThat(routes).isInstanceOf(List.class);
        return (List<Map<String, Object>>) routes;
    }

    private static Map<String, Object> routeById(String id) {
        return routes().stream()
                .filter(route -> id.equals(String.valueOf(route.get("id"))))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少路由 " + id));
    }

    @SuppressWarnings("unchecked")
    private static String pathPredicate(Map<String, Object> route) {
        Object predicates = route.get("predicates");
        assertThat(predicates).isInstanceOf(List.class);
        return ((List<Object>) predicates).stream()
                .map(String::valueOf)
                .filter(predicate -> predicate.startsWith("Path="))
                .findFirst()
                .orElse("");
    }

    private static Map<String, Object> root() {
        InputStream input = GatewayRouteConfigContractTest.class.getClassLoader()
                .getResourceAsStream("application.yml");
        assertThat(input).as("classpath:application.yml").isNotNull();
        try (InputStream resource = input) {
            Object loaded = new Yaml().load(resource);
            assertThat(loaded).isInstanceOf(Map.class);
            return copyOf(loaded, "root");
        } catch (Exception ex) {
            throw new IllegalStateException("读取网关 application.yml 失败", ex);
        }
    }

    /**
     * 读取并复制指定层级的子配置，层级缺失时立即失败而不是返回 null。
     *
     * @param node 当前配置层级节点。
     * @param key 子配置键名。
     * @return 子配置的浅拷贝。
     */
    private static Map<String, Object> mapAt(Object node, String key) {
        assertThat(node).as("配置层级 %s 的父节点", key).isInstanceOf(Map.class);
        Object child = ((Map<?, ?>) node).get(key);
        return copyOf(child, key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyOf(Object node, String key) {
        assertThat(node).as("配置层级 %s", key).isInstanceOf(Map.class);
        return new LinkedHashMap<>((Map<String, Object>) node);
    }
}
