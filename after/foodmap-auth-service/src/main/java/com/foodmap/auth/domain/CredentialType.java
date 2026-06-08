package com.foodmap.auth.domain;

/**
 * 登录凭证类型，区分密码、验证码和后续第三方登录等认证方式。
 */
public enum CredentialType {

    /**
     * 密码凭证，MVP 阶段唯一落地的凭证类型，必须保存强哈希而不是明文。
     */
    PASSWORD
}
