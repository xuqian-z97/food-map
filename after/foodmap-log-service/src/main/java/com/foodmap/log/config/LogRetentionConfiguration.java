package com.foodmap.log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 日志保留策略配置，提供清理任务所需的统一时间源并启用 Spring 调度能力。
 */
@Configuration
@EnableScheduling
public class LogRetentionConfiguration {

    /**
     * 提供 UTC 系统时钟，便于清理服务按同一时间源计算保留边界。
     *
     * @return UTC 系统时钟。
     */
    @Bean
    public Clock foodmapLogClock() {
        return Clock.systemUTC();
    }
}
