package com.foodmap.admin.infrastructure.persistence.mybatis;

import com.foodmap.admin.application.port.AdminRoleRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminRoleEntity;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminRoleMapper;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminUserRoleDefineMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 后台角色仓储实现，把角色授权端口转换为角色表和角色关联表的 Mapper 调用。
 */
@Repository
public class AdminRoleRepositoryImpl implements AdminRoleRepository {
    private final AdminRoleMapper adminRoleMapper;
    private final AdminUserRoleDefineMapper adminUserRoleDefineMapper;

    public AdminRoleRepositoryImpl(
            AdminRoleMapper adminRoleMapper,
            AdminUserRoleDefineMapper adminUserRoleDefineMapper
    ) {
        this.adminRoleMapper = adminRoleMapper;
        this.adminUserRoleDefineMapper = adminUserRoleDefineMapper;
    }

    /**
     * 按角色编码查询有效角色业务主键。
     *
     * @param roleCode 后台角色编码。
     * @return 有效角色业务主键，未命中返回空 Optional。
     */
    @Override
    public Optional<Long> findActiveRoleIdByCode(String roleCode) {
        AdminRoleEntity condition = new AdminRoleEntity();
        condition.setRoleCode(roleCode);
        condition.setRoleStatus("ACTIVE");
        return adminRoleMapper.selectListByCondition(condition).stream()
                .findFirst()
                .map(AdminRoleEntity::getRoleId);
    }

    /**
     * 查询管理员当前有效角色编码列表。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 有效角色编码列表，无角色时返回空列表。
     */
    @Override
    public List<String> findActiveRoleCodes(Long adminUserId) {
        return adminUserRoleDefineMapper.selectActiveRoleCodesByAdminUserId(adminUserId);
    }

    /**
     * 幂等授予管理员角色，已存在有效授权时跳过写入。
     *
     * @param relationId 管理员角色关联业务主键，由调用方通过业务主键生成器获取。
     * @param adminUserId 后台管理员业务主键。
     * @param roleId 后台角色业务主键。
     * @param grantedTime 授权时间。
     * @return 实际新增时返回 true，已存在有效授权时返回 false。
     */
    @Override
    public boolean grantRole(Long relationId, Long adminUserId, Long roleId, OffsetDateTime grantedTime) {
        return adminUserRoleDefineMapper.insertIgnoreDuplicate(relationId, adminUserId, roleId, grantedTime) > 0;
    }
}
