package com.shawnidea.community.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class LikeRecordRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void ensureTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS like_record (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    entity_type INT NOT NULL,
                    entity_id INT NOT NULL,
                    entity_user_id INT NOT NULL,
                    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_like_user_entity (user_id, entity_type, entity_id),
                    KEY idx_like_entity (entity_type, entity_id),
                    KEY idx_like_entity_user (entity_user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("select count(*) from like_record", Long.class);
        return count == null ? 0 : count;
    }

    public boolean exists(int userId, int entityType, int entityId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from like_record where user_id = ? and entity_type = ? and entity_id = ?",
                Integer.class,
                userId,
                entityType,
                entityId
        );
        return count != null && count > 0;
    }

    public int insertIgnore(int userId, int entityType, int entityId, int entityUserId) {
        return jdbcTemplate.update(
                "insert ignore into like_record(user_id, entity_type, entity_id, entity_user_id, create_time) values (?, ?, ?, ?, ?)",
                userId,
                entityType,
                entityId,
                entityUserId,
                new Timestamp(System.currentTimeMillis())
        );
    }

    public int delete(int userId, int entityType, int entityId) {
        return jdbcTemplate.update(
                "delete from like_record where user_id = ? and entity_type = ? and entity_id = ?",
                userId,
                entityType,
                entityId
        );
    }

    public long countByEntity(int entityType, int entityId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from like_record where entity_type = ? and entity_id = ?",
                Long.class,
                entityType,
                entityId
        );
        return count == null ? 0 : count;
    }

    public int countByEntityUser(int entityUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from like_record where entity_user_id = ?",
                Integer.class,
                entityUserId
        );
        return count == null ? 0 : count;
    }

    public List<Integer> findUserIdsByEntity(int entityType, int entityId) {
        return jdbcTemplate.queryForList(
                "select user_id from like_record where entity_type = ? and entity_id = ? order by user_id",
                Integer.class,
                entityType,
                entityId
        );
    }

    public List<LikeRecordRow> findAll() {
        return jdbcTemplate.query(
                "select user_id, entity_type, entity_id, entity_user_id from like_record order by id",
                (rs, rowNum) -> new LikeRecordRow(
                        rs.getInt("user_id"),
                        rs.getInt("entity_type"),
                        rs.getInt("entity_id"),
                        rs.getInt("entity_user_id")
                )
        );
    }

    public Map<Integer, Long> countByEntityIds(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = entityIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "select entity_id, count(*) as like_count from like_record where entity_type = ? and entity_id in (" +
                placeholders + ") group by entity_id";

        Object[] args = new Object[entityIds.size() + 1];
        args[0] = entityType;
        for (int i = 0; i < entityIds.size(); i++) {
            args[i + 1] = entityIds.get(i);
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("entity_id")).intValue(), ((Number) row.get("like_count")).longValue());
        }
        return counts;
    }

    public void batchInsert(List<LikeRecordRow> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "insert ignore into like_record(user_id, entity_type, entity_id, entity_user_id, create_time) values (?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        LikeRecordRow record = records.get(i);
                        ps.setInt(1, record.userId());
                        ps.setInt(2, record.entityType());
                        ps.setInt(3, record.entityId());
                        ps.setInt(4, record.entityUserId());
                        ps.setTimestamp(5, new Timestamp(new Date().getTime()));
                    }

                    @Override
                    public int getBatchSize() {
                        return records.size();
                    }
                }
        );
    }

    public record LikeRecordRow(int userId, int entityType, int entityId, int entityUserId) {
    }
}
