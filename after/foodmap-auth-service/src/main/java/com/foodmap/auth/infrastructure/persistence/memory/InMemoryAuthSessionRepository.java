package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.application.port.AuthSessionRepository;
import com.foodmap.auth.domain.AuthSessionStatus;
import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证会话内存仓储，用于单元测试验证登录、刷新和退出登录的会话状态流转。
 *
 * <p>生产环境必须使用 MyBatis 仓储实现；内存实现只保留会话实体引用，测试可以直接观察字段变化。</p>
 */
public class InMemoryAuthSessionRepository implements AuthSessionRepository {
    private final Map<String, AuthSessionEntity> sessionsBySessionId = new ConcurrentHashMap<>();

    /**
     * 保存认证会话实体，调用方负责生成会话 ID 和 Refresh Token 哈希。
     *
     * @param entity 待保存的会话实体。
     */
    @Override
    public void save(AuthSessionEntity entity) {
        sessionsBySessionId.put(entity.getSessionId(), entity);
    }

    /**
     * 按会话业务主键查找认证会话。
     *
     * @param sessionId 会话 ID，格式为 sess-{refreshTokenId}。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthSessionEntity> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessionsBySessionId.get(sessionId));
    }

    /**
     * 按 Refresh Token 哈希查找认证会话，测试用于确认刷新和退出登录命中了正确会话。
     *
     * @param refreshTokenHash Refresh Token 的哈希值。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash) {
        return sessionsBySessionId.values().stream()
                .filter(session -> refreshTokenHash.equals(session.getRefreshTokenHash()))
                .findFirst();
    }

    /**
     * 按用户业务主键查找全部会话，测试用于区分历史会话和刷新后的新会话。
     *
     * @param userId 用户业务主键。
     * @return 该用户的会话列表，没有会话时返回空列表。
     */
    @Override
    public List<AuthSessionEntity> findByUserId(Long userId) {
        return sessionsBySessionId.values().stream()
                .filter(session -> userId.equals(session.getUserId()))
                .toList();
    }

    /**
     * 撤销内存会话，测试中可直接观察 sessionStatus 和 revokedTime 的变化。
     *
     * @param entity 待撤销的会话实体。
     * @param revokedTime 会话撤销时间。
     */
    @Override
    public void revoke(AuthSessionEntity entity, OffsetDateTime revokedTime) {
        entity.setSessionStatus(AuthSessionStatus.REVOKED.name());
        entity.setRevokedTime(revokedTime);
        entity.setUpdatedTime(revokedTime);
    }
}
