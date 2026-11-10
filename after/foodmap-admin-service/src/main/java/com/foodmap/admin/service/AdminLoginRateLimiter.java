package com.foodmap.admin.service;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.FoodMapException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台登录失败限流器，按登录账号名在滑动时间窗内统计失败次数，超出阈值后临时拒绝登录。
 *
 * <p>限流身份取自请求体中的账号名，不信任任何客户端可伪造的来源头；当前为单实例内存实现，
 * 多实例部署时应升级为 Redis 统一封装实现。生产路由开放前还必须叠加 HTTPS 与后台入口访问限制。</p>
 */
@Component
public class AdminLoginRateLimiter {
    private final int maxFailures;
    private final Duration window;
    private final Map<String, Deque<Instant>> failuresByUsername = new ConcurrentHashMap<>();

    /**
     * 创建登录失败限流器。
     *
     * @param maxFailures 时间窗内允许的最大失败次数，小于等于 0 时按不限流处理。
     * @param window 失败统计滑动窗口。
     */
    public AdminLoginRateLimiter(
            @Value("${foodmap.admin.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${foodmap.admin.login-rate-limit.window:5m}") Duration window
    ) {
        this.maxFailures = maxFailures;
        this.window = window;
    }

    /**
     * 登录前检查账号是否已被限流。
     *
     * @param username 后台登录账号名。
     * @throws FoodMapException 时间窗内失败次数达到阈值时抛出 429。
     */
    public void check(String username) {
        if (maxFailures <= 0 || username == null || username.isBlank()) {
            return;
        }
        Deque<Instant> failures = failuresByUsername.get(username);
        if (failures == null) {
            return;
        }
        synchronized (failures) {
            pruneExpired(failures);
            if (failures.size() >= maxFailures) {
                throw new FoodMapException(CommonErrorCode.TOO_MANY_REQUESTS, "后台登录尝试过于频繁，请稍后再试");
            }
        }
    }

    /**
     * 登录失败后记录一次失败。
     *
     * @param username 后台登录账号名。
     */
    public void recordFailure(String username) {
        if (maxFailures <= 0 || username == null || username.isBlank()) {
            return;
        }
        Deque<Instant> failures = failuresByUsername.computeIfAbsent(username, key -> new ArrayDeque<>());
        synchronized (failures) {
            pruneExpired(failures);
            failures.addLast(Instant.now());
        }
    }

    /**
     * 登录成功后清空账号的失败记录。
     *
     * @param username 后台登录账号名。
     */
    public void reset(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        failuresByUsername.remove(username);
    }

    /**
     * 清理滑动窗口之外的过期失败记录。
     *
     * @param failures 失败时间队列。
     */
    private void pruneExpired(Deque<Instant> failures) {
        Instant threshold = Instant.now().minus(window);
        while (!failures.isEmpty() && failures.peekFirst().isBefore(threshold)) {
            failures.removeFirst();
        }
    }
}
