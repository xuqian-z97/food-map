package com.foodmap.common.redis;

import com.foodmap.common.exception.CommonErrorCode;
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
}
