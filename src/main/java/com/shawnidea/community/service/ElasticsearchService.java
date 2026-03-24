package com.shawnidea.community.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.shawnidea.community.dao.elasticsearch.DiscussPostRepository;
import com.shawnidea.community.entity.DiscussPost;
import jakarta.annotation.PostConstruct;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.NoSuchIndexException;
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
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchService {

    private static final int SEARCH_CACHE_MAX_SIZE = 256;
    private static final int SEARCH_CACHE_EXPIRE_SECONDS = 30;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private LoadingCache<SearchCacheKey, Page<DiscussPost>> searchResultCache;

    @PostConstruct
    public void init() {
        searchResultCache = Caffeine.newBuilder()
                .maximumSize(SEARCH_CACHE_MAX_SIZE)
                .expireAfterWrite(SEARCH_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
                .build(new CacheLoader<SearchCacheKey, Page<DiscussPost>>() {
                    @Override
                    public @Nullable Page<DiscussPost> load(@NonNull SearchCacheKey key) {
                        return loadSearchResults(key);
                    }
                });
    }

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
        invalidateSearchCache();
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
        invalidateSearchCache();
    }

    public long countDiscussPosts() {
        try {
            return discussRepository.count();
        } catch (NoSuchIndexException exception) {
            return 0;
        }
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        PageRequest pageable = PageRequest.of(current, limit);
        if (!StringUtils.hasText(keyword)) {
            return Page.empty(pageable);
        }

        return searchResultCache.get(new SearchCacheKey(keyword.trim(), current, limit));
    }

    private Page<DiscussPost> loadSearchResults(SearchCacheKey key) {
        PageRequest pageable = PageRequest.of(key.current(), key.limit());

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query -> query.multiMatch(multiMatch -> multiMatch
                        .query(key.keyword())
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
                                List.of(new HighlightField("title"))
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

        return post;
    }

    private DiscussPost copyPost(DiscussPost source) {
        DiscussPost target = new DiscussPost();
        target.setId(source.getId());
        target.setUserId(source.getUserId());
        target.setTitle(source.getTitle());
        target.setContent(null);
        target.setType(source.getType());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        target.setCommentCount(source.getCommentCount());
        target.setScore(source.getScore());
        return target;
    }

    private void invalidateSearchCache() {
        if (searchResultCache != null) {
            searchResultCache.invalidateAll();
        }
    }

    private record SearchCacheKey(String keyword, int current, int limit) {
    }

}
