package com.foodmap.admin.service;

/**
 * 后台管理员密码哈希服务，隐藏具体哈希算法并禁止业务代码保存明文密码。
 */
public interface AdminPasswordHashService {

    /**
     * 将后台管理员原始密码转换为可持久化密码哈希。
     *
     * @param rawPassword 原始密码。
     * @return 可持久化密码哈希。
     */
    String hash(String rawPassword);
}
