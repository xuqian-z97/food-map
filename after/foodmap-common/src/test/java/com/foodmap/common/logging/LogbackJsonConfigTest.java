package com.foodmap.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 logback-common.xml 的 JSON 输出与降级行为，防止日志格式回退成无法被 Fluent Bit 解析的纯文本。
 */
class LogbackJsonConfigTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICE_CONFIG = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>
                <include resource="logback-common.xml"/>
            </configuration>
            """;

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteSingleLineJsonWithMdcFieldsToLogFile() throws Exception {
        String serviceName = "foodmap-t028-json-test";
        Path logDir = tempDir.resolve("logs");
        LoggerContext context = configure(logDir, serviceName);
        try {
            MDC.put(LogMdcKeys.REQUEST_ID, "req-t028");
            MDC.put(LogMdcKeys.TRACE_ID, "trace-t028");
            MDC.put(LogMdcKeys.SPAN_ID, "span-t028");
            MDC.put(LogMdcKeys.SERVICE_NAME, serviceName);
            MDC.put(LogMdcKeys.LOG_TYPE, LogMdcKeys.LOG_TYPE_APPLICATION);
            Logger logger = context.getLogger("com.foodmap.common.logging.LogbackJsonConfigTest");

            SafeLog.info(logger, LogMdcKeys.LOG_TYPE_API_ACCESS, "api.access.completed",
                    LogField.of("status", 200), LogField.of("phone", "13812345678"));

            MDC.clear();
            logger.info("framework style log without request context");
        } finally {
            context.stop();
            MDC.clear();
        }

        Path logFile = logDir.resolve(serviceName + ".log");
        assertThat(logFile).exists();
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);

        JsonNode accessLogLine = OBJECT_MAPPER.readTree(lines.get(0));
        assertThat(accessLogLine.path("timestamp").asText()).isNotBlank();
        assertThat(accessLogLine.path("level").asText()).isEqualTo("INFO");
        assertThat(accessLogLine.path("logger").asText()).isEqualTo("com.foodmap.common.logging.LogbackJsonConfigTest");
        assertThat(accessLogLine.path("message").asText())
                .contains("api.access.completed")
                .contains("status=200")
                .doesNotContain("13812345678");
        assertThat(accessLogLine.path("requestId").asText()).isEqualTo("req-t028");
        assertThat(accessLogLine.path("traceId").asText()).isEqualTo("trace-t028");
        assertThat(accessLogLine.path("spanId").asText()).isEqualTo("span-t028");
        assertThat(accessLogLine.path("serviceName").asText()).isEqualTo(serviceName);
        assertThat(accessLogLine.path("logType").asText()).isEqualTo(LogMdcKeys.LOG_TYPE_API_ACCESS);

        JsonNode frameworkLogLine = OBJECT_MAPPER.readTree(lines.get(1));
        assertThat(frameworkLogLine.path("logType").asText()).isEqualTo(LogMdcKeys.LOG_TYPE_APPLICATION);
        assertThat(frameworkLogLine.has("requestId")).isFalse();
    }

    @Test
    void shouldFallbackToConsoleOnlyWhenLogDirectoryIsNotWritable() throws Exception {
        Path blockingFile = tempDir.resolve("blocked");
        Files.writeString(blockingFile, "not a directory");
        Path logDir = blockingFile.resolve("logs");
        LoggerContext context = configure(logDir, "foodmap-t028-unwritable");
        try {
            Logger logger = context.getLogger("com.foodmap.common.logging.LogbackJsonConfigTest");
            logger.info("still logging after file appender failure");
        } finally {
            context.stop();
        }

        assertThat(Files.exists(logDir)).isFalse();
    }

    /**
     * 用 Spring Boot 的 Joran 配置器加载服务级 logback-spring.xml，保证测试覆盖 springProperty 解析路径。
     *
     * @param logDir 测试用日志目录，通过 foodmap.logging.dir 注入模板。
     * @param serviceName 测试用服务名，通过 spring.application.name 注入模板。
     * @return 已按模板完成配置的独立 LoggerContext。
     * @throws Exception 配置文件写入或解析失败。
     */
    private LoggerContext configure(Path logDir, String serviceName) throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", serviceName)
                .withProperty("foodmap.logging.dir", logDir.toString());
        LoggerContext context = new LoggerContext();
        context.start();
        // 测试上下文复用 SLF4J 全局 MDC 适配器，保证 SafeLog 写入的 MDC 能被同步日志事件读到。
        context.setMDCAdapter(MDC.getMDCAdapter());
        JoranConfigurator configurator = newSpringBootJoranConfigurator(environment);
        configurator.setContext(context);
        Path serviceConfig = tempDir.resolve("logback-spring.xml");
        Files.writeString(serviceConfig, SERVICE_CONFIG, StandardCharsets.UTF_8);
        configurator.doConfigure(serviceConfig.toUri().toURL());
        return context;
    }

    /**
     * 通过反射创建 Spring Boot 内部的 Joran 配置器，使测试与真实服务使用同一套 springProperty 规则。
     *
     * @param environment 测试用 Spring 环境，提供 spring.application.name 和 foodmap.logging.dir。
     * @return 已绑定测试环境的 Joran 配置器。
     * @throws Exception 反射创建失败。
     */
    private JoranConfigurator newSpringBootJoranConfigurator(MockEnvironment environment) throws Exception {
        Class<?> configuratorType = Class.forName("org.springframework.boot.logging.logback.SpringBootJoranConfigurator");
        Constructor<?> constructor = configuratorType.getDeclaredConstructor(LoggingInitializationContext.class);
        constructor.setAccessible(true);
        return (JoranConfigurator) constructor.newInstance(new LoggingInitializationContext(environment));
    }
}
