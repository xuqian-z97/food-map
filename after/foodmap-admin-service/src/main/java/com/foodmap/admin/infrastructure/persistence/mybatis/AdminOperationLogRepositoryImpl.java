package com.foodmap.admin.infrastructure.persistence.mybatis;

import com.foodmap.admin.application.port.AdminOperationLogRepository;
import com.foodmap.admin.infrastructure.persistence.entity.AdminOperationLogEntity;
import com.foodmap.admin.infrastructure.persistence.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Repository;

/**
 * 后台操作记录仓储实现，把审计端口转换为操作记录表的 Mapper 调用。
 */
@Repository
public class AdminOperationLogRepositoryImpl implements AdminOperationLogRepository {
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminOperationLogRepositoryImpl(AdminOperationLogMapper adminOperationLogMapper) {
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    /**
     * 追加一条后台操作审计记录。
     *
     * @param entity 后台操作记录实体。
     */
    @Override
    public void append(AdminOperationLogEntity entity) {
        adminOperationLogMapper.insertOne(entity);
    }
}
