package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `auth_credentials` 标准 MyBatis Mapper，只承载认证凭证表的单表模板 SQL。
 */
@Mapper
public interface AuthCredentialMapper {

    /**
     * 按数据库内部主键查询认证凭证。
     */
    AuthCredentialEntity selectById(@Param("id") Long id);

    /**
     * 按凭证业务主键查询认证凭证。
     */
    AuthCredentialEntity selectByBizId(@Param("credentialId") Long credentialId);

    /**
     * 按有限条件查询认证凭证列表。
     */
    List<AuthCredentialEntity> selectListByCondition(@Param("condition") AuthCredentialEntity condition);

    /**
     * 按有限条件分页查询认证凭证列表。
     */
    List<AuthCredentialEntity> selectPageByCondition(
            @Param("condition") AuthCredentialEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增认证凭证。
     */
    int insertOne(AuthCredentialEntity entity);

    /**
     * 批量新增认证凭证。
     */
    int insertBatch(@Param("items") List<AuthCredentialEntity> items);

    /**
     * 按数据库内部主键更新认证凭证。
     */
    int updateById(AuthCredentialEntity entity);

    /**
     * 按凭证业务主键更新认证凭证。
     */
    int updateByBizId(AuthCredentialEntity entity);

    /**
     * 按凭证业务主键批量更新认证凭证。
     */
    int updateBatchByBizId(@Param("items") List<AuthCredentialEntity> items);

    /**
     * 按数据库内部主键逻辑删除认证凭证。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按凭证业务主键批量逻辑删除认证凭证。
     */
    int logicDeleteByBizIds(@Param("credentialIds") List<Long> credentialIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
