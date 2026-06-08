package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;

import java.util.Optional;

/**
 * 认证账号仓储端口，应用层通过该接口访问账号数据，避免依赖具体 JDBC 或内存实现。
 */
public interface AuthAccountRepository {

    /**
     * 保存账号实体。实现方需要支持新增账号和登录后更新时间等更新场景。
     */
    void save(AuthAccountEntity entity);

    /**
     * 根据账号业务主键查询账号，跨服务和应用层都不能使用数据库自增主键定位账号。
     */
    Optional<AuthAccountEntity> findByAccountId(Long accountId);

    /**
     * 根据账号名、手机号或邮箱查询账号，用于统一登录入口。
     */
    Optional<AuthAccountEntity> findByLoginIdentifier(String loginIdentifier);
}
