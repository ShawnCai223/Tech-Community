package com.shawnidea.community.service;

import com.shawnidea.community.entity.DiscussPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchBootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchBootstrapService.class);

    private final DiscussPostService discussPostService;
    private final ElasticsearchIndexService elasticsearchIndexService;
    private final ElasticsearchService elasticsearchService;

    @Value("${community.elasticsearch.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${community.elasticsearch.bootstrap.analyzer:standard}")
    private String analyzer;

    public ElasticsearchBootstrapService(DiscussPostService discussPostService,
                                         ElasticsearchIndexService elasticsearchIndexService,
                                         ElasticsearchService elasticsearchService) {
        this.discussPostService = discussPostService;
        this.elasticsearchIndexService = elasticsearchIndexService;
        this.elasticsearchService = elasticsearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void bootstrapOnStartup() {
        if (!bootstrapEnabled) {
            return;
        }

        try {
            long indexedPosts = elasticsearchService.countDiscussPosts();
            int databasePosts = discussPostService.findDiscussPostRows(0);
            if (indexedPosts >= databasePosts && indexedPosts > 0) {
                logger.info("Skip Elasticsearch bootstrap because index already contains {} documents.", indexedPosts);
                return;
            }

            String normalizedAnalyzer = elasticsearchIndexService.normalizeAnalyzer(analyzer);
            elasticsearchIndexService.recreateDiscussPostIndex(normalizedAnalyzer);

            int synced = 0;
            int batchSize = 100;
            for (int offset = 0; offset < databasePosts; offset += batchSize) {
                List<DiscussPost> posts = discussPostService.findDiscussPosts(0, offset, batchSize, 0);
                for (DiscussPost post : posts) {
                    elasticsearchService.saveDiscussPost(post);
                    synced++;
                }
            }

            logger.info(
                    "Bootstrapped Elasticsearch discusspost index with analyzer={} and synced {} posts.",
                    normalizedAnalyzer,
                    synced
            );
        } catch (Exception e) {
            logger.warn("Failed to bootstrap Elasticsearch index on startup.", e);
        }
    }
}
