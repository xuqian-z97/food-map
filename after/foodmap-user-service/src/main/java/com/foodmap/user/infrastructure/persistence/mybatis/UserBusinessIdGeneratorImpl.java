package com.foodmap.user.infrastructure.persistence.mybatis;

import com.foodmap.user.application.port.UserBusinessIdGenerator;
import com.foodmap.user.infrastructure.persistence.mapper.UserBusinessIdMapper;
import org.springframework.stereotype.Component;

/**
 * 用户服务业务主键生成器实现，通过数据库 sequence 生成用户资料和设置业务主键。
 */
@Component
public class UserBusinessIdGeneratorImpl implements UserBusinessIdGenerator {
    private final UserBusinessIdMapper userBusinessIdMapper;

    /**
     * 创建用户服务业务主键生成器。
     *
     * @param userBusinessIdMapper 业务主键 Mapper。
     */
    public UserBusinessIdGeneratorImpl(UserBusinessIdMapper userBusinessIdMapper) {
        this.userBusinessIdMapper = userBusinessIdMapper;
    }

    /**
     * 生成用户资料业务主键。
     *
     * @return 用户资料业务主键。
     */
    @Override
    public Long nextProfileId() {
        return userBusinessIdMapper.nextProfileId();
    }

    /**
     * 生成用户设置业务主键。
     *
     * @return 用户设置业务主键。
     */
    @Override
    public Long nextSettingId() {
        return userBusinessIdMapper.nextSettingId();
    }
}
