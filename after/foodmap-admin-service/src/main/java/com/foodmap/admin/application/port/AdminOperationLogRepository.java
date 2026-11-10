package com.foodmap.admin.application.port;

import com.foodmap.admin.infrastructure.persistence.entity.AdminOperationLogEntity;

/**
 * 后台操作记录仓储端口，隔离应用层与审计落库的具体持久化技术。
 */
public interface AdminOperationLogRepository {

    /**
     * 追加一条后台操作审计记录。
     *
     * @param entity 后台操作记录实体。
     */
    void append(AdminOperationLogEntity entity);
}
