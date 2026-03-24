package com.shawnidea.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.shawnidea.community.dao.DiscussPostMapper;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Core Caffeine cache abstractions: Cache, LoadingCache, AsyncLoadingCache.

    // Cached home-page post lists.
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // Cached total post count.
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // Initialize the post-list cache.
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("Invalid parameters!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("Invalid parameters!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // The hot-list cache ultimately falls back to MySQL.

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // Initialize the post-count cache.
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }

        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId);
        }

        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("Parameters cannot be empty!");
        }

        // Escape raw HTML.
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // Filter sensitive words.
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        int rows = discussPostMapper.insertDiscussPost(post);
        invalidatePostListCache();
        invalidatePostRowsCache();
        return rows;
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        int rows = discussPostMapper.updateCommentCount(id, commentCount);
        invalidatePostListCache();
        return rows;
    }

    public int updateType(int id, int type) {
        int rows = discussPostMapper.updateType(id, type);
        invalidatePostListCache();
        return rows;
    }

    public int updateStatus(int id, int status) {
        int rows = discussPostMapper.updateStatus(id, status);
        invalidatePostListCache();
        invalidatePostRowsCache();
        return rows;
    }

    public int updateScore(int id, double score) {
        int rows = discussPostMapper.updateScore(id, score);
        invalidatePostListCache();
        return rows;
    }

    private void invalidatePostListCache() {
        if (postListCache != null) {
            postListCache.invalidateAll();
        }
    }

    private void invalidatePostRowsCache() {
        if (postRowsCache != null) {
            postRowsCache.invalidateAll();
        }
    }

}
