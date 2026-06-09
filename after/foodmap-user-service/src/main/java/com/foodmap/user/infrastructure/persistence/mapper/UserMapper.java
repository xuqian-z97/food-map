package com.foodmap.user.infrastructure.persistence.mapper;

import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `users` 标准 MyBatis Mapper，只承载用户主表的单表模板 SQL。
 */
@Mapper
public interface UserMapper {

    /**
     * 按数据库内部主键查询用户。
     */
    UserEntity selectById(@Param("id") Long id);

    /**
     * 按用户业务主键查询用户。
     */
    UserEntity selectByBizId(@Param("userId") Long userId);

    /**
     * 按有限条件查询用户列表。
     */
    List<UserEntity> selectListByCondition(@Param("condition") UserEntity condition);

    /**
     * 按有限条件分页查询用户列表。
     */
    List<UserEntity> selectPageByCondition(
            @Param("condition") UserEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增用户。
     */
    int insertOne(UserEntity entity);

    /**
     * 批量新增用户。
     */
    int insertBatch(@Param("items") List<UserEntity> items);

    /**
     * 按数据库内部主键更新用户。
     */
    int updateById(UserEntity entity);

    /**
     * 按用户业务主键更新用户。
     */
    int updateByBizId(UserEntity entity);

    /**
     * 按用户业务主键批量更新用户。
     */
    int updateBatchByBizId(@Param("items") List<UserEntity> items);

    /**
     * 按数据库内部主键逻辑删除用户。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按用户业务主键批量逻辑删除用户。
     */
    int logicDeleteByBizIds(@Param("userIds") List<Long> userIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
