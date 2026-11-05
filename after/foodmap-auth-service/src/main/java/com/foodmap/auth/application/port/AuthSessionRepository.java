package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 认证会话仓储端口，应用层通过该接口维护 Refresh Token 对应的长期会话事实。
 *
 * <p>会话只保存 Refresh Token 哈希，不保存明文 Token。刷新和退出登录必须通过哈希定位会话，
 * 新增会话前必须先确认 sessionId 和 refreshTokenHash 尚未写入。</p>
 */
public interface AuthSessionRepository {

    /**
     * 单条新增认证会话。调用方必须保证会话 ID 和 Refresh Token 哈希尚未写入。
     *
     * @param entity 待保存的会话实体。
     */
    void save(AuthSessionEntity entity);

    /**
     * 按会话业务主键查询认证会话。
     *
     * @param sessionId 会话 ID，格式为 sess-{refreshTokenId}。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    Optional<AuthSessionEntity> findBySessionId(String sessionId);

    /**
     * 按 Refresh Token 哈希查询认证会话，刷新和退出登录链路使用该入口定位会话。
     *
     * @param refreshTokenHash Refresh Token 的哈希值。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    /**
     * 按用户业务主键查询全部有效会话，用于多端会话管理和双写核验。
     *
     * @param userId 用户业务主键。
     * @return 该用户的会话列表，没有会话时返回空列表。
     */
    List<AuthSessionEntity> findByUserId(Long userId);

    /**
     * 撤销会话并记录撤销时间，调用方必须传入已从仓储查询到的会话实体。
     *
     * @param entity 待撤销的会话实体。
     * @param revokedTime 会话撤销时间。
     */
    void revoke(AuthSessionEntity entity, OffsetDateTime revokedTime);
}
