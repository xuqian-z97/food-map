package com.foodmap.auth.infrastructure.persistence.mapper;

import com.foodmap.auth.infrastructure.persistence.entity.AuthCredentialEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * `auth_credentials` 自定义 MyBatis Mapper，承载账号凭证组合查询和更新 SQL。
 */
@Mapper
public interface AuthCredentialDefineMapper {

    /**
     * 根据账号业务主键和凭证类型查询凭证，用于登录密码校验。
     */
    AuthCredentialEntity findByAccountIdAndCredentialType(
            @Param("accountId") Long accountId,
            @Param("credentialType") String credentialType
    );

    /**
     * 根据账号业务主键和凭证类型更新凭证，用于保持账号下同类型凭证唯一。
     */
    int updateByAccountIdAndCredentialType(AuthCredentialEntity entity);
}
