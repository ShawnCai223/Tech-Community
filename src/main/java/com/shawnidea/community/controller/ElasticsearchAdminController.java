package com.shawnidea.community.controller;

import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.service.DiscussPostService;
import com.shawnidea.community.service.ElasticsearchIndexService;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.util.AppUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/elasticsearch")
@ConditionalOnProperty(
        name = {"community.elasticsearch.enabled", "community.elasticsearch.admin-sync.enabled"},
        havingValue = "true"
)
public class ElasticsearchAdminController {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAdminController.class);

    private final DiscussPostService discussPostService;
    private final ElasticsearchIndexService elasticsearchIndexService;
    private final ElasticsearchService elasticsearchService;

    public ElasticsearchAdminController(DiscussPostService discussPostService,
                                        ElasticsearchIndexService elasticsearchIndexService,
                                        ElasticsearchService elasticsearchService) {
        this.discussPostService = discussPostService;
        this.elasticsearchIndexService = elasticsearchIndexService;
        this.elasticsearchService = elasticsearchService;
    }

    @PostMapping(path = "/index", produces = MediaType.APPLICATION_JSON_VALUE)
    public String recreateDiscussPostIndex(@RequestParam(name = "analyzer", defaultValue = "standard") String analyzer) {
        try {
            Map<String, Object> data = elasticsearchIndexService.recreateDiscussPostIndex(analyzer);
            logger.info("Elasticsearch discusspost index recreated: analyzer={}", data.get("analyzer"));
            return AppUtil.getJSONString(0, "Index recreated", data);
        } catch (IllegalArgumentException e) {
            return AppUtil.getJSONString(1, e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Failed to recreate Elasticsearch index with analyzer={}: {}", analyzer, e.getMessage());
            String message = "Failed to recreate the index.";
            if ("ik".equalsIgnoreCase(analyzer)) {
                message += " Make sure the analysis-ik plugin matching your Elasticsearch version is installed.";
            }
            return AppUtil.getJSONString(1, message);
        }
    }

    @PostMapping(path = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    public String syncDiscussPosts() {
        int total = discussPostService.findDiscussPostRows(0);
        int limit = 100;
        int synced = 0;

        for (int offset = 0; offset < total; offset += limit) {
            List<DiscussPost> posts = discussPostService.findDiscussPosts(0, offset, limit, 0);
            for (DiscussPost post : posts) {
                elasticsearchService.saveDiscussPost(post);
                synced++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("synced", synced);
        logger.info("Elasticsearch discuss post sync completed: total={}, synced={}", total, synced);
        return AppUtil.getJSONString(0, "Sync completed", data);
    }
}
