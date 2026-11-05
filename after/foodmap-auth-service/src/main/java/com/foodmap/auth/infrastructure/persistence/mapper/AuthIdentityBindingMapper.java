package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthIdentityBindingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `auth_identity_bindings` 标准 MyBatis Mapper，只承载身份绑定表的单表模板 SQL。
 *
 * <p>登录标识查找必须使用 provider + identity_type + identity_hash 三元组，禁止按明文登录标识查询。
 * 复杂统计或跨表 SQL 不允许写入本 Mapper，应新增对应的 DefineMapper。</p>
 */
@Mapper
public interface AuthIdentityBindingMapper {

    /**
     * 按数据库内部主键查询登录身份绑定。
     *
     * @param id 数据库内部自增主键。
     * @return 未命中的身份绑定返回 null。
     */
    AuthIdentityBindingEntity selectById(@Param("id") Long id);

    /**
     * 按身份绑定业务主键查询登录身份绑定。
     *
     * @param identityId 登录身份绑定业务主键。
     * @return 未命中的身份绑定返回 null。
     */
    AuthIdentityBindingEntity selectByBizId(@Param("identityId") Long identityId);

    /**
     * 按有限条件查询登录身份绑定列表。
     *
     * @param condition 单表查询条件，空值字段不参与过滤。
     * @return 符合条件的身份绑定列表。
     */
    List<AuthIdentityBindingEntity> selectListByCondition(@Param("condition") AuthIdentityBindingEntity condition);

    /**
     * 按有限条件分页查询登录身份绑定列表。
     *
     * @param condition 单表查询条件，空值字段不参与过滤。
     * @param limit 单页最大记录数。
     * @param offset 分页偏移量。
     * @return 当前页身份绑定列表。
     */
    List<AuthIdentityBindingEntity> selectPageByCondition(
            @Param("condition") AuthIdentityBindingEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增登录身份绑定。
     *
     * @param entity 待新增的身份绑定实体。
     * @return 受影响行数。
     */
    int insertOne(AuthIdentityBindingEntity entity);

    /**
     * 批量新增登录身份绑定。
     *
     * @param items 待新增的身份绑定实体列表。
     * @return 受影响行数。
     */
    int insertBatch(@Param("items") List<AuthIdentityBindingEntity> items);

    /**
     * 按数据库内部主键更新登录身份绑定。
     *
     * @param entity 待更新的身份绑定实体。
     * @return 受影响行数。
     */
    int updateById(AuthIdentityBindingEntity entity);

    /**
     * 按身份绑定业务主键更新登录身份绑定。
     *
     * @param entity 待更新的身份绑定实体。
     * @return 受影响行数。
     */
    int updateByBizId(AuthIdentityBindingEntity entity);

    /**
     * 按身份绑定业务主键批量更新登录身份绑定。
     *
     * @param items 待更新的身份绑定实体列表。
     * @return 受影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AuthIdentityBindingEntity> items);

    /**
     * 按数据库内部主键逻辑删除登录身份绑定。
     *
     * @param id 数据库内部自增主键。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按身份绑定业务主键批量逻辑删除登录身份绑定。
     *
     * @param identityIds 登录身份绑定业务主键列表。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int logicDeleteByBizIds(@Param("identityIds") List<Long> identityIds, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按数据库内部主键逻辑删除登录身份绑定，模板方法名与 deleteById 约定对齐，实际执行 is_delete = 1。
     *
     * @param id 数据库内部自增主键。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int deleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按业务主键批量逻辑删除登录身份绑定，模板方法名与 deleteBatchByIds 约定对齐，实际执行 is_delete = 1。
     *
     * @param identityIds 登录身份绑定业务主键列表。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int deleteBatchByIds(@Param("identityIds") List<Long> identityIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
