package com.foodmap.auth.infrastructure.client.user;

/**
 * 用户服务资料开通 Feign 请求载荷。
 *
 * @param accountId 认证账号业务主键。
 * @param userId 用户业务主键。
 * @param nickname 注册时填写的初始昵称。
 * @return 用户服务资料开通 Feign 请求载荷。
 */
public record UserProvisionFeignRequest(Long accountId, Long userId, String nickname) {
}
