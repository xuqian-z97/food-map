package com.foodmap.admin.application.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 后台角色仓储端口，隔离应用层与角色授权关系的具体持久化技术。
 */
public interface AdminRoleRepository {

    /**
     * 按角色编码查询有效角色业务主键。
     *
     * @param roleCode 后台角色编码。
     * @return 有效角色业务主键，未命中返回空 Optional。
     */
    Optional<Long> findActiveRoleIdByCode(String roleCode);

    /**
     * 查询管理员当前有效角色编码列表，只统计有效且未逻辑删除的授权关系。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 有效角色编码列表，无角色时返回空列表。
     */
    List<String> findActiveRoleCodes(Long adminUserId);

    /**
     * 幂等授予管理员角色，已存在有效授权时跳过写入。
     *
     * @param relationId 管理员角色关联业务主键，由调用方通过业务主键生成器获取。
     * @param adminUserId 后台管理员业务主键。
     * @param roleId 后台角色业务主键。
     * @param grantedTime 授权时间。
     * @return 实际新增时返回 true，已存在有效授权时返回 false。
     */
    boolean grantRole(Long relationId, Long adminUserId, Long roleId, OffsetDateTime grantedTime);
}
