package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.application.port.AdminRoleRepository;
import com.foodmap.admin.application.port.AdminUserRepository;
import com.foodmap.admin.domain.AdminStatus;
import com.foodmap.admin.dto.AdminLoginRequest;
import com.foodmap.admin.dto.AdminLoginResponse;
import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import com.foodmap.admin.security.AdminTokenIssuer;
import com.foodmap.admin.security.AuthenticatedAdmin;
import com.foodmap.admin.service.AdminAuthService;
import com.foodmap.admin.service.AdminLoginRateLimiter;
import com.foodmap.admin.service.AdminOperationLogService;
import com.foodmap.admin.service.AdminPasswordHashService;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.validation.Check;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 后台认证应用服务实现，负责后台管理员登录签发、会话登记和登出撤销。
 *
 * <p>登录失败提示统一，避免暴露账号是否存在；未知账号使用占位哈希执行等成本校验。密码计算在数据库
 * 事务之外完成，成功后在短事务内锁定账号行、复核状态与哈希、读取当前角色与权限版本、签发 Token、
 * 登记会话并写成功审计，事务提交后才向客户端返回 Token。</p>
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {
    /**
     * 后台登录账号名最大长度，与账号创建口径一致。
     */
    private static final int MAX_USERNAME_LENGTH = 64;
    /**
     * 后台登录密码最大长度，用于限制登录请求体规模。
     */
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final String BEARER_TYPE = "Bearer";
    private static final String FAILURE_UNKNOWN_USERNAME = "UNKNOWN_USERNAME";
    private static final String FAILURE_PASSWORD_MISMATCH = "PASSWORD_MISMATCH";
    private static final String FAILURE_RECHECK = "LOGIN_RECHECK_FAILED";

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminAccessSessionRepository accessSessionRepository;
    private final AdminBusinessIdGenerator businessIdGenerator;
    private final AdminPasswordHashService passwordHashService;
    private final AdminTokenIssuer tokenIssuer;
    private final AdminOperationLogService operationLogService;
    private final AdminLoginRateLimiter loginRateLimiter;
    private final TransactionOperations transactionOperations;

    /**
     * 创建后台认证应用服务。
     *
     * @param adminUserRepository 后台管理员仓储端口。
     * @param adminRoleRepository 后台角色仓储端口。
     * @param accessSessionRepository 后台会话仓储端口。
     * @param businessIdGenerator 后台业务主键生成端口。
     * @param passwordHashService 后台密码哈希服务。
     * @param tokenIssuer 后台 Token 签发组件。
     * @param operationLogService 后台操作审计服务。
     * @param loginRateLimiter 后台登录限流器。
     * @param transactionOperations 事务模板，登录短事务入口，避免同类内部调用绕过事务代理。
     */
    public AdminAuthServiceImpl(AdminUserRepository adminUserRepository,
                                AdminRoleRepository adminRoleRepository,
                                AdminAccessSessionRepository accessSessionRepository,
                                AdminBusinessIdGenerator businessIdGenerator,
                                AdminPasswordHashService passwordHashService,
                                AdminTokenIssuer tokenIssuer,
                                AdminOperationLogService operationLogService,
                                AdminLoginRateLimiter loginRateLimiter,
                                TransactionOperations transactionOperations) {
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.accessSessionRepository = accessSessionRepository;
        this.businessIdGenerator = businessIdGenerator;
        this.passwordHashService = passwordHashService;
        this.tokenIssuer = tokenIssuer;
        this.operationLogService = operationLogService;
        this.loginRateLimiter = loginRateLimiter;
        this.transactionOperations = transactionOperations;
    }

    /**
     * 后台管理员登录，签发后台 Access Token 并登记会话。
     *
     * @param request 后台登录请求。
     * @return 后台登录响应，含 Token 文本和过期时间。
     */
    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        String username = Check.notBlank("username", request.username());
        String password = Check.notBlank("password", request.password());
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new FoodMapException(CommonErrorCode.BAD_REQUEST, "后台登录账号名长度不能超过 64 字符");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new FoodMapException(CommonErrorCode.BAD_REQUEST, "后台登录密码长度不能超过 128 字符");
        }
        loginRateLimiter.check(username);

        AdminUserEntity account;
        try {
            account = adminUserRepository.findByUsername(username).orElse(null);
        } catch (DataAccessException ex) {
            throw serviceUnavailable(ex);
        }
        if (account == null) {
            passwordHashService.matches(password, placeholderHash());
            loginRateLimiter.recordFailure(username);
            operationLogService.recordLoginFailure(null, username, FAILURE_UNKNOWN_USERNAME);
            throw loginFailed();
        }
        if (!passwordHashService.matches(password, account.getPasswordHash())) {
            loginRateLimiter.recordFailure(username);
            operationLogService.recordLoginFailure(account.getAdminUserId(), username, FAILURE_PASSWORD_MISMATCH);
            throw loginFailed();
        }
        if (!AdminStatus.ACTIVE.name().equals(account.getAdminStatus())) {
            loginRateLimiter.recordFailure(username);
            operationLogService.recordLoginFailure(account.getAdminUserId(), username,
                    "ADMIN_STATUS_" + account.getAdminStatus());
            throw loginFailed();
        }

        LoginOutcome outcome;
        try {
            outcome = transactionOperations.execute(status ->
                    loginInShortTransaction(account.getAdminUserId(), password));
        } catch (DataAccessException ex) {
            throw serviceUnavailable(ex);
        }
        if (outcome.failureReason() != null) {
            loginRateLimiter.recordFailure(username);
            operationLogService.recordLoginFailure(account.getAdminUserId(), username, outcome.failureReason());
            throw loginFailed();
        }
        loginRateLimiter.reset(username);
        return new AdminLoginResponse(outcome.token().tokenValue(), BEARER_TYPE, outcome.token().expiresTime());
    }

    /**
     * 后台管理员登出，撤销当前 Token 会话，重复登出幂等成功。
     *
     * @param admin 认证拦截器装配的后台管理员上下文。
     */
    @Override
    public void logout(AuthenticatedAdmin admin) {
        try {
            transactionOperations.execute(status -> {
                boolean revoked = accessSessionRepository.revokeActive(
                        admin.tokenId(), admin.adminUserId(), OffsetDateTime.now());
                if (revoked) {
                    operationLogService.recordLogout(admin.adminUserId(), admin.tokenId());
                }
                return null;
            });
        } catch (DataAccessException ex) {
            throw new FoodMapException(CommonErrorCode.SERVICE_UNAVAILABLE, "管理后台会话数据源暂不可用", ex);
        }
    }

    /**
     * 登录短事务：锁定账号行复核状态与哈希，读取角色与权限版本，签发 Token、登记会话并写成功审计。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param password 原始密码，用于复核锁定行上的存量哈希。
     * @return 登录结果，复核失败时携带稳定失败原因。
     */
    private LoginOutcome loginInShortTransaction(Long adminUserId, String password) {
        Optional<AdminUserEntity> locked = adminUserRepository.lockByAdminUserId(adminUserId);
        if (locked.isEmpty()
                || !AdminStatus.ACTIVE.name().equals(locked.get().getAdminStatus())
                || !passwordHashService.matches(password, locked.get().getPasswordHash())) {
            return LoginOutcome.failure(FAILURE_RECHECK);
        }
        AdminUserEntity user = locked.get();
        List<String> roles = adminRoleRepository.findActiveRoleCodes(adminUserId);
        AdminTokenIssuer.AdminIssuedToken token = tokenIssuer.issue(
                adminUserId, roles, user.getPermissionVersion());
        OffsetDateTime now = OffsetDateTime.now();
        adminUserRepository.updateLastLoginTime(adminUserId, now);
        accessSessionRepository.register(sessionEntity(adminUserId, token));
        operationLogService.recordLoginSuccess(adminUserId, user.getUsername());
        return LoginOutcome.success(token);
    }

    /**
     * 构造后台 Token 会话登记实体。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param token 签发结果。
     * @return 会话登记实体。
     */
    private AdminAccessSessionEntity sessionEntity(Long adminUserId, AdminTokenIssuer.AdminIssuedToken token) {
        AdminAccessSessionEntity entity = new AdminAccessSessionEntity();
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        entity.setSessionId(businessIdGenerator.nextAdminAccessSessionId());
        entity.setTokenId(token.tokenId());
        entity.setAdminUserId(adminUserId);
        entity.setExpiresTime(token.expiresTime());
        return entity;
    }

    /**
     * 读取未知账号占位哈希，与实例方法分离便于统一异常口径。
     *
     * @return 占位哈希。
     */
    private String placeholderHash() {
        if (passwordHashService instanceof com.foodmap.admin.service.Pbkdf2AdminPasswordHashService concrete) {
            return concrete.placeholderHash();
        }
        return passwordHashService.hash("foodmap-admin-placeholder");
    }

    /**
     * 构造统一登录失败提示，避免暴露账号是否存在或具体失败原因。
     *
     * @return 401 业务异常。
     */
    private static FoodMapException loginFailed() {
        return new FoodMapException(CommonErrorCode.UNAUTHORIZED, "后台账号名或密码错误");
    }

    /**
     * 构造数据源不可用异常，保留原始根因用于服务端排查。
     *
     * @param cause 数据访问异常根因。
     * @return 503 业务异常。
     */
    private static FoodMapException serviceUnavailable(Exception cause) {
        return new FoodMapException(CommonErrorCode.SERVICE_UNAVAILABLE, "管理后台身份数据源暂不可用", cause);
    }

    /**
     * 登录短事务执行结果，成功携带签发 Token，失败携带稳定失败原因。
     */
    private record LoginOutcome(AdminTokenIssuer.AdminIssuedToken token, String failureReason) {
        static LoginOutcome success(AdminTokenIssuer.AdminIssuedToken token) {
            return new LoginOutcome(token, null);
        }

        static LoginOutcome failure(String reason) {
            return new LoginOutcome(null, reason);
        }
    }
}
