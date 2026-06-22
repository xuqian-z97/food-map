package com.foodmap.user.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 用户服务业务主键 Mapper，集中访问 Flyway 管理的业务主键 sequence。
 */
@Mapper
public interface UserBusinessIdMapper {

    /**
     * 获取下一个用户资料业务主键。
     *
     * @return 用户资料业务主键。
     */
    Long nextProfileId();

    /**
     * 获取下一个用户设置业务主键。
     *
     * @return 用户设置业务主键。
     */
    Long nextSettingId();
}
