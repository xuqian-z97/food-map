package com.foodmap.admin.infrastructure.persistence.mybatis;

import com.foodmap.admin.application.port.AdminAccessSessionRepository;
import com.foodmap.admin.application.port.AdminAuthorizationSnapshot;
import com.foodmap.admin.infrastructure.persistence.entity.AdminAccessSessionEntity;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminAccessSessionDefineMapper;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminAccessSessionMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;

/**
 * 后台 Token 会话登记仓储实现，把会话端口转换为会话表的 Mapper 调用并组装授权快照。
 */
@Repository
public class AdminAccessSessionRepositoryImpl implements AdminAccessSessionRepository {
    private final AdminAccessSessionMapper adminAccessSessionMapper;
    private final AdminAccessSessionDefineMapper adminAccessSessionDefineMapper;

    public AdminAccessSessionRepositoryImpl(
            AdminAccessSessionMapper adminAccessSessionMapper,
            AdminAccessSessionDefineMapper adminAccessSessionDefineMapper
    ) {
        this.adminAccessSessionMapper = adminAccessSessionMapper;
        this.adminAccessSessionDefineMapper = adminAccessSessionDefineMapper;
    }

    /**
     * 登记新的后台 Token 会话。
     *
     * @param entity 后台 Token 会话登记实体。
     */
    @Override
    public void register(AdminAccessSessionEntity entity) {
        adminAccessSessionMapper.insertOne(entity);
    }

    /**
     * 按 Token 唯一标识和管理员业务主键撤销未撤销会话。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @param adminUserId 后台管理员业务主键。
     * @param revokedTime 会话撤销时间。
     * @return 首次撤销返回 true，会话不存在、归属不符或已撤销返回 false。
     */
    @Override
    public boolean revokeActive(String tokenId, Long adminUserId, OffsetDateTime revokedTime) {
        return adminAccessSessionDefineMapper.revokeActiveByTokenId(tokenId, adminUserId, revokedTime, revokedTime) > 0;
    }

    /**
     * 加载后台认证授权快照，会话与管理员联合行缺失时返回空 Optional。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @return 认证授权快照，会话或管理员未命中返回空 Optional。
     */
    @Override
    public Optional<AdminAuthorizationSnapshot> loadAuthorizationSnapshot(String tokenId) {
        return Optional.ofNullable(adminAccessSessionDefineMapper.selectSessionWithAdmin(tokenId))
                .map(row -> new AdminAuthorizationSnapshot(
                        row.getAdminUserId(),
                        row.getAdminStatus(),
                        row.getPermissionVersion(),
                        row.getExpiresTime(),
                        row.getRevokedTime(),
                        new HashSet<>(adminAccessSessionDefineMapper.selectActivePermissionCodes(row.getAdminUserId()))
                ));
    }
}
