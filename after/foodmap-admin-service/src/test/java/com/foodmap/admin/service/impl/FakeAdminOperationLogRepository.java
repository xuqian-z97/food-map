package com.foodmap.admin.service.impl;

import com.foodmap.admin.application.port.AdminOperationLogRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminOperationLogEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台操作记录仓储内存替身，用于断言审计行为。
 */
class FakeAdminOperationLogRepository implements AdminOperationLogRepository {
    private final List<AdminOperationLogEntity> appended = new ArrayList<>();

    /**
     * 读取已追加的审计记录。
     *
     * @return 审计记录列表。
     */
    List<AdminOperationLogEntity> appended() {
        return appended;
    }

    @Override
    public void append(AdminOperationLogEntity entity) {
        appended.add(entity);
    }
}
