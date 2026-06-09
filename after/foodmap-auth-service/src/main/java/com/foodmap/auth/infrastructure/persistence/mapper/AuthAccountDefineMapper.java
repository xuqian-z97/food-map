package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * `auth_accounts` 自定义 MyBatis Mapper，承载账号登录标识等业务查询 SQL。
 */
@Mapper
public interface AuthAccountDefineMapper {

    /**
     * 根据账号名、手机号或邮箱查询账号，供统一登录入口和注册唯一性校验使用。
     */
    AuthAccountEntity findByLoginIdentifier(
            @Param("normalized") String normalized,
            @Param("original") String original
    );
}
