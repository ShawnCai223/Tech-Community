package com.shawnidea.community.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.shawnidea.community.dao.elasticsearch.DiscussPostRepository;
import com.shawnidea.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    public long countDiscussPosts() {
        return discussRepository.count();
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        PageRequest pageable = PageRequest.of(current, limit);
        if (!StringUtils.hasText(keyword)) {
            return Page.empty(pageable);
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query -> query.multiMatch(multiMatch -> multiMatch
                        .query(keyword)
                        .fields("title", "content")))
                .withSort(sort -> sort.field(field -> field.field("type").order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("score").order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("createTime").order(SortOrder.Desc)))
                .withPageable(pageable)
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(
                                HighlightParameters.builder()
                                        .withPreTags("<em>")
                                        .withPostTags("</em>")
                                        .build(),
                                List.of(
                                        new HighlightField("title"),
                                        new HighlightField("content")
                                )
                        ),
                        DiscussPost.class
                ))
                .build();

        SearchHits<DiscussPost> searchHits = elasticsearchOperations.search(searchQuery, DiscussPost.class);
        List<DiscussPost> posts = searchHits.getSearchHits().stream()
                .map(this::mapSearchHit)
                .toList();
        return new PageImpl<>(posts, pageable, searchHits.getTotalHits());
    }

    private DiscussPost mapSearchHit(SearchHit<DiscussPost> searchHit) {
        DiscussPost source = searchHit.getContent();
        DiscussPost post = copyPost(source);

        List<String> titleHighlights = searchHit.getHighlightField("title");
        if (titleHighlights != null && !titleHighlights.isEmpty()) {
            post.setTitle(titleHighlights.get(0));
        }

        List<String> contentHighlights = searchHit.getHighlightField("content");
        if (contentHighlights != null && !contentHighlights.isEmpty()) {
            post.setContent(contentHighlights.get(0));
        }

        return post;
    }

    private DiscussPost copyPost(DiscussPost source) {
        DiscussPost target = new DiscussPost();
        target.setId(source.getId());
        target.setUserId(source.getUserId());
        target.setTitle(source.getTitle());
        target.setContent(source.getContent());
        target.setType(source.getType());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        target.setCommentCount(source.getCommentCount());
        target.setScore(source.getScore());
        return target;
    }

}
