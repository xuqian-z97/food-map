package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `auth_accounts` 标准 MyBatis Mapper，只承载认证账号表的单表模板 SQL。
 */
@Mapper
public interface AuthAccountMapper {

    /**
     * 按数据库内部主键查询认证账号，排查数据问题时使用，不对外暴露 id。
     */
    AuthAccountEntity selectById(@Param("id") Long id);

    /**
     * 按账号业务主键查询认证账号，是服务内主要定位方式。
     */
    AuthAccountEntity selectByBizId(@Param("accountId") Long accountId);

    /**
     * 按有限条件查询认证账号列表，默认过滤逻辑删除数据。
     */
    List<AuthAccountEntity> selectListByCondition(@Param("condition") AuthAccountEntity condition);

    /**
     * 按有限条件分页查询认证账号列表，排序由 XML 固定控制。
     */
    List<AuthAccountEntity> selectPageByCondition(
            @Param("condition") AuthAccountEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增认证账号。
     */
    int insertOne(AuthAccountEntity entity);

    /**
     * 批量新增认证账号，调用方必须保证业务主键已生成。
     */
    int insertBatch(@Param("items") List<AuthAccountEntity> items);

    /**
     * 按数据库内部主键更新认证账号。
     */
    int updateById(AuthAccountEntity entity);

    /**
     * 按账号业务主键更新认证账号。
     */
    int updateByBizId(AuthAccountEntity entity);

    /**
     * 按账号业务主键批量更新认证账号。
     */
    int updateBatchByBizId(@Param("items") List<AuthAccountEntity> items);

    /**
     * 按数据库内部主键逻辑删除认证账号。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按账号业务主键批量逻辑删除认证账号。
     */
    int logicDeleteByBizIds(@Param("accountIds") List<Long> accountIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
