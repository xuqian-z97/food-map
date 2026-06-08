package com.foodmap.user.infrastructure.persistence.memory;

import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户内存仓储，用于 B1 阶段在 Mapper 未落地前验证用户资料读取用例。
 */
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, UserEntity> usersByUserId = new ConcurrentHashMap<>();

    /**
     * 保存用户主表实体。调用方必须传入用户业务主键。
     */
    @Override
    public void save(UserEntity entity) {
        usersByUserId.put(entity.getUserId(), entity);
    }

    /**
     * 根据用户业务主键查找用户资料。
     */
    @Override
    public Optional<UserEntity> findByUserId(Long userId) {
        return Optional.ofNullable(usersByUserId.get(userId));
    }
}
