package com.foodmap.auth.application;

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
import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.TokenClaims;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 认证应用服务，编排注册、登录、密码校验和 Token 签发用例。
 *
 * <p>Controller 只与本服务交换 DTO，不接触持久化实体。业务主键必须通过 AuthBusinessIdGenerator 生成，
 * 不能使用服务内存计数器，避免服务重启后与数据库已有业务主键冲突。</p>
 */
@Service
public class AuthApplicationService {
    private final AuthBusinessIdGenerator businessIdGenerator;
    private final AuthAccountRepository accountRepository;
    private final AuthCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginLogRepository loginLogRepository;
    private final Pbkdf2PasswordHashService passwordHashService;
    private final HmacTokenIssuer tokenIssuer;
    private final UserProfileProvisionClient userProfileProvisionClient;

    public AuthApplicationService(
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
     * 注册账号并保存密码哈希，返回账号和用户业务主键。
     */
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
        return new RegisterResponse(accountId, userId, account.getAccountStatus());
    }

    /**
     * 使用账号名、手机号或邮箱登录，成功后签发 Access Token 和 Refresh Token。
     */
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
        String accessToken = tokenIssuer.issueAccessToken(account.getAccountId(), account.getUserId(), accessExpiresTime);
        String refreshToken = tokenIssuer.issueRefreshToken(account.getAccountId(), account.getUserId(), refreshExpiresTime);
        saveRefreshToken(account.getAccountId(), refreshToken, refreshExpiresTime, now);
        account.setLastLoginTime(now);
        account.setUpdatedTime(now);
        accountRepository.save(account);
        writeLoginLog(account.getAccountId(), request.loginIdentifier(), LoginResult.SUCCESS);
        return new LoginResponse(account.getAccountId(), account.getUserId(), accessToken, refreshToken, accessExpiresTime, refreshExpiresTime);
    }

    /**
     * 使用有效 Refresh Token 刷新 Access Token。MVP 阶段不轮换 Refresh Token，便于前端联调和问题排查。
     */
    public LoginResponse refresh(RefreshTokenRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        TokenClaims claims = tokenIssuer.parseRefreshToken(request.refreshToken());
        ensureTokenNotExpired(claims, now, "Refresh Token已过期");

        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenIssuer.tokenHash(request.refreshToken()))
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token无效"));
        ensureRefreshTokenUsable(refreshTokenEntity, now);

        AuthAccountEntity account = accountRepository.findByAccountId(claims.accountId())
                .orElseThrow(() -> new FoodMapException(CommonErrorCode.UNAUTHORIZED, "账号不存在"));
        if (!AccountStatus.NORMAL.name().equals(account.getAccountStatus())) {
            throw new FoodMapException(CommonErrorCode.FORBIDDEN, "账号状态不允许刷新登录状态");
        }

        OffsetDateTime accessExpiresTime = now.plusHours(2);
        String accessToken = tokenIssuer.issueAccessToken(account.getAccountId(), account.getUserId(), accessExpiresTime);
        return new LoginResponse(
                account.getAccountId(),
                account.getUserId(),
                accessToken,
                request.refreshToken(),
                accessExpiresTime,
                refreshTokenEntity.getExpiresTime()
        );
    }

    /**
     * 退出登录并撤销 Refresh Token。重复退出不暴露令牌是否存在，降低 Token 枚举风险。
     */
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
     * 解析当前 Access Token，返回账号和用户业务主键，供前端启动时确认会话状态。
     */
    public CurrentAuthResponse current(String accessToken) {
        OffsetDateTime now = OffsetDateTime.now();
        TokenClaims claims = tokenIssuer.parseAccessToken(accessToken);
        ensureTokenNotExpired(claims, now, "Access Token已过期");
        return new CurrentAuthResponse(claims.accountId(), claims.userId(), claims.expiresTime());
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
