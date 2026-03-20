package com.shawnidea.community;

import com.shawnidea.community.entity.LoginTicket;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.support.ExternalDependencyTest;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.AppUtil;
import com.shawnidea.community.util.RedisKeyUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExternalDependencyTest
@org.springframework.boot.test.context.SpringBootTest(properties = {
        "community.elasticsearch.enabled=true",
        "community.elasticsearch.admin-sync.enabled=true",
        "spring.elasticsearch.uris=localhost:9200"
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "community.es.integration", matches = "true")
public class ElasticsearchIntegrationTests implements AppConstants {

    private static final int AUTHOR_ID = 149;
    private static final int ADMIN_ID = 11;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private final List<String> ticketKeys = new ArrayList<>();
    private Integer createdPostId;

    @AfterEach
    void cleanup() {
        if (createdPostId != null) {
            jdbcTemplate.update("delete from discuss_post where id = ?", createdPostId);
            elasticsearchService.deleteDiscussPost(createdPostId);
            redisTemplate.opsForSet().remove(RedisKeyUtil.getPostScoreKey(), createdPostId);
        }

        for (String ticketKey : ticketKeys) {
            redisTemplate.delete(ticketKey);
        }
    }

    @Test
    void shouldSyncAndSearchDiscussPostsOnRealElasticsearch() throws Exception {
        MockCookie adminCookie = createLoginCookie(ADMIN_ID);
        MockCookie authorCookie = createLoginCookie(AUTHOR_ID);

        mockMvc.perform(post("/admin/elasticsearch/index")
                        .cookie(adminCookie)
                        .param("analyzer", "standard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        mockMvc.perform(post("/admin/elasticsearch/sync")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        String unique = "es-flow-" + Instant.now().toEpochMilli();
        String title = "搜索联调 " + unique;
        String body = "这是搜索联调内容 " + unique;

        mockMvc.perform(post("/discuss/add")
                        .cookie(authorCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", title)
                        .param("content", body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        createdPostId = jdbcTemplate.queryForObject(
                "select id from discuss_post where user_id = ? and title = ? order by id desc limit 1",
                Integer.class, AUTHOR_ID, title);
        assertNotNull(createdPostId);

        waitForIndexedPost(unique, createdPostId);

        Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(unique, 0, 10);
        assertTrue(searchResult.stream().anyMatch(post ->
                post.getId() == createdPostId
                        && (post.getTitle().contains("<em>") || post.getContent().contains("<em>"))));

        mockMvc.perform(get("/search")
                        .param("keyword", unique))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(unique)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<em>")));

        mockMvc.perform(post("/discuss/delete")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", String.valueOf(createdPostId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        waitForDeletedPost(unique, createdPostId);
    }

    @Test
    @EnabledIfSystemProperty(named = "community.es.ik.integration", matches = "true")
    void shouldSupportChineseSearchWhenIkAnalyzerIsInstalled() throws Exception {
        MockCookie adminCookie = createLoginCookie(ADMIN_ID);
        MockCookie authorCookie = createLoginCookie(AUTHOR_ID);

        mockMvc.perform(post("/admin/elasticsearch/index")
                        .cookie(adminCookie)
                        .param("analyzer", "ik"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        mockMvc.perform(post("/admin/elasticsearch/sync")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        String unique = "ik-flow-" + Instant.now().toEpochMilli();
        String title = "互联网岗位 " + unique;
        String body = "这是一条关于互联网求职的测试内容 " + unique;

        mockMvc.perform(post("/discuss/add")
                        .cookie(authorCookie)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", title)
                        .param("content", body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        createdPostId = jdbcTemplate.queryForObject(
                "select id from discuss_post where user_id = ? and title = ? order by id desc limit 1",
                Integer.class, AUTHOR_ID, title);
        assertNotNull(createdPostId);

        waitForIndexedPost(unique, createdPostId);

        Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost("互联网", 0, 10);
        assertTrue(searchResult.stream().anyMatch(post ->
                post.getId() == createdPostId
                        && (post.getTitle().contains("<em>互联网</em>")
                        || post.getContent().contains("<em>互联网</em>"))));

        mockMvc.perform(get("/search")
                        .param("keyword", "互联网"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(unique)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<em>互联网</em>")));
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

    private void waitForIndexedPost(String keyword, int expectedPostId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Page<DiscussPost> result = elasticsearchService.searchDiscussPost(keyword, 0, 10);
            if (result.stream().anyMatch(post -> post.getId() == expectedPostId)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Discuss post was not indexed into Elasticsearch in time: " + expectedPostId);
    }

    private void waitForDeletedPost(String keyword, int deletedPostId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Page<DiscussPost> result = elasticsearchService.searchDiscussPost(keyword, 0, 10);
            if (result.stream().noneMatch(post -> post.getId() == deletedPostId)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Discuss post was not removed from Elasticsearch in time: " + deletedPostId);
    }
}
