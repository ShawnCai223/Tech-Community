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
    private static final double RECENCY_SCALE_DAYS = 45.0;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ObjectProvider<ElasticsearchService> elasticsearchServiceProvider;

    // Score epoch baseline
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to initialize score epoch baseline!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: " + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) {
            logger.error("该帖子不存在: id = " + postId);
            return;
        }

        // 是否精华
        boolean wonderful = post.getStatus() == 1;
        // 评论数量
        int commentCount = post.getCommentCount();
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        // 计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数 = 帖子权重 + 折算后的时间衰减.
        // 时间项如果过重, 新发但零互动的帖子会长期压过已有互动的内容.
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000.0 * 3600 * 24 * RECENCY_SCALE_DAYS);
        // 更新帖子分数
        discussPostService.updateScore(postId, score);

        ElasticsearchService elasticsearchService = elasticsearchServiceProvider.getIfAvailable();
        if (elasticsearchService != null) {
            // 同步搜索数据
            post.setScore(score);
            elasticsearchService.saveDiscussPost(post);
        }
    }

}
