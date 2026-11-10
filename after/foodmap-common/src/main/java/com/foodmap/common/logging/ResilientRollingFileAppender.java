package com.foodmap.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 失败安全的滚动文件日志追加器，用于在日志目录不可写时降级为仅控制台输出而不阻断服务启动。
 *
 * <p>本地未挂载 {@code /foodmap/logs} 卷时，原生 {@link RollingFileAppender} 会在 {@code start()}
 * 阶段向 logback 状态管理器写入 ERROR 级状态并抛出异常；Spring Boot 在加载 logback 配置后会检查
 * ERROR 状态并主动终止启动（{@code Logback configuration error detected}）。因此仅在
 * {@code start()} 里 try-catch 是不够的：必须**在调用父类之前**判断目录可写，避免产生 ERROR 状态。</p>
 *
 * <p>本类先尝试创建并校验日志目录：
 * <ul>
 *   <li>目录可写：走原生滚动文件行为，容器环境与生产行为不变；</li>
 *   <li>目录不可写：跳过文件 appender，只留一条 WARN，控制台 appender 继续工作，服务正常启动。</li>
 * </ul></p>
 *
 * <p>排查日志文件缺失时，先看启动日志中的 WARN，再确认 {@code foodmap.logging.dir}
 * （或 {@code FOODMAP_LOGGING_DIR}）指向的目录是否可写。</p>
 */
public class ResilientRollingFileAppender extends RollingFileAppender<ILoggingEvent> {

    /**
     * 启动文件 appender，日志目录不可写时降级为仅控制台输出。
     *
     * <p>目录不可写时直接返回，不调用父类实现：父类会向状态管理器写入 ERROR 级状态，
     * 而 Spring Boot 见到 ERROR 状态就会终止启动，那样「降级」仍然变成「启动失败」。</p>
     */
    @Override
    public void start() {
        if (!ensureParentDirectoryWritable()) {
            addWarn("日志目录不可写，已降级为仅控制台输出：" + getFile()
                    + "；可用 foodmap.logging.dir 或 FOODMAP_LOGGING_DIR 指定可写目录");
            return;
        }
        try {
            super.start();
        } catch (Throwable ex) {
            // 兜底：目录检查通过但打开文件仍失败（例如路径被并发占用）时，同样只降级、不阻断启动。
            addWarn("文件日志初始化失败，已降级为仅控制台输出：" + getFile(), ex);
            try {
                stop();
            } catch (Throwable ignored) {
                // appender 未被标记为已启动，logback 会跳过它，无需进一步处理。
            }
        }
    }

    /**
     * 确保日志文件的父目录存在且可写。
     *
     * @return 目录可写返回 true；文件未配置、目录无法创建或不可写时返回 false
     */
    private boolean ensureParentDirectoryWritable() {
        String file = getFile();
        if (file == null || file.isBlank()) {
            return true;
        }
        try {
            Path parent = Paths.get(file).toAbsolutePath().getParent();
            if (parent == null) {
                return true;
            }
            Files.createDirectories(parent);
            return Files.isWritable(parent);
        } catch (Exception ex) {
            return false;
        }
    }
}
