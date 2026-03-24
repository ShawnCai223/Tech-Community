package com.shawnidea.community.quartz;

import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.service.DiscussPostService;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    private static final long MILLIS_PER_DAY = 1000L * 3600 * 24;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ObjectProvider<ElasticsearchService> elasticsearchServiceProvider;

    // Score baseline date
    private static final Date baselineDate;

    static {
        try {
            baselineDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2025-07-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to initialize score baseline date!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[Skip] No posts require score refresh.");
            return;
        }

        logger.info("[Start] Refreshing scores for {} posts.", operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }
        logger.info("[Done] Post score refresh completed.");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) {
            logger.error("Post does not exist: id={}", postId);
            return;
        }

        // Highlighted posts receive an additional boost.
        boolean wonderful = post.getStatus() == 1;
        // Comment count increases the ranking weight.
        int commentCount = post.getCommentCount();
        // Like count contributes a smaller boost than comments.
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        // Base engagement weight.
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // Time factor = days since 2025-07-01 divided by the days from 2025-07-01 to today.
        // This keeps recency bounded relative to the current date instead of growing indefinitely.
        double elapsedDaysSinceBaseline = (post.getCreateTime().getTime() - baselineDate.getTime()) / (double) MILLIS_PER_DAY;
        double daysFromBaselineToToday = Math.max(
                1.0,
                (System.currentTimeMillis() - baselineDate.getTime()) / (double) MILLIS_PER_DAY
        );
        double score = Math.log10(Math.max(w, 1)) + elapsedDaysSinceBaseline / daysFromBaselineToToday;
        // Persist the refreshed score.
        discussPostService.updateScore(postId, score);

        ElasticsearchService elasticsearchService = elasticsearchServiceProvider.getIfAvailable();
        if (elasticsearchService != null) {
            // Keep the search index aligned with the latest score.
            post.setScore(score);
            elasticsearchService.saveDiscussPost(post);
        }
    }

}
