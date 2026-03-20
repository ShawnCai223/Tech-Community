package com.shawnidea.community.dao.elasticsearch;

import com.shawnidea.community.entity.DiscussPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {

    Page<DiscussPost> findByTitleOrContent(String title, String content, Pageable pageable);
}
