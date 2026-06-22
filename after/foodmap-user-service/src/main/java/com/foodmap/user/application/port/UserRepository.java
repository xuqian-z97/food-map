package com.foodmap.user.application.port;

import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserProfileEntity;
import com.foodmap.user.infrastructure.persistence.entity.UserSettingEntity;

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
     * 开通用户资料，原子保存用户主表、用户资料表和用户设置表。
     *
     * @param user 用户主表实体。
     * @param profile 用户资料实体。
     * @param setting 用户设置实体。
     */
    void provision(UserEntity user, UserProfileEntity profile, UserSettingEntity setting);

    /**
     * 根据用户业务主键查询用户主表实体。
     */
    Optional<UserEntity> findByUserId(Long userId);

    /**
     * 根据用户业务主键查询用户资料实体。
     *
     * @param userId 用户业务主键。
     * @return 用户资料实体。
     */
    Optional<UserProfileEntity> findProfileByUserId(Long userId);

    /**
     * 根据用户业务主键查询用户设置实体。
     *
     * @param userId 用户业务主键。
     * @return 用户设置实体。
     */
    Optional<UserSettingEntity> findSettingByUserId(Long userId);
}
