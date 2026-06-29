package com.foodmap.auth.service.impl;

import com.foodmap.auth.application.UserProfileProvisionClient;
import com.foodmap.auth.domain.AccountStatus;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.domain.HmacTokenIssuer;
import com.foodmap.auth.domain.LoginResult;
import com.foodmap.auth.domain.LoginType;
import com.foodmap.auth.domain.Pbkdf2PasswordHashService;
import com.foodmap.auth.domain.RegisteredChannel;
import com.foodmap.auth.domain.TokenStatus;
import com.foodmap.auth.application.port.AuthBusinessIdGenerator;
import com.foodmap.auth.application.port.AuthAccountRepository;
import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.application.port.LoginLogRepository;
import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.LogoutRequest;
import com.foodmap.auth.dto.CurrentAuthResponse;
import com.foodmap.auth.dto.RefreshTokenRequest;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.auth.service.AuthService;
import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.TokenClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 认证业务服务实现，编排注册、登录、密码校验和 Token 签发用例。
 *
 * <p>Controller 只与本服务交换 DTO，不接触持久化实体。业务主键必须通过 AuthBusinessIdGenerator 生成，
 * 不能使用服务内存计数器，避免服务重启后与数据库已有业务主键冲突。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final AuthBusinessIdGenerator businessIdGenerator;
    private final AuthAccountRepository accountRepository;
    private final AuthCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginLogRepository loginLogRepository;
    private final Pbkdf2PasswordHashService passwordHashService;
    private final HmacTokenIssuer tokenIssuer;
    private final UserProfileProvisionClient userProfileProvisionClient;

    public AuthServiceImpl(
            AuthBusinessIdGenerator businessIdGenerator,
            AuthAccountRepository accountRepository,
            AuthCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginLogRepository loginLogRepository,
            Pbkdf2PasswordHashService passwordHashService,
            HmacTokenIssuer tokenIssuer,
            UserProfileProvisionClient userProfileProvisionClient
    ) {
        this.businessIdGenerator = businessIdGenerator;
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordHashService = passwordHashService;
        this.tokenIssuer = tokenIssuer;
        this.userProfileProvisionClient = userProfileProvisionClient;
    }

    /**
     * 注册登录身份并保存密码哈希，返回标准用户业务主键。
     *
     * @param request 注册请求，包含账号标识、密码和昵称等信息。
     * @return 注册成功后的用户业务主键和账号状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest request) {
        ensureLoginIdentifierAvailable(request.accountName(), request.phone(), request.email());
        Long accountId = businessIdGenerator.nextAccountId();
        Long userId = businessIdGenerator.nextUserId();
        OffsetDateTime now = OffsetDateTime.now();

        AuthAccountEntity account = new AuthAccountEntity();
        account.setAccountId(accountId);
        account.setUserId(userId);
        account.setAccountName(request.accountName());
        account.setPhone(blankToNull(request.phone()));
        account.setEmail(blankToNull(request.email()));
        account.setAccountStatus(AccountStatus.NORMAL.name());
        account.setRegisteredChannel(resolveRegisteredChannel(request.registeredChannel()).name());
        account.setCreatedTime(now);
        account.setUpdatedTime(now);
        account.setIsDelete((short) 0);
        accountRepository.save(account);

        AuthCredentialEntity credential = new AuthCredentialEntity();
        credential.setCredentialId(businessIdGenerator.nextCredentialId());
        credential.setAccountId(accountId);
        credential.setCredentialType(CredentialType.PASSWORD.name());
        credential.setPasswordHash(passwordHashService.hash(request.password()));
        credential.setHashAlgorithm("PBKDF2WithHmacSHA256");
        credential.setCreatedTime(now);
        credential.setUpdatedTime(now);
        credential.setIsDelete((short) 0);
        credentialRepository.save(credential);

        userProfileProvisionClient.provision(accountId, userId, request);
        return new RegisterResponse(null, userId, account.getAccountStatus());
    }

    /**
     * 使用账号名、手机号或邮箱登录，成功后签发 Access Token 和 Refresh Token。
     *
     * @param request 登录请求，包含登录标识和明文密码。
     * @return 登录成功后的 Token、过期时间和用户业务主键。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        AuthAccountEntity account = accountRepository.findByLoginIdentifier(request.loginIdentifier())
                .orElseThrow(() -> {
                    writeLoginLog(null, request.loginIdentifier(), LoginResult.FAILED);
                    return new FoodMapException(CommonErrorCode.UNAUTHORIZED, "账号或密码错误");
                });
        if (!AccountStatus.NORMAL.name().equals(account.getAccountStatus())) {
            writeLoginLog(account.getAccountId(), request.loginIdentifier(), LoginResult.FAILED);
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "账号状态不允许登录");
        }
        AuthCredentialEntity credential = credentialRepository.findPasswordByAccountId(account.getAccountId())
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.UNAUTHORIZED, "账号或密码错误"));
        if (!passwordHashService.matches(request.password(), credential.getPasswordHash())) {
            writeLoginLog(account.getAccountId(), request.loginIdentifier(), LoginResult.FAILED);
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime accessExpiresTime = now.plusHours(2);
        OffsetDateTime refreshExpiresTime = now.plusDays(30);
        String accessToken = tokenIssuer.issueAccessToken(account.getUserId(), accessExpiresTime);
        String refreshToken = tokenIssuer.issueRefreshToken(account.getUserId(), refreshExpiresTime);
        saveRefreshToken(account.getAccountId(), refreshToken, refreshExpiresTime, now);
        account.setLastLoginTime(now);
        account.setUpdatedTime(now);
        accountRepository.save(account);
        writeLoginLog(account.getAccountId(), request.loginIdentifier(), LoginResult.SUCCESS);
        return new LoginResponse(null, account.getUserId(), accessToken, refreshToken, accessExpiresTime, refreshExpiresTime);
    }

    /**
     * 使用有效 Refresh Token 刷新 Access Token。MVP 阶段不轮换 Refresh Token，便于前端联调和问题排查。
     *
     * @param request 刷新请求，包含 Refresh Token。
     * @return 刷新后的 Access Token 以及 Refresh Token 有效期信息。
     */
    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        TokenClaims claims = tokenIssuer.parseRefreshToken(request.refreshToken());
        ensureTokenNotExpired(claims, now, "Refresh Token已过期");

        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenIssuer.tokenHash(request.refreshToken()))
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token无效"));
        ensureRefreshTokenUsable(refreshTokenEntity, now);

        Long legacyAccountId = refreshTokenEntity.getAccountId();
        if (legacyAccountId == null) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token会话无效");
        }
        AuthAccountEntity account = accountRepository.findByAccountId(legacyAccountId)
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.UNAUTHORIZED, "账号不存在"));
        ensureRefreshTokenUserMatches(account, claims);
        if (!AccountStatus.NORMAL.name().equals(account.getAccountStatus())) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "账号状态不允许刷新登录状态");
        }

        OffsetDateTime accessExpiresTime = now.plusHours(2);
        String accessToken = tokenIssuer.issueAccessToken(account.getUserId(), accessExpiresTime);
        return new LoginResponse(
                null,
                account.getUserId(),
                accessToken,
                request.refreshToken(),
                accessExpiresTime,
                refreshTokenEntity.getExpiresTime()
        );
    }

    /**
     * 退出登录并撤销 Refresh Token。重复退出不暴露令牌是否存在，降低 Token 枚举风险。
     *
     * @param request 退出登录请求，包含待撤销的 Refresh Token。
     */
    @Override
    public void logout(LogoutRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        tokenIssuer.parseRefreshToken(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenIssuer.tokenHash(request.refreshToken()))
                .ifPresent(entity -> {
                    if (TokenStatus.ACTIVE.name().equals(entity.getTokenStatus())) {
                        refreshTokenRepository.revoke(entity, now);
                    }
                });
    }

    /**
     * 解析当前 Access Token，返回用户业务主键，供前端启动时确认会话状态。
     *
     * @param accessToken 当前客户端持有的 Access Token。
     * @return 当前认证会话中的用户业务主键信息。
     */
    @Override
    public CurrentAuthResponse current(String accessToken) {
        OffsetDateTime now = OffsetDateTime.now();
        TokenClaims claims = tokenIssuer.parseAccessToken(accessToken);
        ensureTokenNotExpired(claims, now, "Access Token已过期");
        return new CurrentAuthResponse(null, claims.userId(), claims.expiresTime());
    }

    private void ensureLoginIdentifierAvailable(String accountName, String phone, String email) {
        if (accountRepository.findByLoginIdentifier(accountName).isPresent()) {
            throw new FoodMapException(CommonErrorCode.CONFLICT, "账号名已被使用");
        }
        if (phone != null && !phone.isBlank() && accountRepository.findByLoginIdentifier(phone).isPresent()) {
            throw new FoodMapException(CommonErrorCode.CONFLICT, "手机号已被使用");
        }
        if (email != null && !email.isBlank() && accountRepository.findByLoginIdentifier(email).isPresent()) {
            throw new FoodMapException(CommonErrorCode.CONFLICT, "邮箱已被使用");
        }
    }

    private void saveRefreshToken(Long accountId, String refreshToken, OffsetDateTime expiresTime, OffsetDateTime now) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenId(businessIdGenerator.nextRefreshTokenId());
        entity.setAccountId(accountId);
        entity.setTokenHash(tokenIssuer.tokenHash(refreshToken));
        entity.setExpiresTime(expiresTime);
        entity.setTokenStatus(TokenStatus.ACTIVE.name());
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        refreshTokenRepository.save(entity);
    }

    private void ensureRefreshTokenUsable(RefreshTokenEntity entity, OffsetDateTime now) {
        if (!TokenStatus.ACTIVE.name().equals(entity.getTokenStatus())) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token已失效");
        }
        if (!entity.getExpiresTime().isAfter(now)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token已过期");
        }
    }

    /**
     * 校验 Refresh Token 声明的用户业务主键与落库会话关联账号一致，避免刷新链路继续信任 legacy accountId claims。
     *
     * @param account 落库 Refresh Token 记录关联的 legacy 账号。
     * @param claims Refresh Token 解析出的身份声明。
     */
    private void ensureRefreshTokenUserMatches(AuthAccountEntity account, TokenClaims claims) {
        if (!claims.userId().equals(account.getUserId())) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token会话不匹配");
        }
    }

    private void ensureTokenNotExpired(TokenClaims claims, OffsetDateTime now, String message) {
        if (claims.isExpiredAt(now)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    private void writeLoginLog(Long accountId, String loginIdentifier, LoginResult result) {
        OffsetDateTime now = OffsetDateTime.now();
        LoginLogEntity entity = new LoginLogEntity();
        entity.setLoginLogId(businessIdGenerator.nextLoginLogId());
        entity.setAccountId(accountId);
        entity.setLoginType(resolveLoginType(loginIdentifier).name());
        entity.setLoginResult(result.name());
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        loginLogRepository.save(entity);
    }

    private LoginType resolveLoginType(String loginIdentifier) {
        if (loginIdentifier != null && loginIdentifier.contains("@")) {
            return LoginType.EMAIL;
        }
        if (loginIdentifier != null && loginIdentifier.chars().allMatch(Character::isDigit)) {
            return LoginType.PHONE;
        }
        return LoginType.ACCOUNT_NAME;
    }

    private RegisteredChannel resolveRegisteredChannel(String value) {
        if (value == null || value.isBlank()) {
            return RegisteredChannel.IOS;
        }
        return RegisteredChannel.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
