package com.foodmap.common.redis;

import com.foodmap.common.exception.CommonErrorCode;
import com.foodmap.common.exception.ErrorCode;
import com.foodmap.common.exception.FoodMapException;

/**
 * 分布式锁异常，用于表达获取锁失败、释放锁失败或锁客户端不可用。
 *
 * <p>业务层捕获该异常时应按并发冲突或稍后重试处理，不能把 Redis 内部错误细节透传给前端。</p>
 */
public class DistributedLockException extends FoodMapException {

    /**
     * 使用可展示提示创建分布式锁异常，错误码统一归类为资源冲突。
     *
     * @param message 可展示错误提示。
     */
    public DistributedLockException(String message) {
        super(CommonErrorCode.CONFLICT, message);
    }

    /**
     * 使用指定错误码创建分布式锁异常，适合区分锁冲突和 Redis 依赖不可用。
     *
     * @param errorCode 稳定错误码。
     * @param message 可展示错误提示。
     */
    public DistributedLockException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 使用指定错误码和根因创建分布式锁异常，原始异常只用于日志排查。
     *
     * @param errorCode 稳定错误码。
     * @param message 可展示错误提示。
     * @param cause 原始异常根因。
     */
    public DistributedLockException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
