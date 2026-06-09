package com.foodmap.user.infrastructure.persistence.mapper;

import com.foodmap.user.infrastructure.persistence.entity.UserProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `user_profiles` 标准 MyBatis Mapper，只承载用户资料表的单表模板 SQL。
 */
@Mapper
public interface UserProfileMapper {

    /**
     * 按数据库内部主键查询用户资料。
     */
    UserProfileEntity selectById(@Param("id") Long id);

    /**
     * 按资料业务主键查询用户资料。
     */
    UserProfileEntity selectByBizId(@Param("profileId") Long profileId);

    /**
     * 按有限条件查询用户资料列表。
     */
    List<UserProfileEntity> selectListByCondition(@Param("condition") UserProfileEntity condition);

    /**
     * 按有限条件分页查询用户资料列表。
     */
    List<UserProfileEntity> selectPageByCondition(
            @Param("condition") UserProfileEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增用户资料。
     */
    int insertOne(UserProfileEntity entity);

    /**
     * 批量新增用户资料。
     */
    int insertBatch(@Param("items") List<UserProfileEntity> items);

    /**
     * 按数据库内部主键更新用户资料。
     */
    int updateById(UserProfileEntity entity);

    /**
     * 按资料业务主键更新用户资料。
     */
    int updateByBizId(UserProfileEntity entity);

    /**
     * 按资料业务主键批量更新用户资料。
     */
    int updateBatchByBizId(@Param("items") List<UserProfileEntity> items);

    /**
     * 按数据库内部主键逻辑删除用户资料。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按资料业务主键批量逻辑删除用户资料。
     */
    int logicDeleteByBizIds(@Param("profileIds") List<Long> profileIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
