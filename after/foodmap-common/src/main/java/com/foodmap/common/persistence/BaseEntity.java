package com.foodmap.common.persistence;

import java.time.OffsetDateTime;

/**
 * 业务表固定字段基类，统一承载 FoodMap 所有业务表的内部主键、创建时间、更新时间和逻辑删除标记。
 *
 * <p>该类只表达数据库固定字段，不包含 {@code user_id}、{@code account_id} 等业务主键。
 * 排查持久化映射问题时，应先确认子类业务主键和本类内部主键没有被混用。</p>
 */
public abstract class BaseEntity {

    private Long id;
    private OffsetDateTime createdTime;
    private OffsetDateTime updatedTime;
    private Short isDelete;

    /**
     * 返回数据库内部自增主键。该值只允许在服务自己的数据库内部使用。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置数据库内部自增主键。业务代码跨服务引用时不能使用该值。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回记录创建时间。排查数据写入顺序和审计问题时优先查看该字段。
     */
    public OffsetDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * 设置记录创建时间。后续接入 ORM 自动填充时应集中维护该字段。
     */
    public void setCreatedTime(OffsetDateTime createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * 返回记录最近更新时间。排查并发覆盖和缓存不一致时优先查看该字段。
     */
    public OffsetDateTime getUpdatedTime() {
        return updatedTime;
    }

    /**
     * 设置记录最近更新时间。后续接入 ORM 自动填充时应集中维护该字段。
     */
    public void setUpdatedTime(OffsetDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    /**
     * 返回逻辑删除标记，{@code 0} 表示未删除，{@code 1} 表示已删除。
     */
    public Short getIsDelete() {
        return isDelete;
    }

    /**
     * 设置逻辑删除标记。查询条件必须显式过滤已删除记录。
     */
    public void setIsDelete(Short isDelete) {
        this.isDelete = isDelete;
    }
}
