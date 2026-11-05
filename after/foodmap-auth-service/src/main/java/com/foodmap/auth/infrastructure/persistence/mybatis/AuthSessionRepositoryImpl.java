package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.AuthSessionRepository;
import com.foodmap.auth.domain.AuthSessionStatus;
import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.AuthSessionMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 认证会话仓储实现，负责在 MyBatis 适配层保存 Refresh Token 哈希和会话状态。
 */
@Repository
public class AuthSessionRepositoryImpl implements AuthSessionRepository {
    private final AuthSessionMapper authSessionMapper;

    public AuthSessionRepositoryImpl(AuthSessionMapper authSessionMapper) {
        this.authSessionMapper = authSessionMapper;
    }

    /**
     * 单条新增认证会话。
     *
     * @param entity 待保存的会话实体。
     */
    @Override
    public void save(AuthSessionEntity entity) {
        authSessionMapper.insertOne(entity);
    }

    /**
     * 按会话业务主键查询认证会话。
     *
     * @param sessionId 会话 ID，格式为 sess-{refreshTokenId}。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthSessionEntity> findBySessionId(String sessionId) {
        return Optional.ofNullable(authSessionMapper.selectByBizId(sessionId));
    }

    /**
     * 按 Refresh Token 哈希查询认证会话。
     *
     * @param refreshTokenHash Refresh Token 的哈希值。
     * @return 查询到的会话，未命中时返回空 Optional。
     */
    @Override
    public Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash) {
        AuthSessionEntity condition = new AuthSessionEntity();
        condition.setRefreshTokenHash(refreshTokenHash);
        List<AuthSessionEntity> items = authSessionMapper.selectPageByCondition(condition, 1, 0);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    /**
     * 按用户业务主键查询全部有效会话。
     *
     * @param userId 用户业务主键。
     * @return 该用户的会话列表，没有会话时返回空列表。
     */
    @Override
    public List<AuthSessionEntity> findByUserId(Long userId) {
        AuthSessionEntity condition = new AuthSessionEntity();
        condition.setUserId(userId);
        return authSessionMapper.selectListByCondition(condition);
    }

    /**
     * 撤销会话并记录撤销时间，便于排查退出登录和 Refresh Token 轮换。
     *
     * @param entity 待撤销的会话实体。
     * @param revokedTime 会话撤销时间。
     */
    @Override
    public void revoke(AuthSessionEntity entity, OffsetDateTime revokedTime) {
        entity.setSessionStatus(AuthSessionStatus.REVOKED.name());
        entity.setRevokedTime(revokedTime);
        entity.setUpdatedTime(revokedTime);
        authSessionMapper.updateByBizId(entity);
    }
}
