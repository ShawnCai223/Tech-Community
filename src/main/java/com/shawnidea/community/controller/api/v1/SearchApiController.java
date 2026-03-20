package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.dto.PageResponse;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class SearchApiController implements AppConstants {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page, limit);

        List<Map<String, Object>> list = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> vo = new HashMap<>();
                vo.put("post", post);
                vo.put("user", userService.findUserById(post.getUserId()));
                vo.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                list.add(vo);
            }
        }

        long totalElements = searchResult != null ? searchResult.getTotalElements() : 0;
        int totalPages = searchResult != null ? searchResult.getTotalPages() : 0;
        return ApiResponse.ok(new PageResponse<>(list, page, totalPages, totalElements));
    }

}
