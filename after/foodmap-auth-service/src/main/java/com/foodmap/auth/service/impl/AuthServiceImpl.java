package com.foodmap.auth.service.impl;

import com.foodmap.auth.application.UserProfileProvisionClient;
import com.foodmap.auth.domain.AccountStatus;
import com.foodmap.auth.domain.AuthSessionStatus;
import com.foodmap.auth.domain.CredentialType;
import com.foodmap.auth.domain.HmacTokenIssuer;
import com.foodmap.auth.domain.IdentityProvider;
import com.foodmap.auth.domain.IdentityStatus;
import com.foodmap.auth.domain.IdentityType;
import com.foodmap.auth.domain.LoginResult;
import com.foodmap.auth.domain.LoginType;
import com.foodmap.auth.domain.Pbkdf2PasswordHashService;
import com.foodmap.auth.domain.RegisteredChannel;
import com.foodmap.auth.domain.TokenStatus;
import com.foodmap.auth.application.port.AuthBusinessIdGenerator;
import com.foodmap.auth.application.port.AuthAccountRepository;
import com.foodmap.auth.application.port.AuthCredentialRepository;
import com.foodmap.auth.application.port.AuthIdentityBindingRepository;
import com.foodmap.auth.application.port.AuthSessionRepository;
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
import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;
import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.common.security.AccessTokenDenylistClient;
import com.foodmap.common.security.NoopAccessTokenDenylistClient;
import com.foodmap.common.security.TokenClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 认证业务服务实现，编排注册、登录、刷新、退出登录和 Token 签发用例。
 *
 * <p>Controller 只与本服务交换 DTO，不接触持久化实体。业务主键必须通过 AuthBusinessIdGenerator 生成，
 * 不能使用服务内存计数器，避免服务重启后与数据库已有业务主键冲突。</p>
 *
 * <p>迁移期采用双写：旧表 auth_accounts、auth_credentials、refresh_tokens 的既有读写语义保持不变，
 * 同时把身份绑定和会话事实写入 auth_identity_bindings、auth_sessions。刷新链路保持原有
 * 「不轮换 Refresh Token」语义，因此不改动会话事实。双写异常不会被吞掉，注册、登录和退出登录
 * 都通过 ServiceImpl 上的本地事务回滚半成品写入，保证迁移期强一致。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Refresh Token 会话业务主键前缀，必须与 V4 回填脚本的 'sess-' || rt.token_id 保持一致。
     */
    private static final String SESSION_ID_PREFIX = "sess-";

    private final AuthBusinessIdGenerator businessIdGenerator;
    private final AuthAccountRepository accountRepository;
    private final AuthCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthIdentityBindingRepository identityBindingRepository;
    private final AuthSessionRepository sessionRepository;
    private final LoginLogRepository loginLogRepository;
    private final Pbkdf2PasswordHashService passwordHashService;
    private final HmacTokenIssuer tokenIssuer;
    private final UserProfileProvisionClient userProfileProvisionClient;
    private final AccessTokenDenylistClient accessTokenDenylistClient;

    /**
     * 创建认证业务服务实现，并默认使用空实现的 Access Token 拒绝名单客户端。
     *
     * @param businessIdGenerator 认证业务主键生成器。
     * @param accountRepository 认证账号仓储端口。
     * @param credentialRepository 认证凭证仓储端口。
     * @param refreshTokenRepository Refresh Token 仓储端口。
     * @param identityBindingRepository 登录身份绑定仓储端口。
     * @param sessionRepository 认证会话仓储端口。
     * @param loginLogRepository 登录日志仓储端口。
     * @param passwordHashService 密码哈希服务。
     * @param tokenIssuer Token 签发和解析服务。
     * @param userProfileProvisionClient 用户资料开通客户端。
     */
    public AuthServiceImpl(
            AuthBusinessIdGenerator businessIdGenerator,
            AuthAccountRepository accountRepository,
            AuthCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthIdentityBindingRepository identityBindingRepository,
            AuthSessionRepository sessionRepository,
            LoginLogRepository loginLogRepository,
            Pbkdf2PasswordHashService passwordHashService,
            HmacTokenIssuer tokenIssuer,
            UserProfileProvisionClient userProfileProvisionClient
    ) {
        this(
                businessIdGenerator,
                accountRepository,
                credentialRepository,
                refreshTokenRepository,
                identityBindingRepository,
                sessionRepository,
                loginLogRepository,
                passwordHashService,
                tokenIssuer,
                userProfileProvisionClient,
                new NoopAccessTokenDenylistClient()
        );
    }

    /**
     * 创建认证业务服务实现，并注入 Access Token 拒绝名单客户端。
     *
     * @param businessIdGenerator 认证业务主键生成器。
     * @param accountRepository 认证账号仓储端口。
     * @param credentialRepository 认证凭证仓储端口。
     * @param refreshTokenRepository Refresh Token 仓储端口。
     * @param identityBindingRepository 登录身份绑定仓储端口。
     * @param sessionRepository 认证会话仓储端口。
     * @param loginLogRepository 登录日志仓储端口。
     * @param passwordHashService 密码哈希服务。
     * @param tokenIssuer Token 签发和解析服务。
     * @param userProfileProvisionClient 用户资料开通客户端。
     * @param accessTokenDenylistClient Access Token 拒绝名单客户端。
     */
    @Autowired
    public AuthServiceImpl(
            AuthBusinessIdGenerator businessIdGenerator,
            AuthAccountRepository accountRepository,
            AuthCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthIdentityBindingRepository identityBindingRepository,
            AuthSessionRepository sessionRepository,
            LoginLogRepository loginLogRepository,
            Pbkdf2PasswordHashService passwordHashService,
            HmacTokenIssuer tokenIssuer,
            UserProfileProvisionClient userProfileProvisionClient,
            AccessTokenDenylistClient accessTokenDenylistClient
    ) {
        this.businessIdGenerator = businessIdGenerator;
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.identityBindingRepository = identityBindingRepository;
        this.sessionRepository = sessionRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordHashService = passwordHashService;
        this.tokenIssuer = tokenIssuer;
        this.userProfileProvisionClient = userProfileProvisionClient;
        this.accessTokenDenylistClient = accessTokenDenylistClient;
    }

    /**
     * 注册登录身份并保存密码哈希，返回标准用户业务主键。
     *
     * <p>除旧表 auth_accounts、auth_credentials 外，同步写入账号名、手机号和邮箱三类
     * auth_identity_bindings，保证新表与 V4 回填语义一致。</p>
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

        saveRegistrationIdentityBindings(userId, request, now);
        userProfileProvisionClient.provision(accountId, userId, request);
        return new RegisterResponse(null, userId, account.getAccountStatus());
    }

    /**
     * 使用账号名、手机号或邮箱登录，成功后签发 Access Token 和 Refresh Token。
     *
     * <p>除旧表 refresh_tokens 外，同步写入一条 auth_sessions 会话事实，session_id 采用
     * {@code sess-{refreshTokenId}}，与 V4 回填规则保持一致。</p>
     *
     * <p>登录失败必须保留 login_logs 审计记录，因此业务异常不触发回滚；数据库和未预期异常仍整体回滚，
     * 避免会话双写只成功一半。</p>
     *
     * @param request 登录请求，包含登录标识和明文密码。
     * @return 登录成功后的 Token、过期时间和用户业务主键。
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = FoodMapException.class)
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
        RefreshTokenEntity refreshTokenEntity = saveRefreshToken(
                account.getAccountId(), refreshToken, refreshExpiresTime, now);
        saveSession(account.getUserId(), refreshTokenEntity, account.getRegisteredChannel(), now);
        account.setLastLoginTime(now);
        account.setUpdatedTime(now);
        accountRepository.save(account);
        writeLoginLog(account.getAccountId(), request.loginIdentifier(), LoginResult.SUCCESS);
        return new LoginResponse(null, account.getUserId(), accessToken, refreshToken, accessExpiresTime, refreshExpiresTime);
    }

    /**
     * 使用有效 Refresh Token 刷新 Access Token。MVP 阶段不轮换 Refresh Token，便于前端联调和问题排查。
     *
     * <p>因为不轮换，auth_sessions 中的会话事实无需改动：同一 refresh_token_hash 持续有效，
     * 会话保持 ACTIVE，避免出现与旧表不一致的会话状态。</p>
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
    @Transactional(rollbackFor = Exception.class)
    public void logout(LogoutRequest request) {
        doLogout(request, null);
    }

    /**
     * 退出登录并撤销 Refresh Token；请求携带 Bearer Access Token 时同步写入 Redis 拒绝名单。
     *
     * <p>两个分支的语义必须区分清楚：</p>
     * <ul>
     *   <li>携带 Authorization：Token 必须是可解析、类型正确且未过期的 Access Token，否则抛出 401，
     *       不能静默返回成功；校验通过后按 Access Token 原过期时间写入拒绝名单，实现「登出即失效」。</li>
     *   <li>不携带 Authorization：仅撤销 Refresh Token 和对应会话，不写拒绝名单；旧 Access Token 在自然过期前
     *       仍然可用，这是兼容旧调用方的已知边界，客户端登出时应始终携带 Access Token。</li>
     * </ul>
     *
     * <p>除旧表 refresh_tokens 外，同步把 refresh_token_hash 对应的 auth_sessions 会话置为 REVOKED，
     * 并记录 revoked_time，保证新旧会话事实一致。</p>
     *
     * @param request 退出登录请求，包含待撤销的 Refresh Token。
     * @param authorization HTTP Authorization 请求头，允许为空以兼容旧调用方。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(LogoutRequest request, String authorization) {
        doLogout(request, authorization);
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

    /**
     * 执行退出登录：撤销旧表 Refresh Token、撤销新表会话，并按需把 Access Token 写入拒绝名单。
     *
     * <p>携带 Authorization 时必须先校验其格式、Access Token 与有效期，再进行任何会话写操作；
     * 无效认证返回 401，即使使用不支持事务的测试替身也不能留下半撤销状态。
     * 完全不携带 Authorization 时跳过拒绝名单写入，只完成 Refresh Token 撤销。</p>
     *
     * @param request 退出登录请求，包含待撤销的 Refresh Token。
     * @param authorization HTTP Authorization 请求头，允许为空。
     */
    private void doLogout(LogoutRequest request, String authorization) {
        OffsetDateTime now = OffsetDateTime.now();
        tokenIssuer.parseRefreshToken(request.refreshToken());
        String accessToken = extractBearerToken(authorization);
        TokenClaims accessClaims = accessToken == null ? null : tokenIssuer.parseAccessToken(accessToken);
        if (accessClaims != null) {
            ensureTokenNotExpired(accessClaims, now, "Access Token已过期");
        }
        String refreshTokenHash = tokenIssuer.tokenHash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .ifPresent(entity -> {
                    if (TokenStatus.ACTIVE.name().equals(entity.getTokenStatus())) {
                        refreshTokenRepository.revoke(entity, now);
                    }
                });
        revokeSessionByRefreshTokenHash(refreshTokenHash, now);
        if (accessClaims != null) {
            accessTokenDenylistClient.deny(tokenIssuer.tokenHash(accessToken), accessClaims.expiresTime());
        }
    }

    /**
     * 写入注册产生的 LOCAL 身份绑定：账号名必写，手机号和邮箱仅在请求携带非空白值时写入。
     *
     * @param userId 用户业务主键。
     * @param request 注册请求，提供原始登录标识用于脱敏展示和哈希计算。
     * @param now 当前业务时间，同时作为创建时间和绑定时间。
     */
    private void saveRegistrationIdentityBindings(Long userId, RegisterRequest request, OffsetDateTime now) {
        String provider = IdentityProvider.LOCAL.name();
        saveIdentityBindingIfAbsent(
                userId,
                provider,
                IdentityType.ACCOUNT_NAME,
                sha256Hex(normalizeLower(request.accountName())),
                request.accountName(),
                (short) 1,
                now
        );
        if (hasText(request.phone())) {
            saveIdentityBindingIfAbsent(
                    userId,
                    provider,
                    IdentityType.PHONE,
                    sha256Hex(request.phone().trim()),
                    maskPhone(request.phone()),
                    (short) 0,
                    now
            );
        }
        if (hasText(request.email())) {
            saveIdentityBindingIfAbsent(
                    userId,
                    provider,
                    IdentityType.EMAIL,
                    sha256Hex(normalizeLower(request.email())),
                    maskEmail(request.email()),
                    (short) 0,
                    now
            );
        }
    }

    /**
     * 在身份绑定不存在时写入一条 LOCAL 绑定，避免与 auth_identity_bindings 的非删除唯一索引冲突。
     *
     * @param userId 用户业务主键。
     * @param provider 身份提供方。
     * @param identityType 身份类型。
     * @param identityHash 标准化登录标识的 SHA-256 小写十六进制哈希。
     * @param displayMasked 脱敏展示值，账号名保留原始值。
     * @param primaryIdentity 是否主登录身份，1-主身份，0-普通身份。
     * @param now 当前业务时间，同时作为创建时间和绑定时间。
     */
    private void saveIdentityBindingIfAbsent(
            Long userId,
            String provider,
            IdentityType identityType,
            String identityHash,
            String displayMasked,
            short primaryIdentity,
            OffsetDateTime now
    ) {
        if (identityBindingRepository.findByProviderAndIdentityTypeAndIdentityHash(
                provider, identityType.name(), identityHash).isPresent()) {
            return;
        }
        AuthIdentityBindingEntity entity = new AuthIdentityBindingEntity();
        entity.setIdentityId(businessIdGenerator.nextIdentityId());
        entity.setUserId(userId);
        entity.setProvider(provider);
        entity.setIdentityType(identityType.name());
        entity.setIdentityHash(identityHash);
        entity.setDisplayMasked(displayMasked);
        entity.setVerified((short) 1);
        entity.setPrimaryIdentity(primaryIdentity);
        entity.setIdentityStatus(IdentityStatus.NORMAL.name());
        entity.setBoundTime(now);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        identityBindingRepository.save(entity);
    }

    /**
     * 为新签发的 Refresh Token 写入 ACTIVE 会话；会话 ID 或 Token 哈希已存在时跳过，避免唯一索引冲突。
     *
     * @param userId 用户业务主键。
     * @param refreshTokenEntity 新签发的 Refresh Token 元数据。
     * @param registeredChannel 账号注册渠道，用作设备类型。
     * @param now 当前业务时间，作为会话创建时间。
     */
    private void saveSession(
            Long userId,
            RefreshTokenEntity refreshTokenEntity,
            String registeredChannel,
            OffsetDateTime now
    ) {
        String sessionId = SESSION_ID_PREFIX + refreshTokenEntity.getTokenId();
        String refreshTokenHash = refreshTokenEntity.getTokenHash();
        if (sessionRepository.findBySessionId(sessionId).isPresent()
                || sessionRepository.findByRefreshTokenHash(refreshTokenHash).isPresent()) {
            return;
        }
        AuthSessionEntity session = new AuthSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setDeviceType(resolveDeviceType(registeredChannel));
        session.setExpiresTime(refreshTokenEntity.getExpiresTime());
        session.setSessionStatus(AuthSessionStatus.ACTIVE.name());
        session.setCreatedTime(now);
        session.setUpdatedTime(now);
        session.setIsDelete((short) 0);
        sessionRepository.save(session);
    }

    /**
     * 按 Refresh Token 哈希撤销仍处于 ACTIVE 的会话；已是 REVOKED 或不存在时保持幂等。
     *
     * @param refreshTokenHash Refresh Token 的哈希值。
     * @param now 当前业务时间，作为会话撤销时间。
     */
    private void revokeSessionByRefreshTokenHash(String refreshTokenHash, OffsetDateTime now) {
        sessionRepository.findByRefreshTokenHash(refreshTokenHash).ifPresent(session -> {
            if (AuthSessionStatus.ACTIVE.name().equals(session.getSessionStatus())) {
                sessionRepository.revoke(session, now);
            }
        });
    }

    /**
     * 计算登录标识的 SHA-256 小写十六进制摘要，与 PostgreSQL encode(sha256(...), 'hex') 保持一致。
     *
     * @param normalizedIdentity 已按身份类型完成标准化处理的登录标识，不能传入明文用户输入的原值。
     * @return 小写十六进制哈希字符串。
     */
    private String sha256Hex(String normalizedIdentity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedIdentity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new FoodMapException(CommonErrorCode.INTERNAL_ERROR, "登录标识哈希服务不可用");
        }
    }

    /**
     * 按 V4 回填规则对账号名和邮箱做标准化：去首尾空白并转小写，避免同一标识产生不同哈希。
     *
     * @param value 原始登录标识。
     * @return 标准化后的登录标识。
     */
    private String normalizeLower(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 按 V4 回填规则脱敏手机号：长度不足 7 位时统一返回 ***，否则保留前 3 位和后 4 位。
     *
     * @param phone 原始手机号。
     * @return 脱敏后的手机号展示值。
     */
    private String maskPhone(String phone) {
        String trimmed = phone.trim();
        if (trimmed.length() < 7) {
            return "***";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 按 V4 回填规则脱敏邮箱：保留首字母和域名，@ 前没有有效字符时统一返回 ***。
     *
     * @param email 原始邮箱。
     * @return 脱敏后的邮箱展示值。
     */
    private String maskEmail(String email) {
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return trimmed.substring(0, 1) + "***@" + trimmed.substring(atIndex + 1);
    }

    /**
     * 解析设备类型，账号注册渠道为空时回退到 IOS。
     *
     * @param registeredChannel 账号注册渠道。
     * @return 写入 auth_sessions.device_type 的设备类型。
     */
    private String resolveDeviceType(String registeredChannel) {
        return hasText(registeredChannel) ? registeredChannel : RegisteredChannel.IOS.name();
    }

    /**
     * 判断字符串是否包含非空白字符，用于决定手机号和邮箱身份绑定是否写入。
     *
     * @param value 待判断字符串。
     * @return 包含非空白字符时返回 true。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 在注册前检查账号名、手机号和邮箱是否已被旧表账号占用，避免注册成功后无法登录。
     *
     * @param accountName 账号名登录标识。
     * @param phone 手机号登录标识，允许为空。
     * @param email 邮箱登录标识，允许为空。
     */
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

    /**
     * 保存 Refresh Token 元数据并返回带业务主键的实体，便于后续生成会话 ID。
     *
     * @param accountId 账号业务主键。
     * @param refreshToken Refresh Token 明文，只在方法内计算哈希。
     * @param expiresTime Refresh Token 过期时间。
     * @param now 当前业务时间。
     * @return 已写入仓储的 Refresh Token 实体。
     */
    private RefreshTokenEntity saveRefreshToken(
            Long accountId,
            String refreshToken,
            OffsetDateTime expiresTime,
            OffsetDateTime now
    ) {
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
        return entity;
    }

    /**
     * 校验 Refresh Token 旧表记录仍可刷新，已撤销和已过期都必须拒绝续签。
     *
     * @param entity 落库的 Refresh Token 实体。
     * @param now 当前业务时间。
     */
    private void ensureRefreshTokenUsable(RefreshTokenEntity entity, OffsetDateTime now) {
        if (!TokenStatus.ACTIVE.name().equals(entity.getTokenStatus())) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token已失效");
        }
        if (!entity.getExpiresTime().isAfter(now)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Refresh Token已过期");
        }
    }

    /**
     * 提取登出请求的 Bearer Token，仅完全缺少请求头时走兼容分支。
     *
     * @param authorization HTTP Authorization 请求头；已提供但格式错误时返回 401。
     * @return Bearer Token 明文；请求头不存在时返回 null。
     */
    private String extractBearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "Authorization 格式无效");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, "缺少Access Token");
        }
        return token;
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

    /**
     * 校验 Token 未过期，过期时按未认证语义抛出业务异常。
     *
     * @param claims Token 声明。
     * @param now 当前业务时间。
     * @param message 过期时返回给调用方的错误提示。
     */
    private void ensureTokenNotExpired(TokenClaims claims, OffsetDateTime now, String message) {
        if (claims.isExpiredAt(now)) {
            throw new FoodMapException(CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    /**
     * 写入登录日志，记录登录标识类型和成功/失败结果，供安全排查使用。
     *
     * @param accountId 账号业务主键，未知账号时允许为空。
     * @param loginIdentifier 登录标识，用于推断登录类型。
     * @param result 登录结果。
     */
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

    /**
     * 根据登录标识形态推断登录类型，优先识别邮箱，其次识别纯数字手机号。
     *
     * @param loginIdentifier 登录标识。
     * @return 用于登录日志分类的登录类型。
     */
    private LoginType resolveLoginType(String loginIdentifier) {
        if (loginIdentifier != null && loginIdentifier.contains("@")) {
            return LoginType.EMAIL;
        }
        if (loginIdentifier != null && loginIdentifier.chars().allMatch(Character::isDigit)) {
            return LoginType.PHONE;
        }
        return LoginType.ACCOUNT_NAME;
    }

    /**
     * 解析注册渠道，未传或空白时回退到 iOS 默认渠道。
     *
     * @param value 注册请求中的渠道文本。
     * @return 注册渠道枚举。
     */
    private RegisteredChannel resolveRegisteredChannel(String value) {
        if (value == null || value.isBlank()) {
            return RegisteredChannel.IOS;
        }
        return RegisteredChannel.valueOf(value);
    }

    /**
     * 把空白文本统一转换为 null，避免旧表出现无意义的空字符串登录标识。
     *
     * @param value 原始文本。
     * @return 非空白文本或 null。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
