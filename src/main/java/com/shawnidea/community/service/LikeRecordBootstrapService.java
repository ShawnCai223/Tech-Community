package com.shawnidea.community.service;

import com.shawnidea.community.repository.LikeRecordRepository;
import com.shawnidea.community.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LikeRecordBootstrapService implements AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(LikeRecordBootstrapService.class);

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void bootstrapOnStartup() {
        try {
            likeRecordRepository.ensureTable();

            if (likeRecordRepository.countAll() > 0) {
                return;
            }

            Set<String> entityKeys = redisTemplate.keys("like:entity:*");
            if (entityKeys == null || entityKeys.isEmpty()) {
                return;
            }

            Map<Integer, Integer> postOwners = loadEntityOwners(
                    "select id, cast(user_id as signed) as owner_id from discuss_post where status != 2"
            );
            Map<Integer, Integer> commentOwners = loadEntityOwners(
                    "select id, user_id as owner_id from comment where status = 0"
            );

            List<LikeRecordRepository.LikeRecordRow> records = new ArrayList<>();
            for (String entityKey : entityKeys) {
                String[] parts = entityKey.split(":");
                if (parts.length != 4) {
                    continue;
                }

                int entityType = Integer.parseInt(parts[2]);
                int entityId = Integer.parseInt(parts[3]);
                Integer entityUserId = entityType == ENTITY_TYPE_POST
                        ? postOwners.get(entityId)
                        : commentOwners.get(entityId);
                if (entityUserId == null) {
                    continue;
                }

                Set<Object> members = redisTemplate.opsForSet().members(entityKey);
                if (members == null || members.isEmpty()) {
                    continue;
                }

                for (Object member : members) {
                    Integer userId = parseInteger(member);
                    if (userId == null) {
                        continue;
                    }
                    records.add(new LikeRecordRepository.LikeRecordRow(userId, entityType, entityId, entityUserId));
                }
            }

            likeRecordRepository.batchInsert(records);
            logger.info("Backfilled {} like records from Redis into MySQL.", records.size());
        } catch (Exception e) {
            logger.warn("Failed to bootstrap like records from Redis.", e);
        }
    }

    private Map<Integer, Integer> loadEntityOwners(String sql) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Integer, Integer> owners = new HashMap<>();
        for (Map<String, Object> row : rows) {
            owners.put(((Number) row.get("id")).intValue(), ((Number) row.get("owner_id")).intValue());
        }
        return owners;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof String stringValue) {
            return Integer.parseInt(stringValue);
        }
        return null;
    }
}
