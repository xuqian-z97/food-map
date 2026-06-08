package com.foodmap.user.application.port;

import com.foodmap.user.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

/**
 * 用户仓储端口，应用层通过该接口读取用户资料，避免依赖具体 JDBC 或内存实现。
 */
public interface UserRepository {

    /**
     * 保存用户主表实体。实现方必须使用用户业务主键作为服务内事实标识。
     */
    void save(UserEntity entity);

    /**
     * 根据用户业务主键查询用户主表实体。
     */
    Optional<UserEntity> findByUserId(Long userId);
}
