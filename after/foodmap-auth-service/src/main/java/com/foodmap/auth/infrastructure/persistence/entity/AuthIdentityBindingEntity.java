package com.foodmap.auth.infrastructure.persistence.entity;

import com.foodmap.common.persistence.BaseEntity;

import java.time.OffsetDateTime;

/**
 * 认证身份绑定持久化实体，对应 `auth_identity_bindings` 表。
 *
 * <p>该表不是账号主体表，只回答“某个登录标识属于哪个 userId”。字段 Javadoc 必须与 V4 迁移脚本中的
 * `comment on column` 保持一致，便于数据库结构文档、Flyway 脚本和 Java 实体三方核对。</p>
 */
public class AuthIdentityBindingEntity extends BaseEntity {

    /**
     * 登录身份绑定业务主键，用于跨服务引用身份绑定记录。
     */
    private Long identityId;

    /**
     * 用户业务主键，关联用户服务的 users.user_id。
     */
    private Long userId;

    /**
     * 身份提供方，如 LOCAL、WECHAT、APPLE。
     */
    private String provider;

    /**
     * 身份类型，如 ACCOUNT_NAME、PHONE、EMAIL、WECHAT_OPENID、APPLE_SUB。
     */
    private String identityType;

    /**
     * 标准化登录标识哈希，用于唯一索引和检索，禁止用明文直接建索引。
     */
    private String identityHash;

    /**
     * 加密后的登录标识，可选保存，禁止明文敏感标识散落到日志。
     */
    private String identityCipher;

    /**
     * 脱敏展示值，如 186****1693 或 f***@example.com。
     */
    private String displayMasked;

    /**
     * 第三方平台主体 ID，如微信 openid 或 Apple sub，保存前需评估加密和脱敏策略。
     */
    private String providerSubject;

    /**
     * 第三方跨应用主体 ID，如微信 unionid，保存前需评估加密和脱敏策略。
     */
    private String unionSubject;

    /**
     * 是否已验证，1-已验证，0-未验证。
     */
    private Short verified;

    /**
     * 是否主登录身份，1-主身份，0-普通身份。
     */
    private Short primaryIdentity;

    /**
     * 登录身份状态，如 NORMAL、DISABLED、UNBOUND。
     */
    private String identityStatus;

    /**
     * 绑定时间，回填历史账号时取账号创建时间。
     */
    private OffsetDateTime boundTime;

    public Long getIdentityId() {
        return identityId;
    }

    public void setIdentityId(Long identityId) {
        this.identityId = identityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getIdentityHash() {
        return identityHash;
    }

    public void setIdentityHash(String identityHash) {
        this.identityHash = identityHash;
    }

    public String getIdentityCipher() {
        return identityCipher;
    }

    public void setIdentityCipher(String identityCipher) {
        this.identityCipher = identityCipher;
    }

    public String getDisplayMasked() {
        return displayMasked;
    }

    public void setDisplayMasked(String displayMasked) {
        this.displayMasked = displayMasked;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public void setProviderSubject(String providerSubject) {
        this.providerSubject = providerSubject;
    }

    public String getUnionSubject() {
        return unionSubject;
    }

    public void setUnionSubject(String unionSubject) {
        this.unionSubject = unionSubject;
    }

    public Short getVerified() {
        return verified;
    }

    public void setVerified(Short verified) {
        this.verified = verified;
    }

    public Short getPrimaryIdentity() {
        return primaryIdentity;
    }

    public void setPrimaryIdentity(Short primaryIdentity) {
        this.primaryIdentity = primaryIdentity;
    }

    public String getIdentityStatus() {
        return identityStatus;
    }

    public void setIdentityStatus(String identityStatus) {
        this.identityStatus = identityStatus;
    }

    public OffsetDateTime getBoundTime() {
        return boundTime;
    }

    public void setBoundTime(OffsetDateTime boundTime) {
        this.boundTime = boundTime;
    }
}
