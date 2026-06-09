package com.foodmap.user.infrastructure.persistence.mybatis;

import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 MyBatis 仓储实现，负责把用户仓储端口转换为用户主表 Mapper 调用。
 */
@Repository
public class MyBatisUserRepository implements UserRepository {
    private final UserMapper userMapper;

    public MyBatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 保存用户，先按用户业务主键更新，未命中时新增。
     */
    @Override
    public void save(UserEntity entity) {
        int updated = userMapper.updateByBizId(entity);
        if (updated == 0) {
            userMapper.insertOne(entity);
        }
    }

    /**
     * 根据用户业务主键读取用户主表实体。
     */
    @Override
    public Optional<UserEntity> findByUserId(Long userId) {
        return Optional.ofNullable(userMapper.selectByBizId(userId));
    }
}
