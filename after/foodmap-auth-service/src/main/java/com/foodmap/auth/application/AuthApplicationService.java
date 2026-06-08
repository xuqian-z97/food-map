package com.foodmap.auth.application;

import com.foodmap.auth.domain.AccountStatus;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.domain.HmacTokenIssuer;
import com.foodmap.auth.domain.LoginResult;
import com.foodmap.auth.domain.LoginType;
import com.foodmap.auth.domain.Pbkdf2PasswordHashService;
import com.foodmap.auth.domain.RegisteredChannel;
import com.foodmap.auth.domain.TokenStatus;
import com.foodmap.auth.application.port.AuthAccountRepository;
import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.application.port.LoginLogRepository;
import com.foodmap.auth.application.port.RefreshTokenRepository;
import com.foodmap.auth.dto.LoginRequest;
import com.foodmap.auth.dto.LoginResponse;
import com.foodmap.auth.dto.RegisterRequest;
import com.foodmap.auth.dto.RegisterResponse;
import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 认证应用服务，编排注册、登录、密码校验和 Token 签发用例。
 *
 * <p>Controller 只与本服务交换 DTO，不接触持久化实体。后续接入真实数据库时替换仓储实现即可。</p>
 */
@Service
public class AuthApplicationService {
    private final AtomicLong accountIdSequence = new AtomicLong(100_000L);
    private final AtomicLong userIdSequence = new AtomicLong(200_000L);
    private final AtomicLong credentialIdSequence = new AtomicLong(300_000L);
    private final AtomicLong tokenIdSequence = new AtomicLong(400_000L);
    private final AtomicLong loginLogIdSequence = new AtomicLong(500_000L);

    private final AuthAccountRepository accountRepository;
    private final AuthCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginLogRepository loginLogRepository;
    private final Pbkdf2PasswordHashService passwordHashService;
    private final HmacTokenIssuer tokenIssuer;
    private final UserProfileProvisionClient userProfileProvisionClient;

    public AuthApplicationService(
            AuthAccountRepository accountRepository,
            AuthCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginLogRepository loginLogRepository,
            Pbkdf2PasswordHashService passwordHashService,
            HmacTokenIssuer tokenIssuer,
            UserProfileProvisionClient userProfileProvisionClient
    ) {
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
        Long accountId = accountIdSequence.incrementAndGet();
        Long userId = userIdSequence.incrementAndGet();
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
        credential.setCredentialId(credentialIdSequence.incrementAndGet());
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
        entity.setTokenId(tokenIdSequence.incrementAndGet());
        entity.setAccountId(accountId);
        entity.setTokenHash(tokenIssuer.tokenHash(refreshToken));
        entity.setExpiresTime(expiresTime);
        entity.setTokenStatus(TokenStatus.ACTIVE.name());
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        refreshTokenRepository.save(entity);
    }

    private void writeLoginLog(Long accountId, String loginIdentifier, LoginResult result) {
        OffsetDateTime now = OffsetDateTime.now();
        LoginLogEntity entity = new LoginLogEntity();
        entity.setLoginLogId(loginLogIdSequence.incrementAndGet());
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
