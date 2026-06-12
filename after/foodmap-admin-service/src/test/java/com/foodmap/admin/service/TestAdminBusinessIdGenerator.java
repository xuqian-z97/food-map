package com.foodmap.admin.service;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 后台服务测试业务主键生成器，用于让后台管理员创建用例保持可重复。
 */
class TestAdminBusinessIdGenerator implements AdminBusinessIdGenerator {
    private final AtomicLong adminUserId = new AtomicLong(9000);

    /**
     * 生成后台管理员测试业务主键。
     *
     * @return 后台管理员业务主键。
     */
    @Override
    public long nextAdminUserId() {
        return adminUserId.incrementAndGet();
    }
}
