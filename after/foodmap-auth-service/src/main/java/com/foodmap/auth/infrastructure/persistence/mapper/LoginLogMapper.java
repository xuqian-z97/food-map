package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.LoginLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `login_logs` 标准 MyBatis Mapper，只承载登录日志表的单表模板 SQL。
 */
@Mapper
public interface LoginLogMapper {

    /**
     * 按数据库内部主键查询登录日志。
     */
    LoginLogEntity selectById(@Param("id") Long id);

    /**
     * 按登录日志业务主键查询登录日志。
     */
    LoginLogEntity selectByBizId(@Param("loginLogId") Long loginLogId);

    /**
     * 按有限条件查询登录日志列表。
     */
    List<LoginLogEntity> selectListByCondition(@Param("condition") LoginLogEntity condition);

    /**
     * 按有限条件分页查询登录日志列表。
     */
    List<LoginLogEntity> selectPageByCondition(
            @Param("condition") LoginLogEntity condition,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    /**
     * 单条新增登录日志。
     */
    int insertOne(LoginLogEntity entity);

    /**
     * 批量新增登录日志。
     */
    int insertBatch(@Param("items") List<LoginLogEntity> items);

    /**
     * 按数据库内部主键更新登录日志。
     */
    int updateById(LoginLogEntity entity);

    /**
     * 按登录日志业务主键更新登录日志。
     */
    int updateByBizId(LoginLogEntity entity);

    /**
     * 按登录日志业务主键批量更新登录日志。
     */
    int updateBatchByBizId(@Param("items") List<LoginLogEntity> items);

    /**
     * 按数据库内部主键逻辑删除登录日志。
     */
    int logicDeleteById(@Param("id") Long id, @Param("updatedTime") OffsetDateTime updatedTime);

    /**
     * 按登录日志业务主键批量逻辑删除登录日志。
     */
    int logicDeleteByBizIds(@Param("loginLogIds") List<Long> loginLogIds, @Param("updatedTime") OffsetDateTime updatedTime);
}
