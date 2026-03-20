package com.shawnidea.community;

import com.shawnidea.community.entity.LoginTicket;
import com.shawnidea.community.support.ShawnIdeaIntegrationTest;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.AppUtil;
import com.shawnidea.community.util.RedisKeyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ShawnIdeaIntegrationTest
@AutoConfigureMockMvc
public class MainFlowIntegrationTests implements AppConstants {

    private static final int AUTHOR_ID = 149;
    private static final int COMMENTER_ID = 101;
    private static final int ADMIN_ID = 11;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final List<String> ticketKeys = new ArrayList<>();
    private Integer createdPostId;
    private Integer createdCommentId;
    private Integer beforeAuthorLikeCount;

    @AfterEach
    void cleanup() {
        if (createdPostId != null) {
            jdbcTemplate.update("delete from message where from_id = ? and content like ?", SYSTEM_USER_ID, "%\\\"postId\\\":" + createdPostId + "%");
            jdbcTemplate.update("delete from comment where entity_type = ? and entity_id = ?", ENTITY_TYPE_POST, createdPostId);
            jdbcTemplate.update("delete from discuss_post where id = ?", createdPostId);
            redisTemplate.opsForSet().remove(RedisKeyUtil.getPostScoreKey(), createdPostId);
            redisTemplate.delete(RedisKeyUtil.getEntityLikeKey(ENTITY_TYPE_POST, createdPostId));
        }

        if (beforeAuthorLikeCount == null) {
            redisTemplate.delete(RedisKeyUtil.getUserLikeKey(AUTHOR_ID));
        } else {
            redisTemplate.opsForValue().set(RedisKeyUtil.getUserLikeKey(AUTHOR_ID), beforeAuthorLikeCount);
        }

        for (String ticketKey : ticketKeys) {
            redisTemplate.delete(ticketKey);
        }
    }

    @Test
    void shouldCompleteMainFlowWithLocalEventFallback() throws Exception {
        MockCookie authorCookie = createLoginCookie(AUTHOR_ID);
        MockCookie commenterCookie = createLoginCookie(COMMENTER_ID);
        MockCookie adminCookie = createLoginCookie(ADMIN_ID);

        String unique = "mockmvc-flow-" + Instant.now().toEpochMilli();
        String title = "主链路联调 " + unique;
        String postContent = "post-" + unique;
        String commentContent = "comment-" + unique;

        beforeAuthorLikeCount = getRedisInt(RedisKeyUtil.getUserLikeKey(AUTHOR_ID));
        int beforeCommentNoticeCount = countNotices(AUTHOR_ID, TOPIC_COMMENT);
        int beforeLikeNoticeCount = countNotices(AUTHOR_ID, TOPIC_LIKE);

        mockMvc.perform(post("/discuss/add")
                        .cookie(authorCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", title)
                        .param("content", postContent))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        createdPostId = jdbcTemplate.queryForObject(
                "select id from discuss_post where user_id = ? and title = ? order by id desc limit 1",
                Integer.class, AUTHOR_ID, title);
        assertNotNull(createdPostId);
        assertEquals(0, jdbcTemplate.queryForObject(
                "select status from discuss_post where id = ?", Integer.class, createdPostId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select comment_count from discuss_post where id = ?", Integer.class, createdPostId));
        assertTrue(Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(
                RedisKeyUtil.getPostScoreKey(), createdPostId)));

        mockMvc.perform(post("/comment/add/" + createdPostId)
                        .cookie(commenterCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("entityType", String.valueOf(ENTITY_TYPE_POST))
                .param("entityId", String.valueOf(createdPostId))
                .param("targetId", "0")
                .param("content", commentContent))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/discuss/detail/" + createdPostId + "*"));

        createdCommentId = jdbcTemplate.queryForObject(
                "select id from comment where user_id = ? and entity_type = ? and entity_id = ? and content = ? order by id desc limit 1",
                Integer.class, COMMENTER_ID, ENTITY_TYPE_POST, createdPostId, commentContent);
        assertNotNull(createdCommentId);
        assertEquals(1, jdbcTemplate.queryForObject(
                "select comment_count from discuss_post where id = ?", Integer.class, createdPostId));
        assertEquals(beforeCommentNoticeCount + 1, countNotices(AUTHOR_ID, TOPIC_COMMENT));
        String latestCommentNotice = jdbcTemplate.queryForObject(
                "select content from message where from_id = ? and to_id = ? and conversation_id = ? order by id desc limit 1",
                String.class, SYSTEM_USER_ID, AUTHOR_ID, TOPIC_COMMENT);
        assertTrue(latestCommentNotice.contains("&quot;userId&quot;:101"));
        assertTrue(latestCommentNotice.contains("&quot;entityType&quot;:1"));
        assertTrue(latestCommentNotice.contains("&quot;entityId&quot;:" + createdPostId));
        assertTrue(latestCommentNotice.contains("&quot;postId&quot;:" + createdPostId));

        mockMvc.perform(post("/like")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("entityType", String.valueOf(ENTITY_TYPE_POST))
                        .param("entityId", String.valueOf(createdPostId))
                        .param("entityUserId", String.valueOf(AUTHOR_ID))
                        .param("postId", String.valueOf(createdPostId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"likeCount\":1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"likeStatus\":1")));

        assertTrue(Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(
                RedisKeyUtil.getEntityLikeKey(ENTITY_TYPE_POST, createdPostId), ADMIN_ID)));
        assertEquals((beforeAuthorLikeCount == null ? 0 : beforeAuthorLikeCount) + 1,
                getRedisInt(RedisKeyUtil.getUserLikeKey(AUTHOR_ID)));
        assertEquals(beforeLikeNoticeCount + 1, countNotices(AUTHOR_ID, TOPIC_LIKE));
        String latestLikeNotice = jdbcTemplate.queryForObject(
                "select content from message where from_id = ? and to_id = ? and conversation_id = ? order by id desc limit 1",
                String.class, SYSTEM_USER_ID, AUTHOR_ID, TOPIC_LIKE);
        assertTrue(latestLikeNotice.contains("&quot;userId&quot;:11"));
        assertTrue(latestLikeNotice.contains("&quot;entityType&quot;:1"));
        assertTrue(latestLikeNotice.contains("&quot;entityId&quot;:" + createdPostId));
        assertTrue(latestLikeNotice.contains("&quot;postId&quot;:" + createdPostId));

        mockMvc.perform(post("/discuss/delete")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", String.valueOf(createdPostId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        assertEquals(2, jdbcTemplate.queryForObject(
                "select status from discuss_post where id = ?", Integer.class, createdPostId));
    }

    private MockCookie createLoginCookie(int userId) {
        String ticket = AppUtil.generateUUID();
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(userId);
        loginTicket.setTicket(ticket);
        loginTicket.setStatus(0);
        loginTicket.setExpired(Date.from(Instant.now().plusSeconds(DEFAULT_EXPIRED_SECONDS)));

        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
        ticketKeys.add(redisKey);

        return new MockCookie("ticket", ticket);
    }

    private int countNotices(int userId, String topic) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(id) from message where from_id = ? and to_id = ? and conversation_id = ?",
                Integer.class, SYSTEM_USER_ID, userId, topic);
        return count == null ? 0 : count;
    }

    private Integer getRedisInt(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof String stringValue) {
            return Integer.parseInt(stringValue);
        }
        throw new IllegalStateException("Unexpected redis value type: " + value.getClass());
    }
}
