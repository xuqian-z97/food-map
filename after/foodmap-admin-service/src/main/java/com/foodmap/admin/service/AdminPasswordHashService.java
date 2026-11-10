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

    /**
     * 校验原始密码与存量密码哈希是否匹配，格式异常按校验失败处理。
     *
     * @param rawPassword 原始密码。
     * @param storedHash 存量密码哈希，格式为 `算法$迭代次数$盐$摘要`。
     * @return 匹配时返回 true。
     */
    boolean matches(String rawPassword, String storedHash);
}
