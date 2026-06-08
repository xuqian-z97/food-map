package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;

/**
 * 登录日志仓储端口，负责写入认证安全审计记录。
 */
public interface LoginLogRepository {

    /**
     * 保存登录日志。日志中不得包含密码、Token、密钥或完整敏感联系方式。
     */
    void save(LoginLogEntity entity);
}
