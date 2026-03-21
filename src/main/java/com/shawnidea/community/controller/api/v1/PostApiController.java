package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.dto.PageResponse;
import com.shawnidea.community.entity.Comment;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.entity.Event;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.event.EventProducer;
import com.shawnidea.community.service.CommentService;
import com.shawnidea.community.service.DiscussPostService;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.service.UserBatchLookupService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.HostHolder;
import com.shawnidea.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/posts")
public class PostApiController implements AppConstants {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserBatchLookupService userBatchLookupService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int orderMode) {
        int offset = page * limit;
        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, offset, limit, orderMode);
        int totalRows = discussPostService.findDiscussPostRows(0);

        List<Integer> postIds = posts.stream().map(DiscussPost::getId).collect(Collectors.toList());
        List<Integer> userIds = posts.stream().map(DiscussPost::getUserId).distinct().collect(Collectors.toList());
        Map<Integer, User> usersById = userBatchLookupService.findUsersByIds(userIds);
        Map<Integer, Long> likeCounts = likeService.findEntityLikeCounts(ENTITY_TYPE_POST, postIds);

        List<Map<String, Object>> list = new ArrayList<>();
        for (DiscussPost post : posts) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("post", post);
            vo.put("user", usersById.get(post.getUserId()));
            vo.put("likeCount", likeCounts.getOrDefault(post.getId(), 0L));
            list.add(vo);
        }

        int totalPages = (totalRows + limit - 1) / limit;
        return ApiResponse.ok(new PageResponse<>(list, page, totalPages, totalRows));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") int commentPage,
            @RequestParam(defaultValue = "5") int commentLimit) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        if (post == null) {
            return ApiResponse.error(404, "Post not found.");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("user", userService.findUserById(post.getUserId()));
        data.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, id));

        User currentUser = hostHolder.getUser();
        data.put("likeStatus", currentUser == null ? 0 :
                likeService.findEntityLikeStatus(currentUser.getId(), ENTITY_TYPE_POST, id));

        // Comments
        int commentOffset = commentPage * commentLimit;
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, id, commentOffset, commentLimit);
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> commentVo = new HashMap<>();
                commentVo.put("comment", comment);
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                commentVo.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId()));
                commentVo.put("likeStatus", currentUser == null ? 0 :
                        likeService.findEntityLikeStatus(currentUser.getId(), ENTITY_TYPE_COMMENT, comment.getId()));

                // Replies
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply", reply);
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        replyVo.put("target", reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId()));
                        replyVo.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId()));
                        replyVo.put("likeStatus", currentUser == null ? 0 :
                                likeService.findEntityLikeStatus(currentUser.getId(), ENTITY_TYPE_COMMENT, reply.getId()));
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replies", replyVoList);
                commentVo.put("replyCount", commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId()));
                commentVoList.add(commentVo);
            }
        }
        data.put("comments", commentVoList);
        data.put("commentCount", post.getCommentCount());

        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(401, "You are not signed in.");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(body.get("title"));
        post.setContent(body.get("content"));
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("id", post.getId());
        return ApiResponse.ok(data);
    }

    @PutMapping("/{id}/pin")
    public ApiResponse<Void> pin(@PathVariable int id) {
        discussPostService.updateType(id, 1);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return ApiResponse.ok();
    }

    @PutMapping("/{id}/highlight")
    public ApiResponse<Void> highlight(@PathVariable int id) {
        discussPostService.updateStatus(id, 1);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable int id) {
        discussPostService.updateStatus(id, 2);

        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return ApiResponse.ok();
    }

}
