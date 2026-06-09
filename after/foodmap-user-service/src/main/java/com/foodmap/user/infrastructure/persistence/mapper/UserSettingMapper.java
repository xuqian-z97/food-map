package com.foodmap.user.infrastructure.persistence.mapper;

import com.foodmap.user.infrastructure.persistence.entity.UserSettingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `user_settings` 标准 MyBatis Mapper，只承载用户设置表的单表模板 SQL。
 */
@Mapper
public interface UserSettingMapper {

    /**
     * 按数据库内部主键查询用户设置。
     */
    UserSettingEntity selectById(@Param("id") Long id);

    /**
     * 按设置业务主键查询用户设置。
     */
    UserSettingEntity selectByBizId(@Param("settingId") Long settingId);

    /**
     * 按有限条件查询用户设置列表。
     */
    List<UserSettingEntity> selectListByCondition(@Param("condition") UserSettingEntity condition);

    /**
     * 按有限条件分页查询用户设置列表。
     */
    List<UserSettingEntity> selectPageByCondition(
            @Param("condition") UserSettingEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增用户设置。
     */
    int insertOne(UserSettingEntity entity);

    /**
     * 批量新增用户设置。
     */
    int insertBatch(@Param("items") List<UserSettingEntity> items);

    /**
     * 按数据库内部主键更新用户设置。
     */
    int updateById(UserSettingEntity entity);

    /**
     * 按设置业务主键更新用户设置。
     */
    int updateByBizId(UserSettingEntity entity);

    /**
     * 按设置业务主键批量更新用户设置。
     */
    int updateBatchByBizId(@Param("items") List<UserSettingEntity> items);

    /**
     * 按数据库内部主键逻辑删除用户设置。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按设置业务主键批量逻辑删除用户设置。
     */
    int logicDeleteByBizIds(@Param("settingIds") List<Long> settingIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
