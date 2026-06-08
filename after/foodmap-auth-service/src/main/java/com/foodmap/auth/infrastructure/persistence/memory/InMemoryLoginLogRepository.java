package com.foodmap.auth.infrastructure.persistence.memory;

import com.foodmap.auth.application.port.LoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 登录日志内存仓储，用于 B1 阶段保留安全审计写入位置。
 */
public class InMemoryLoginLogRepository implements LoginLogRepository {
    private final List<LoginLogEntity> logs = Collections.synchronizedList(new ArrayList<>());

    /**
     * 保存登录日志。日志字段不得包含密码、Token 或完整敏感联系方式。
     */
    @Override
    public void save(LoginLogEntity entity) {
        logs.add(entity);
    }
}
