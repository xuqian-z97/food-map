package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `auth_sessions` 标准 MyBatis Mapper，只承载认证会话表的单表模板 SQL。
 *
 * <p>会话业务主键是 session_id 字符串，刷新和退出登录都通过 refresh_token_hash 定位会话。
 * 复杂会话统计或跨账号关联 SQL 不允许写入本 Mapper。</p>
 */
@Mapper
public interface AuthSessionMapper {

    /**
     * 按数据库内部主键查询认证会话。
     *
     * @param id 数据库内部自增主键。
     * @return 未命中的会话返回 null。
     */
    AuthSessionEntity selectById(@Param("id") Long id);

    /**
     * 按会话业务主键查询认证会话。
     *
     * @param sessionId 会话 ID。
     * @return 未命中的会话返回 null。
     */
    AuthSessionEntity selectByBizId(@Param("sessionId") String sessionId);

    /**
     * 按有限条件查询认证会话列表。
     *
     * @param condition 单表查询条件，空值字段不参与过滤。
     * @return 符合条件的会话列表。
     */
    List<AuthSessionEntity> selectListByCondition(@Param("condition") AuthSessionEntity condition);

    /**
     * 按有限条件分页查询认证会话列表。
     *
     * @param condition 单表查询条件，空值字段不参与过滤。
     * @param limit 单页最大记录数。
     * @param offset 分页偏移量。
     * @return 当前页会话列表。
     */
    List<AuthSessionEntity> selectPageByCondition(
            @Param("condition") AuthSessionEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增认证会话。
     *
     * @param entity 待新增的会话实体。
     * @return 受影响行数。
     */
    int insertOne(AuthSessionEntity entity);

    /**
     * 批量新增认证会话。
     *
     * @param items 待新增的会话实体列表。
     * @return 受影响行数。
     */
    int insertBatch(@Param("items") List<AuthSessionEntity> items);

    /**
     * 按数据库内部主键更新认证会话。
     *
     * @param entity 待更新的会话实体。
     * @return 受影响行数。
     */
    int updateById(AuthSessionEntity entity);

    /**
     * 按会话业务主键更新认证会话。
     *
     * @param entity 待更新的会话实体。
     * @return 受影响行数。
     */
    int updateByBizId(AuthSessionEntity entity);

    /**
     * 按会话业务主键批量更新认证会话。
     *
     * @param items 待更新的会话实体列表。
     * @return 受影响行数。
     */
    int updateBatchByBizId(@Param("items") List<AuthSessionEntity> items);

    /**
     * 按数据库内部主键逻辑删除认证会话。
     *
     * @param id 数据库内部自增主键。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按会话业务主键批量逻辑删除认证会话。
     *
     * @param sessionIds 会话 ID 列表。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int logicDeleteByBizIds(@Param("sessionIds") List<String> sessionIds, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按数据库内部主键逻辑删除认证会话，模板方法名与 deleteById 约定对齐，实际执行 is_delete = 1。
     *
     * @param id 数据库内部自增主键。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int deleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按业务主键批量逻辑删除认证会话，模板方法名与 deleteBatchByIds 约定对齐，实际执行 is_delete = 1。
     *
     * @param sessionIds 会话 ID 列表。
     * @param updatedTime 逻辑删除时间。
     * @return 受影响行数。
     */
    int deleteBatchByIds(@Param("sessionIds") List<String> sessionIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
