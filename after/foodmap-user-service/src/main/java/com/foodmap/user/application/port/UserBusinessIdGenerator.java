package com.foodmap.user.application.port;

/**
 * 用户服务业务主键生成器，封装用户资料和设置表的数据库 sequence 访问。
 */
public interface UserBusinessIdGenerator {

    /**
     * 生成用户资料业务主键。
     *
     * @return 用户资料业务主键。
     */
    Long nextProfileId();

    /**
     * 生成用户设置业务主键。
     *
     * @return 用户设置业务主键。
     */
    Long nextSettingId();
}
