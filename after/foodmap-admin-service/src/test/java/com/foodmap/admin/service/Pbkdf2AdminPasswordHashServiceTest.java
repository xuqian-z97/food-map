package com.foodmap.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Pbkdf2AdminPasswordHashService} 的 pepper 注入单测。
 *
 * <p>覆盖三类行为：非本机 profile 缺少 pepper 时启动失败、local profile 允许开发回退值、
 * 显式传入 pepper 时哈希正常产出。用例只做内存计算，不连接数据库。</p>
 */
class Pbkdf2AdminPasswordHashServiceTest {

    /**
     * 测试用明文密码。
     */
    private static final String RAW_PASSWORD = "secret123";

    @Test
    void shouldFailWhenPepperMissingOutsideLocalOrOrbstackProfile() {
        assertThatThrownBy(() -> new Pbkdf2AdminPasswordHashService("", environmentWithProfiles("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("foodmap.admin.password.pepper")
                .hasMessageContaining("ADMIN_PEPPER")
                .hasMessageContaining("prod");
    }

    @Test
    void shouldFailWhenPepperMissingAndNoProfileIsActive() {
        assertThatThrownBy(() -> new Pbkdf2AdminPasswordHashService(null, environmentWithProfiles()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PEPPER");
    }

    @Test
    void shouldUseDevFallbackPepperForLocalProfile() {
        Pbkdf2AdminPasswordHashService service =
                new Pbkdf2AdminPasswordHashService("  ", environmentWithProfiles("local"));

        String hash = service.hash(RAW_PASSWORD);

        assertThat(hash).startsWith("PBKDF2WithHmacSHA256$120000$").doesNotContain(RAW_PASSWORD);
    }

    @Test
    void shouldHashWithExplicitPepper() {
        Pbkdf2AdminPasswordHashService service = new Pbkdf2AdminPasswordHashService("test-pepper");

        String hash = service.hash(RAW_PASSWORD);

        assertThat(hash).startsWith("PBKDF2WithHmacSHA256$120000$").doesNotContain(RAW_PASSWORD);
        assertThat(hash.split("\\$")).hasSize(4);
    }

    /**
     * 构造带指定激活 profile 的测试 Environment，用于验证开发回退值只对 local/orbstack 生效。
     *
     * @param profiles 需要激活的 profile 名称。
     * @return 只包含激活 profile 的 MockEnvironment。
     */
    private static MockEnvironment environmentWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }

    @Test
    void shouldRequireAdminPepperInProdProfileConfig() {
        try (InputStream input = Pbkdf2AdminPasswordHashServiceTest.class.getClassLoader()
                .getResourceAsStream("application-prod.yml")) {
            assertThat(input).as("classpath 配置 application-prod.yml").isNotNull();
            Map<?, ?> root = new Yaml().load(input);
            Map<?, ?> admin = (Map<?, ?>) ((Map<?, ?>) root.get("foodmap")).get("admin");
            Map<?, ?> password = (Map<?, ?>) admin.get("password");
            assertThat(password.get("pepper")).isEqualTo("${ADMIN_PEPPER}");
        } catch (Exception ex) {
            throw new IllegalStateException("读取 admin prod 配置失败", ex);
        }
    }
}
