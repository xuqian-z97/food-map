package com.foodmap.admin.infrastructure.persistence.mapper;

import com.foodmap.admin.infrastructure.persistence.mybatis.AdminSessionAdminRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * `admin_access_sessions` 自定义 MyBatis Mapper，承载后台认证授权快照查询和会话撤销 SQL。
 */
@Mapper
public interface AdminAccessSessionDefineMapper {

    /**
     * 按 Token 唯一标识查询会话与管理员联合行，供认证阶段校验未撤销、未过期和管理员状态。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @return 会话与管理员联合行，未命中返回 null。
     */
    AdminSessionAdminRow selectSessionWithAdmin(@Param("tokenId") String tokenId);

    /**
     * 查询管理员当前有效权限编码列表，只统计有效角色、有效权限且未逻辑删除的授权关系。
     *
     * @param adminUserId 后台管理员业务主键。
     * @return 有效权限编码列表，无权限时返回空列表。
     */
    List<String> selectActivePermissionCodes(@Param("adminUserId") Long adminUserId);

    /**
     * 按 Token 唯一标识和管理员业务主键撤销未撤销会话，只有首次撤销返回影响行数 1。
     *
     * @param tokenId 后台 Access Token 的 JWT jti 文本形式。
     * @param adminUserId 后台管理员业务主键。
     * @param revokedTime 会话撤销时间。
     * @param updatedTime 更新时间。
     * @return 首次撤销返回 1，会话不存在、归属不符或已撤销返回 0。
     */
    int revokeActiveByTokenId(
            @Param("tokenId") String tokenId,
            @Param("adminUserId") Long adminUserId,
            @Param("revokedTime") OffsetDateTime revokedTime,
            @Param("updatedTime") OffsetDateTime updatedTime
    );
}
