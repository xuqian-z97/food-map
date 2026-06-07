package com.foodmap.common.service;

import java.time.Instant;

/**
 * 服务健康信息响应模型，主要用于内部 health 接口和网关联通性验证。
 *
 * <p>排查服务注册或网关路由问题时，可通过 serviceName 和 checkedAt 判断响应来自哪个实例时间点。</p>
 */
public record ServiceInfo(
        String serviceName,
        String status,
        Instant checkedAt
) {
    /**
     * 创建健康状态响应，当前只表达服务进程存活，不代表所有依赖组件都健康。
     */
    public static ServiceInfo up(String serviceName) {
        return new ServiceInfo(serviceName, "UP", Instant.now());
    }
}
