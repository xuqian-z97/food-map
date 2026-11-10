package com.foodmap.admin.service;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 后台服务测试业务主键生成器，用于让后台管理员创建用例保持可重复。
 */
public class TestAdminBusinessIdGenerator implements AdminBusinessIdGenerator {
    private final AtomicLong adminUserId = new AtomicLong(9000);
    private final AtomicLong otherIds = new AtomicLong(500000);

    /**
     * 生成后台管理员测试业务主键。
     *
     * @return 后台管理员测试业务主键。
     */
    @Override
    public long nextAdminUserId() {
        return adminUserId.incrementAndGet();
    }

    /**
     * 生成后台角色测试业务主键。
     *
     * @return 后台角色测试业务主键。
     */
    @Override
    public long nextAdminRoleId() {
        return otherIds.incrementAndGet();
    }

    /**
     * 生成后台权限测试业务主键。
     *
     * @return 后台权限测试业务主键。
     */
    @Override
    public long nextAdminPermissionId() {
        return otherIds.incrementAndGet();
    }

    /**
     * 生成后台管理员角色关联测试业务主键。
     *
     * @return 后台管理员角色关联测试业务主键。
     */
    @Override
    public long nextAdminUserRoleRelationId() {
        return otherIds.incrementAndGet();
    }

    /**
     * 生成后台角色权限关联测试业务主键。
     *
     * @return 后台角色权限关联测试业务主键。
     */
    @Override
    public long nextAdminRolePermissionRelationId() {
        return otherIds.incrementAndGet();
    }

    /**
     * 生成后台会话测试业务主键。
     *
     * @return 后台会话测试业务主键。
     */
    @Override
    public long nextAdminAccessSessionId() {
        return otherIds.incrementAndGet();
    }

    /**
     * 生成后台操作测试业务主键。
     *
     * @return 后台操作测试业务主键。
     */
    @Override
    public long nextAdminOperationId() {
        return otherIds.incrementAndGet();
    }
}
