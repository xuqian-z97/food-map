package com.foodmap.admin.config;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.infrastructure.persistence.memory.InMemoryAdminUserRepository;
import com.foodmap.admin.service.Pbkdf2AdminPasswordHashService;
import com.foodmap.admin.service.TestAdminBusinessIdGenerator;
import com.foodmap.admin.service.impl.AdminUserServiceImpl;
import com.foodmap.admin.service.impl.FakeAdminRoleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 后台首批管理员受控初始化测试：创建、幂等授权与未知角色失败。
 */
class AdminBootstrapRunnerTest {

    @Test
    void shouldSkipWhenDisabled() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        InMemoryAdminUserRepository userRepository = new InMemoryAdminUserRepository();
        AdminBootstrapRunner runner = runner(properties, userRepository, new FakeAdminRoleRepository());

        runner.run();

        assertThat(userRepository.findByUsername("anyone")).isEmpty();
    }

    @Test
    void shouldCreateAdminAndGrantConfiguredRoles() {
        AdminBootstrapProperties properties = enabledProperties("bootstrap_admin", "Bootstrap Admin", "secret123");
        InMemoryAdminUserRepository userRepository = new InMemoryAdminUserRepository();
        FakeAdminRoleRepository roleRepository = new FakeAdminRoleRepository();
        roleRepository.putRole("OPS_VIEWER", 100001L);

        runner(properties, userRepository, roleRepository).run();

        assertThat(userRepository.findByUsername("bootstrap_admin")).isPresent();
        assertThat(roleRepository.grantedRelations()).containsExactly("9001:100001");
    }

    @Test
    void shouldBeIdempotentWhenReRunAndKeepExistingAccount() {
        AdminBootstrapProperties properties = enabledProperties("bootstrap_admin", "Bootstrap Admin", "secret123");
        InMemoryAdminUserRepository userRepository = new InMemoryAdminUserRepository();
        FakeAdminRoleRepository roleRepository = new FakeAdminRoleRepository();
        roleRepository.putRole("OPS_VIEWER", 100001L);
        AdminBootstrapRunner runner = runner(properties, userRepository, roleRepository);

        runner.run();
        runner.run();

        assertThat(userRepository.findByUsername("bootstrap_admin")).isPresent();
        assertThat(roleRepository.grantedRelations()).containsExactly("9001:100001");
    }

    @Test
    void shouldSkipBlankUsernameEntries() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        properties.getEntries().add(entry("  ", "显示名", "secret123", List.of("OPS_VIEWER")));
        InMemoryAdminUserRepository userRepository = new InMemoryAdminUserRepository();

        runner(properties, userRepository, new FakeAdminRoleRepository()).run();

        assertThat(userRepository.findByUsername("")).isEmpty();
    }

    @Test
    void shouldFailWhenConfiguredRoleMissing() {
        AdminBootstrapProperties properties = enabledProperties("bootstrap_admin", "Bootstrap Admin", "secret123");
        FakeAdminRoleRepository emptyRoles = new FakeAdminRoleRepository();

        assertThatThrownBy(() -> runner(properties, new InMemoryAdminUserRepository(), emptyRoles).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPS_VIEWER");
    }

    /**
     * 组装受控初始化入口。
     *
     * @param properties 初始化配置。
     * @param userRepository 用户仓储。
     * @param roleRepository 角色仓储。
     * @return 受控初始化入口。
     */
    private static AdminBootstrapRunner runner(AdminBootstrapProperties properties,
                                               InMemoryAdminUserRepository userRepository,
                                               FakeAdminRoleRepository roleRepository) {
        AdminBusinessIdGenerator idGenerator = new TestAdminBusinessIdGenerator();
        return new AdminBootstrapRunner(
                properties,
                new AdminUserServiceImpl(idGenerator, userRepository, new Pbkdf2AdminPasswordHashService("test-pepper")),
                userRepository,
                roleRepository,
                idGenerator
        );
    }

    /**
     * 构造启用的单管理员初始化配置。
     *
     * @param username 账号名。
     * @param displayName 展示名。
     * @param password 初始密码。
     * @return 初始化配置。
     */
    private static AdminBootstrapProperties enabledProperties(String username, String displayName, String password) {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        properties.getEntries().add(entry(username, displayName, password, List.of("OPS_VIEWER")));
        return properties;
    }

    /**
     * 构造管理员条目配置。
     *
     * @param username 账号名。
     * @param displayName 展示名。
     * @param password 初始密码。
     * @param roleCodes 角色编码。
     * @return 条目配置。
     */
    private static AdminBootstrapProperties.Entry entry(String username, String displayName,
                                                        String password, List<String> roleCodes) {
        AdminBootstrapProperties.Entry entry = new AdminBootstrapProperties.Entry();
        entry.setUsername(username);
        entry.setDisplayName(displayName);
        entry.setPassword(password);
        entry.setRoleCodes(roleCodes);
        return entry;
    }
}
