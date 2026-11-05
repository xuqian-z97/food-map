package com.foodmap.auth.domain;

/**
 * 认证会话状态，描述 Refresh Token 对应的长期会话事实。
 *
 * <p>会话状态必须与 auth_sessions.session_status 保持一致，排查强制下线或刷新失败时，
 * 应先确认会话是主动撤销、自然过期还是仍处于有效状态。</p>
 */
public enum AuthSessionStatus {

    /**
     * 会话有效，可以继续刷新 Access Token。
     */
    ACTIVE,

    /**
     * 会话已撤销，通常由退出登录、Refresh Token 轮换或安全风控触发。
     */
    REVOKED,

    /**
     * 会话已过期，刷新有效期已到但尚未被显式撤销。
     */
    EXPIRED
}
