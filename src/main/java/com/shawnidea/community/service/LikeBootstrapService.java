package com.shawnidea.community.service;

import com.shawnidea.community.repository.LikeRecordRepository;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LikeBootstrapService implements AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(LikeBootstrapService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Value("${community.likes.bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${community.likes.bootstrap.min-entity-keys:0}")
    private int minEntityKeys;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapOnStartup() {
        try {
            bootstrapIfNeeded();
        } catch (Exception e) {
            logger.warn("Failed to bootstrap likes from application startup.", e);
        }
    }

    public void bootstrapIfNeeded() {
        if (!enabled) {
            return;
        }

        int existingEntityKeyCount = sizeOfKeys("like:entity:*");
        if (existingEntityKeyCount >= minEntityKeys) {
            logger.info("Skip like bootstrap because Redis already has {} like entity keys.", existingEntityKeyCount);
            return;
        }

        if (likeRecordRepository.countAll() > 0) {
            rebuildRedisFromDatabase(existingEntityKeyCount);
            return;
        }

        List<Integer> activeUserIds = loadActiveUserIds();
        if (activeUserIds.size() < 2) {
            logger.warn("Skip like bootstrap because there are only {} active users.", activeUserIds.size());
            return;
        }

        List<PostSeed> postSeeds = loadPostSeeds();
        List<CommentSeed> commentSeeds = loadCommentSeeds();

        Map<Integer, Integer> userLikeCounts = new HashMap<>();

        for (PostSeed post : postSeeds) {
            int finalLikeCount = ensureEntityLikeCount(
                    ENTITY_TYPE_POST,
                    post.id(),
                    post.authorId(),
                    calculatePostLikeCount(post),
                    activeUserIds
            );
            if (finalLikeCount > 0) {
                userLikeCounts.merge(post.authorId(), finalLikeCount, Integer::sum);
            }
        }

        for (CommentSeed comment : commentSeeds) {
            int finalLikeCount = ensureEntityLikeCount(
                    ENTITY_TYPE_COMMENT,
                    comment.id(),
                    comment.authorId(),
                    calculateCommentLikeCount(comment),
                    activeUserIds
            );
            if (finalLikeCount > 0) {
                userLikeCounts.merge(comment.authorId(), finalLikeCount, Integer::sum);
            }
        }

        rebuildUserLikeCounts(userLikeCounts);
        logger.info(
                "Bootstrapped likes into Redis because only {} entity keys existed. Seeded {} posts and {} comments.",
                existingEntityKeyCount,
                postSeeds.size(),
                commentSeeds.size()
        );
    }

    private int ensureEntityLikeCount(int entityType, int entityId, int authorId, int desiredLikeCount, List<Integer> activeUserIds) {
        String redisKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        Set<Integer> currentMembers = readMemberIds(redisKey);

        int maxLikes = Math.max(activeUserIds.size() - 1, 0);
        int targetLikeCount = Math.min(Math.max(desiredLikeCount, 0), maxLikes);
        if (currentMembers.size() >= targetLikeCount) {
            return currentMembers.size();
        }

        int startIndex = Math.floorMod(entityId, activeUserIds.size());
        Set<Integer> selectedMembers = new HashSet<>(currentMembers);
        List<Object> additions = new ArrayList<>();

        for (int offset = 0; offset < activeUserIds.size() && selectedMembers.size() < targetLikeCount; offset++) {
            int userId = activeUserIds.get((startIndex + offset) % activeUserIds.size());
            if (userId == authorId || !selectedMembers.add(userId)) {
                continue;
            }
            additions.add(userId);
        }

        if (!additions.isEmpty()) {
            redisTemplate.opsForSet().add(redisKey, additions.toArray());
        }

        return selectedMembers.size();
    }

    private Set<Integer> readMemberIds(String redisKey) {
        Set<Object> rawMembers = redisTemplate.opsForSet().members(redisKey);
        Set<Integer> members = new HashSet<>();
        if (rawMembers == null) {
            return members;
        }
        for (Object rawMember : rawMembers) {
            if (rawMember instanceof Integer integer) {
                members.add(integer);
            } else if (rawMember instanceof Long longValue) {
                members.add(longValue.intValue());
            } else if (rawMember instanceof String stringValue) {
                members.add(Integer.parseInt(stringValue));
            }
        }
        return members;
    }

    private void rebuildUserLikeCounts(Map<Integer, Integer> userLikeCounts) {
        Set<String> userLikeKeys = redisTemplate.keys("like:user:*");
        if (userLikeKeys != null && !userLikeKeys.isEmpty()) {
            redisTemplate.delete(userLikeKeys);
        }

        for (Map.Entry<Integer, Integer> entry : userLikeCounts.entrySet()) {
            redisTemplate.opsForValue().set(RedisKeyUtil.getUserLikeKey(entry.getKey()), entry.getValue());
        }
    }

    private int sizeOfKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? 0 : keys.size();
    }

    private void rebuildRedisFromDatabase(int existingEntityKeyCount) {
        clearLikeKeys();

        Map<Integer, Integer> userLikeCounts = new HashMap<>();
        for (LikeRecordRepository.LikeRecordRow record : likeRecordRepository.findAll()) {
            redisTemplate.opsForSet().add(
                    RedisKeyUtil.getEntityLikeKey(record.entityType(), record.entityId()),
                    record.userId()
            );
            userLikeCounts.merge(record.entityUserId(), 1, Integer::sum);
        }

        rebuildUserLikeCounts(userLikeCounts);
        logger.info(
                "Rebuilt Redis likes from {} durable records because only {} entity keys existed.",
                likeRecordRepository.countAll(),
                existingEntityKeyCount
        );
    }

    private void clearLikeKeys() {
        Set<String> entityKeys = redisTemplate.keys("like:entity:*");
        if (entityKeys != null && !entityKeys.isEmpty()) {
            redisTemplate.delete(entityKeys);
        }
        Set<String> userKeys = redisTemplate.keys("like:user:*");
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
    }

    List<Integer> loadActiveUserIds() {
        return jdbcTemplate.queryForList(
                "select id from user where id <> ? and status = 1 order by id",
                Integer.class,
                SYSTEM_USER_ID
        );
    }

    List<PostSeed> loadPostSeeds() {
        return jdbcTemplate.query(
                "select id, cast(user_id as signed), type, status, comment_count from discuss_post where status != 2 order by id",
                (rs, rowNum) -> new PostSeed(
                        rs.getInt("id"),
                        rs.getInt(2),
                        rs.getInt("type"),
                        rs.getInt("status"),
                        rs.getInt("comment_count")
                )
        );
    }

    List<CommentSeed> loadCommentSeeds() {
        return jdbcTemplate.query(
                "select id, user_id, entity_type, entity_id, target_id from comment where status = 0 order by id",
                (rs, rowNum) -> new CommentSeed(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("entity_type"),
                        rs.getInt("entity_id"),
                        rs.getInt("target_id")
                )
        );
    }

    private int calculatePostLikeCount(PostSeed post) {
        int likeCount = (post.commentCount() * 2) + 3 + (post.id() % 7);
        if (post.status() == 1) {
            likeCount += 12;
        }
        if (post.type() == 1) {
            likeCount += 15;
        }
        if (post.status() == 2) {
            likeCount = (post.commentCount() / 3) + 1;
        }
        return likeCount;
    }

    private int calculateCommentLikeCount(CommentSeed comment) {
        int boost = comment.entityType() == ENTITY_TYPE_POST
                ? comment.entityId() % 6
                : 1 + (comment.targetId() % 3);
        return 2 + (comment.id() % 5) + boost;
    }

    static record PostSeed(int id, int authorId, int type, int status, int commentCount) {
    }

    static record CommentSeed(int id, int authorId, int entityType, int entityId, int targetId) {
    }
}
