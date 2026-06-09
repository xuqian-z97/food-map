package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `refresh_tokens` 标准 MyBatis Mapper，只承载刷新令牌表的单表模板 SQL。
 */
@Mapper
public interface RefreshTokenMapper {

    /**
     * 按数据库内部主键查询 Refresh Token 元数据。
     */
    RefreshTokenEntity selectById(@Param("id") Long id);

    /**
     * 按 Token 业务主键查询 Refresh Token 元数据。
     */
    RefreshTokenEntity selectByBizId(@Param("tokenId") Long tokenId);

    /**
     * 按有限条件查询 Refresh Token 列表。
     */
    List<RefreshTokenEntity> selectListByCondition(@Param("condition") RefreshTokenEntity condition);

    /**
     * 按有限条件分页查询 Refresh Token 列表。
     */
    List<RefreshTokenEntity> selectPageByCondition(
            @Param("condition") RefreshTokenEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增 Refresh Token 元数据。
     */
    int insertOne(RefreshTokenEntity entity);

    /**
     * 批量新增 Refresh Token 元数据。
     */
    int insertBatch(@Param("items") List<RefreshTokenEntity> items);

    /**
     * 按数据库内部主键更新 Refresh Token 元数据。
     */
    int updateById(RefreshTokenEntity entity);

    /**
     * 按 Token 业务主键更新 Refresh Token 元数据。
     */
    int updateByBizId(RefreshTokenEntity entity);

    /**
     * 按 Token 业务主键批量更新 Refresh Token 元数据。
     */
    int updateBatchByBizId(@Param("items") List<RefreshTokenEntity> items);

    /**
     * 按数据库内部主键逻辑删除 Refresh Token。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按 Token 业务主键批量逻辑删除 Refresh Token。
     */
    int logicDeleteByBizIds(@Param("tokenIds") List<Long> tokenIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
