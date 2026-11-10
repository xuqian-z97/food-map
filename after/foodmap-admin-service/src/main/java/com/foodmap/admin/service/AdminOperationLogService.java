package com.foodmap.admin.service;

import com.foodmap.admin.application.port.AdminBusinessIdGenerator;
import com.foodmap.admin.application.port.AdminOperationLogRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminOperationLogEntity;
import com.foodmap.common.logging.LogField;
import com.foodmap.common.logging.LogMdcKeys;
import com.foodmap.common.logging.SafeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 后台操作审计服务，把后台登录、登出等关键动作写入操作记录表并输出审计日志事件。
 *
 * <p>审计只保存动作事实、业务主键和脱敏摘要，不保存密码、Token 或敏感正文；身份未知的登录失败
 * 只输出审计事件，不构造虚假管理员业务主键。</p>
 */
@Service
public class AdminOperationLogService {
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogService.class);

    /**
     * 后台操作模块：认证。
     */
    private static final String MODULE_AUTH = "AUTH";
    /**
     * 后台操作类型：登录。
     */
    private static final String TYPE_LOGIN = "LOGIN";
    /**
     * 后台操作类型：登出。
     */
    private static final String TYPE_LOGOUT = "LOGOUT";
    /**
     * 操作结果：成功。
     */
    private static final String RESULT_SUCCESS = "SUCCESS";
    /**
     * 操作结果：失败。
     */
    private static final String RESULT_FAILED = "FAILED";

    private final AdminOperationLogRepository operationLogRepository;
    private final AdminBusinessIdGenerator businessIdGenerator;

    /**
     * 创建后台操作审计服务。
     *
     * @param operationLogRepository 后台操作记录仓储端口。
     * @param businessIdGenerator 后台业务主键生成端口。
     */
    public AdminOperationLogService(AdminOperationLogRepository operationLogRepository,
                                    AdminBusinessIdGenerator businessIdGenerator) {
        this.operationLogRepository = operationLogRepository;
        this.businessIdGenerator = businessIdGenerator;
    }

    /**
     * 记录后台登录成功事实。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param username 后台登录账号名。
     */
    public void recordLoginSuccess(Long adminUserId, String username) {
        append(adminUserId, TYPE_LOGIN, adminUserId, "后台管理员登录成功", RESULT_SUCCESS);
        SafeLog.info(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.login.success",
                LogField.of("adminUserId", adminUserId),
                LogField.of("username", username));
    }

    /**
     * 记录后台登录失败事实；管理员身份已知时同时落库，未知身份只输出审计事件。
     *
     * @param adminUserId 后台管理员业务主键，未知身份时为 null。
     * @param username 后台登录账号名。
     * @param reason 失败原因编码，只使用稳定枚举值，不携带原始凭证。
     */
    public void recordLoginFailure(Long adminUserId, String username, String reason) {
        if (adminUserId != null) {
            append(adminUserId, TYPE_LOGIN, adminUserId, "后台管理员登录失败：" + reason, RESULT_FAILED);
        }
        SafeLog.warn(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.login.failed",
                LogField.of("adminUserId", adminUserId),
                LogField.of("username", username),
                LogField.of("reason", reason));
    }

    /**
     * 记录后台登出事实，tokenId 用于关联被撤销的会话。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param tokenId 后台 Access Token 的 JWT jti。
     */
    public void recordLogout(Long adminUserId, String tokenId) {
        append(adminUserId, TYPE_LOGOUT, adminUserId, "后台管理员登出并撤销会话", RESULT_SUCCESS);
        SafeLog.info(log, LogMdcKeys.LOG_TYPE_AUDIT, "admin.logout",
                LogField.of("adminUserId", adminUserId),
                LogField.of("tokenId", tokenId));
    }

    /**
     * 追加操作记录，操作主键来自业务主键生成端口，流水号来自当前 MDC。
     *
     * @param adminUserId 后台管理员业务主键。
     * @param operationType 操作类型。
     * @param targetId 被操作对象业务主键。
     * @param summary 操作脱敏摘要。
     * @param result 操作结果。
     */
    private void append(Long adminUserId, String operationType, Long targetId, String summary, String result) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLogEntity entity = new AdminOperationLogEntity();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        entity.setIsDelete((short) 0);
        entity.setOperationId(businessIdGenerator.nextAdminOperationId());
        entity.setAdminUserId(adminUserId);
        entity.setOperationModule(MODULE_AUTH);
        entity.setOperationType(operationType);
        entity.setTargetType("ADMIN_USER");
        entity.setTargetId(targetId);
        entity.setOperationSummary(summary);
        entity.setOperationResult(result);
        entity.setRequestId(MDC.get(LogMdcKeys.REQUEST_ID));
        entity.setTraceId(MDC.get(LogMdcKeys.TRACE_ID));
        operationLogRepository.append(entity);
    }
}
