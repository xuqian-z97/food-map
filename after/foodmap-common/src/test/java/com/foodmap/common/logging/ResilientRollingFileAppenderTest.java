package com.foodmap.common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link ResilientRollingFileAppender} 启动行为的回归测试。
 *
 * <p>覆盖真实启动中暴露过的问题：日志目录不可写时，原生 {@code RollingFileAppender} 会把异常抛到
 * Joran 解析链路导致 Spring Boot 启动失败（本地未挂载 {@code /foodmap/logs} 卷即可复现）；
 * 本类必须保证此时只降级、不抛异常。</p>
 */
class ResilientRollingFileAppenderTest {

    /**
     * 日志目录不可创建时，启动必须不抛异常并保持未启动状态，交给 logback 跳过。
     *
     * @param tempDir 临时目录，用于构造「父路径是普通文件」的不可写场景
     * @throws IOException 创建测试夹具失败
     */
    @Test
    void shouldNotThrowWhenLogDirectoryIsNotCreatable(@TempDir Path tempDir) throws IOException {
        Path parentFile = tempDir.resolve("not-a-directory");
        Files.writeString(parentFile, "x");

        ResilientRollingFileAppender appender = newAppender(parentFile.resolve("logs/service.log").toString());

        assertThatCode(appender::start).doesNotThrowAnyException();
        assertThat(appender.isStarted()).isFalse();
    }

    /**
     * 目录可写时必须正常启动并真正创建日志目录，避免降级逻辑误伤正常路径。
     *
     * @param tempDir 临时目录
     */
    @Test
    void shouldStartAndCreateFileWhenDirectoryIsWritable(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("logs/service.log");

        ResilientRollingFileAppender appender = newAppender(logFile.toString());
        appender.start();

        assertThat(appender.isStarted()).isTrue();
        assertThat(Files.exists(logFile.getParent())).isTrue();
        appender.stop();
    }

    /**
     * 构建测试用 appender，绑定独立的 LoggerContext 避免污染全局日志配置。
     *
     * <p>必须显式设置滚动策略：{@code RollingFileAppender.start()} 在没有触发策略时会直接返回，
     * 那样「不可写」用例会假通过，测不到真实的打开文件失败路径。</p>
     *
     * @param file 日志文件路径
     * @return 已配置编码器与滚动策略的 appender
     */
    private ResilientRollingFileAppender newAppender(String file) {
        LoggerContext context = new LoggerContext();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n");
        encoder.start();

        ResilientRollingFileAppender appender = new ResilientRollingFileAppender();
        appender.setContext(context);
        appender.setName("TEST-FILE");
        appender.setEncoder(encoder);
        appender.setFile(file);

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setFileNamePattern(file + ".%d{yyyy-MM-dd}.gz");
        rollingPolicy.setMaxHistory(1);
        rollingPolicy.setParent(appender);

        // 只设置 rollingPolicy：它会自动兼任 triggeringPolicy，重复设置会让 logback 重置策略并拒绝启动。
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.start();
        return appender;
    }
}
