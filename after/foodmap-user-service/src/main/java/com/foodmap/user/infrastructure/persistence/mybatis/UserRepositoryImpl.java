package com.foodmap.user.infrastructure.persistence.mybatis;

import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserProfileEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserSettingEntity;
import com.foodmap.user.infrastructure.persistence.mapper.UserMapper;
import com.foodmap.user.infrastructure.persistence.mapper.UserProfileMapper;
import com.foodmap.user.infrastructure.persistence.mapper.UserSettingMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现，负责在 MyBatis 适配层把用户仓储端口转换为用户主表 Mapper 调用。
 */
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserSettingMapper userSettingMapper;

    public UserRepositoryImpl(
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            UserSettingMapper userSettingMapper
    ) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userSettingMapper = userSettingMapper;
    }

    /**
     * 保存用户，先按用户业务主键更新，未命中时新增。
     *
     * @param entity 待保存的用户主表实体。
     */
    @Override
    public void save(UserEntity entity) {
        int updated = userMapper.updateByBizId(entity);
        if (updated == 0) {
            userMapper.insertOne(entity);
        }
    }

    /**
     * 开通用户资料，写入用户主表、资料表和设置表。
     *
     * @param user 用户主表实体。
     * @param profile 用户资料实体。
     * @param setting 用户设置实体。
     */
    @Override
    public void provision(UserEntity user, UserProfileEntity profile, UserSettingEntity setting) {
        userMapper.insertOne(user);
        userProfileMapper.insertOne(profile);
        userSettingMapper.insertOne(setting);
    }

    /**
     * 根据用户业务主键读取用户主表实体。
     *
     * @param userId 用户业务主键。
     * @return 查询到的用户主表实体，未命中时返回空 Optional。
     */
    @Override
    public Optional<UserEntity> findByUserId(Long userId) {
        return Optional.ofNullable(userMapper.selectByBizId(userId));
    }

    /**
     * 根据用户业务主键读取用户资料实体。
     *
     * @param userId 用户业务主键。
     * @return 用户资料实体。
     */
    @Override
    public Optional<UserProfileEntity> findProfileByUserId(Long userId) {
        UserProfileEntity condition = new UserProfileEntity();
        condition.setUserId(userId);
        return userProfileMapper.selectListByCondition(condition).stream().findFirst();
    }

    /**
     * 根据用户业务主键读取用户设置实体。
     *
     * @param userId 用户业务主键。
     * @return 用户设置实体。
     */
    @Override
    public Optional<UserSettingEntity> findSettingByUserId(Long userId) {
        UserSettingEntity condition = new UserSettingEntity();
        condition.setUserId(userId);
        return userSettingMapper.selectListByCondition(condition).stream().findFirst();
    }
}
