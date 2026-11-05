package com.foodmap.auth.domain;

/**
 * 登录身份提供方，用于区分同一用户在不同平台上的登录标识。
 *
 * <p>当前双写阶段只写入 {@link #LOCAL}，微信和 Apple 登录属于后续迭代预留。
 * 排查身份绑定漂移时，需要同时核对 provider、identity_type 和 identity_hash，
 * 避免把第三方主体误写成 LOCAL 登录标识。</p>
 */
public enum IdentityProvider {

    /**
     * 本地账号体系，覆盖账号名、手机号和邮箱三类登录标识。
     */
    LOCAL,

    /**
     * 微信开放平台，后续使用 openid/unionid 作为第三方主体。
     */
    WECHAT,

    /**
     * Apple 登录，后续使用 Apple sub 作为第三方主体。
     */
    APPLE
}
