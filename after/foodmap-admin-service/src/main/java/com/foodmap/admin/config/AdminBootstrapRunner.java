package com.foodmap.admin.config;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.application.port.AdminRoleRepository;
import com.foodmap.admin.application.port.AdminUserRepository;
import com.foodmap.admin.dto.AdminUserResponse;
import com.foodmap.admin.dto.CreateAdminUserRequest;
import com.foodmap.admin.service.AdminUserService;
import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.SafeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * 后台首批管理员受控初始化入口，复用现有账号创建用例并显式分配角色。
 *
 * <p>只应在一次性本地运维命令或部署初始化阶段启用；密码从环境变量注入，重复执行幂等。</p>
 */
@Configuration
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final AdminUserService adminUserService;
    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminBusinessIdGenerator businessIdGenerator;

    /**
     * 创建受控初始化入口。
     *
     * @param properties 受控初始化配置。
     * @param adminUserService 后台管理员账号创建用例。
     * @param adminUserRepository 后台管理员仓储端口。
     * @param adminRoleRepository 后台角色仓储端口。
     * @param businessIdGenerator 后台业务主键生成端口。
     */
    public AdminBootstrapRunner(AdminBootstrapProperties properties,
                                AdminUserService adminUserService,
                                AdminUserRepository adminUserRepository,
                                AdminRoleRepository adminRoleRepository,
                                AdminBusinessIdGenerator businessIdGenerator) {
        this.properties = properties;
        this.adminUserService = adminUserService;
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.businessIdGenerator = businessIdGenerator;
    }

    /**
     * 按配置创建缺失的首批管理员并授予角色，账号已存在时只补齐角色授权。
     *
     * @param args 启动参数，本入口不使用。
     * @throws IllegalStateException 配置的角色编码不存在有效定义时抛出，阻止带错误授权启动。
     */
    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            return;
        }
        for (AdminBootstrapProperties.Entry entry : properties.getEntries()) {
            if (entry.getUsername() == null || entry.getUsername().isBlank()) {
                continue;
            }
            bootstrap(entry);
        }
    }

    /**
     * 初始化单个管理员账号及其角色授权。
     *
     * @param entry 管理员条目配置。
     */
    private void bootstrap(AdminBootstrapProperties.Entry entry) {
        AdminUserResponse admin = adminUserRepository.findByUsername(entry.getUsername().trim())
                .map(entity -> new AdminUserResponse(entity.getAdminUserId(), entity.getUsername(),
                        entity.getDisplayName(), entity.getMobileMasked(), entity.getEmailMasked(),
                        entity.getAdminStatus(), entity.getPermissionVersion()))
                .orElseGet(() -> createAdmin(entry));
        for (String roleCode : entry.getRoleCodes()) {
            String normalizedRoleCode = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
            if (normalizedRoleCode.isEmpty()) {
                continue;
            }
            Long roleId = adminRoleRepository.findActiveRoleIdByCode(normalizedRoleCode)
                    .orElseThrow(() -> new IllegalStateException("受控初始化失败：角色编码不存在有效定义 "
                            + normalizedRoleCode));
            boolean granted = adminRoleRepository.grantRole(
                    businessIdGenerator.nextAdminUserRoleRelationId(),
                    admin.adminUserId(),
                    roleId,
                    OffsetDateTime.now());
            SafeLog.info(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.bootstrap.grant",
                    LogField.of("adminUserId", admin.adminUserId()),
                    LogField.of("roleCode", normalizedRoleCode),
                    LogField.of("granted", granted));
        }
        SafeLog.info(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.bootstrap.ready",
                LogField.of("adminUserId", admin.adminUserId()),
                LogField.of("username", admin.username()));
    }

    /**
     * 通过现有账号创建用例创建管理员，密码只来自环境变量注入的配置。
     *
     * @param entry 管理员条目配置。
     * @return 创建后的管理员响应。
     */
    private AdminUserResponse createAdmin(AdminBootstrapProperties.Entry entry) {
        if (entry.getPassword() == null || entry.getPassword().isBlank()) {
            throw new IllegalStateException("受控初始化失败：账号 " + entry.getUsername()
                    + " 未通过环境变量注入初始密码");
        }
        AdminUserResponse created = adminUserService.createAdminUser(new CreateAdminUserRequest(
                entry.getUsername().trim(),
                entry.getPassword(),
                entry.getDisplayName() == null || entry.getDisplayName().isBlank()
                        ? entry.getUsername().trim() : entry.getDisplayName().trim(),
                null,
                null
        ));
        SafeLog.info(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.bootstrap.created",
                LogField.of("adminUserId", created.adminUserId()),
                LogField.of("username", created.username()));
        return created;
    }
}
