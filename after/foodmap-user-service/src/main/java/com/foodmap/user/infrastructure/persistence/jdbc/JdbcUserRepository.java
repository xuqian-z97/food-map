package com.foodmap.user.infrastructure.persistence.jdbc;

import com.foodmap.user.application.port.UserRepository;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 用户 JDBC 仓储实现，负责把 `users` 表映射为用户服务内部持久化实体。
 */
@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserEntity> rowMapper = new UserRowMapper();

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存用户主表实体，先按用户业务主键更新，未命中时插入。
     */
    @Override
    public void save(UserEntity entity) {
        int updated = jdbcTemplate.update("""
                        update users
                        set account_id = ?, nickname = ?, avatar_media_id = ?, user_status = ?,
                            searchable = ?, updated_time = ?, is_delete = ?
                        where user_id = ?
                        """,
                entity.getAccountId(),
                entity.getNickname(),
                entity.getAvatarMediaId(),
                entity.getUserStatus(),
                entity.getSearchable(),
                entity.getUpdatedTime(),
                entity.getIsDelete(),
                entity.getUserId());
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into users
                            (created_time, updated_time, is_delete, user_id, account_id, nickname,
                             avatar_media_id, user_status, searchable)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entity.getCreatedTime(),
                    entity.getUpdatedTime(),
                    entity.getIsDelete(),
                    entity.getUserId(),
                    entity.getAccountId(),
                    entity.getNickname(),
                    entity.getAvatarMediaId(),
                    entity.getUserStatus(),
                    entity.getSearchable());
        }
    }

    /**
     * 根据用户业务主键读取用户主表实体，自动过滤逻辑删除数据。
     */
    @Override
    public Optional<UserEntity> findByUserId(Long userId) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbcTemplate.query("""
                        select * from users
                        where user_id = ? and is_delete = 0
                        limit 1
                        """,
                rowMapper,
                userId)));
    }

    private static final class UserRowMapper implements RowMapper<UserEntity> {
        @Override
        public UserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserEntity entity = new UserEntity();
            entity.setId(rs.getLong("id"));
            entity.setCreatedTime(rs.getObject("created_time", OffsetDateTime.class));
            entity.setUpdatedTime(rs.getObject("updated_time", OffsetDateTime.class));
            entity.setIsDelete(rs.getShort("is_delete"));
            entity.setUserId(rs.getLong("user_id"));
            entity.setAccountId(rs.getLong("account_id"));
            entity.setNickname(rs.getString("nickname"));
            Long avatarMediaId = rs.getObject("avatar_media_id", Long.class);
            entity.setAvatarMediaId(avatarMediaId);
            entity.setUserStatus(rs.getString("user_status"));
            entity.setSearchable(rs.getShort("searchable"));
            return entity;
        }
    }
}
