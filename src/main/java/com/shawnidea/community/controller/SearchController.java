package com.shawnidea.community.controller;

import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.entity.Page;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class SearchController implements AppConstants {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        // 搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(normalizedKeyword, page.getCurrent() - 1, page.getLimit());
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", normalizedKeyword);

        // 分页信息
        page.setPath("/search?keyword=" + normalizedKeyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());

        return "site/search";
    }

}
