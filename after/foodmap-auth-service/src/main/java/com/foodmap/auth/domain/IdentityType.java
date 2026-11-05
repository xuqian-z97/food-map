package com.foodmap.auth.domain;

/**
 * 登录身份类型，决定登录标识的标准化方式和唯一索引维度。
 *
 * <p>身份哈希必须按类型使用不同标准化规则：账号名和邮箱先 trim 再转小写，手机号只 trim。
 * 如果标准化规则变化，历史 identity_hash 需要同步回填，否则登录定位会出现新旧哈希不一致。</p>
 */
public enum IdentityType {

    /**
     * 账号名登录身份，哈希前先执行 trim 并转小写。
     */
    ACCOUNT_NAME,

    /**
     * 手机号登录身份，哈希前只执行 trim，不转换大小写。
     */
    PHONE,

    /**
     * 邮箱登录身份，哈希前先执行 trim 并转小写。
     */
    EMAIL,

    /**
     * 微信 openid 登录身份，预留给后续微信登录。
     */
    WECHAT_OPENID,

    /**
     * 微信 unionid 登录身份，预留给后续微信跨应用登录。
     */
    WECHAT_UNIONID,

    /**
     * Apple sub 登录身份，预留给后续 Sign in with Apple。
     */
    APPLE_SUB
}
