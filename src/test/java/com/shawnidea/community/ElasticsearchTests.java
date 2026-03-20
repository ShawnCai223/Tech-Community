package com.shawnidea.community;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.shawnidea.community.dao.DiscussPostMapper;
import com.shawnidea.community.dao.elasticsearch.DiscussPostRepository;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.support.ManualExplorationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import java.util.List;

@ManualExplorationTest
@org.springframework.boot.test.context.SpringBootTest(properties = {
        "community.elasticsearch.enabled=true",
        "community.elasticsearch.admin-sync.enabled=true"
})
@Disabled("Requires a local Elasticsearch instance with the discusspost index available.")
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100, 0));
    }

    @Test
    void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        discussRepository.save(post);
    }

    @Test
    void testDelete() {
        discussRepository.deleteAll();
    }

    @Test
    void testSearchWithHighlight() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(search -> search.multiMatch(multiMatch -> multiMatch
                        .query("互联网寒冬")
                        .fields("title", "content")))
                .withSort(sort -> sort.field(field -> field.field("type").order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("score").order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("createTime").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(
                                HighlightParameters.builder()
                                        .withPreTags("<em>")
                                        .withPostTags("</em>")
                                        .build(),
                                List.of(new HighlightField("title"), new HighlightField("content"))
                        ),
                        DiscussPost.class
                ))
                .build();

        SearchHits<DiscussPost> hits = elasticsearchOperations.search(query, DiscussPost.class);
        System.out.println(hits.getTotalHits());
        for (SearchHit<DiscussPost> hit : hits) {
            DiscussPost post = hit.getContent();
            List<String> titleHighlights = hit.getHighlightField("title");
            List<String> contentHighlights = hit.getHighlightField("content");
            if (titleHighlights != null && !titleHighlights.isEmpty()) {
                post.setTitle(titleHighlights.get(0));
            }
            if (contentHighlights != null && !contentHighlights.isEmpty()) {
                post.setContent(contentHighlights.get(0));
            }
            System.out.println(post);
        }
    }
}
