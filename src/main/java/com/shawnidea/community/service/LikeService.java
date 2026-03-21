package com.shawnidea.community.service;

import com.shawnidea.community.repository.LikeRecordRepository;
import com.shawnidea.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    // 点赞
    @Transactional
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        boolean liked = likeRecordRepository.exists(userId, entityType, entityId);
        int rows;
        if (liked) {
            rows = likeRecordRepository.delete(userId, entityType, entityId);
        } else {
            rows = likeRecordRepository.insertIgnore(userId, entityType, entityId, entityUserId);
        }

        if (rows <= 0) {
            return;
        }

        syncRedisAfterWrite(userId, entityType, entityId, entityUserId, !liked);
    }

    private void syncRedisAfterWrite(int userId, int entityType, int entityId, int entityUserId, boolean liked) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                operations.multi();

                if (liked && !isMember) {
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                } else if (!liked && isMember) {
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);
                }

                return operations.exec();
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(entityLikeKey))) {
            Long size = redisTemplate.opsForSet().size(entityLikeKey);
            return size == null ? 0 : size;
        }

        long count = likeRecordRepository.countByEntity(entityType, entityId);
        if (count > 0) {
            hydrateEntityLikes(entityType, entityId);
        }
        return count;
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(entityLikeKey))) {
            return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
        }

        boolean liked = likeRecordRepository.exists(userId, entityType, entityId);
        if (liked) {
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
        return liked ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Object cached = redisTemplate.opsForValue().get(userLikeKey);
        if (cached instanceof Integer integer) {
            return integer;
        }
        if (cached instanceof Long longValue) {
            return longValue.intValue();
        }
        if (cached instanceof String stringValue) {
            return Integer.parseInt(stringValue);
        }

        int count = likeRecordRepository.countByEntityUser(userId);
        if (count > 0) {
            redisTemplate.opsForValue().set(userLikeKey, count);
        }
        return count;
    }

    public void hydrateEntityLikes(int entityType, int entityId) {
        List<Integer> userIds = likeRecordRepository.findUserIdsByEntity(entityType, entityId);
        if (userIds.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().add(RedisKeyUtil.getEntityLikeKey(entityType, entityId), userIds.toArray());
    }

    public Map<Integer, Long> findEntityLikeCounts(int entityType, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return likeRecordRepository.countByEntityIds(entityType, entityIds);
    }

}
