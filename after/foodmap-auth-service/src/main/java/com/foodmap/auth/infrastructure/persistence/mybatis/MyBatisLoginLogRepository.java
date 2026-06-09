package com.foodmap.auth.infrastructure.persistence.mybatis;

import com.foodmap.auth.application.port.LoginLogRepository;
import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import com.foodmap.auth.infrastructure.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Repository;

/**
 * 登录日志 MyBatis 仓储实现，负责写入认证安全审计数据。
 */
@Repository
public class MyBatisLoginLogRepository implements LoginLogRepository {
    private final LoginLogMapper loginLogMapper;

    public MyBatisLoginLogRepository(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * 单条保存登录日志。
     */
    @Override
    public void save(LoginLogEntity entity) {
        loginLogMapper.insertOne(entity);
    }
}
