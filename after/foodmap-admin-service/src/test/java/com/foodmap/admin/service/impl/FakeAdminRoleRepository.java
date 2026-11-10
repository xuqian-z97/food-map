package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminRoleRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 后台角色仓储内存替身，用于登录和受控初始化用例的可重复测试。
 */
public class FakeAdminRoleRepository implements AdminRoleRepository {
    private final Map<String, Long> roleIdsByCode = new HashMap<>();
    private final Map<Long, List<String>> roleCodesByAdminUserId = new HashMap<>();
    private final List<String> grantedRelations = new ArrayList<>();

    /**
     * 预置有效角色编码。
     *
     * @param roleCode 角色编码。
     * @param roleId 角色业务主键。
     */
    public void putRole(String roleCode, Long roleId) {
        roleIdsByCode.put(roleCode, roleId);
    }

    /**
     * 预置管理员角色授权。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param roleCodes 角色编码列表。
     */
    public void putAdminRoles(Long adminUserId, List<String> roleCodes) {
        roleCodesByAdminUserId.put(adminUserId, new ArrayList<>(roleCodes));
    }

    /**
     * 读取已执行的授权关系记录，用于断言幂等授权行为。
     *
     * @return 授权关系描述列表。
     */
    public List<String> grantedRelations() {
        return grantedRelations;
    }

    @Override
    public Optional<Long> findActiveRoleIdByCode(String roleCode) {
        return Optional.ofNullable(roleIdsByCode.get(roleCode));
    }

    @Override
    public List<String> findActiveRoleCodes(Long adminUserId) {
        return roleCodesByAdminUserId.getOrDefault(adminUserId, List.of());
    }

    @Override
    public boolean grantRole(Long relationId, Long adminUserId, Long roleId, OffsetDateTime grantedTime) {
        String record = adminUserId + ":" + roleId;
        if (grantedRelations.contains(record)) {
            return false;
        }
        grantedRelations.add(record);
        return true;
    }
}
