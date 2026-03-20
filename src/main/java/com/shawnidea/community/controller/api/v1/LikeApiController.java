package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.entity.Event;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.event.EventProducer;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.HostHolder;
import com.shawnidea.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/likes")
public class LikeApiController implements AppConstants {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    public ApiResponse<Map<String, Object>> like(@RequestBody Map<String, Integer> body) {
        User user = hostHolder.getUser();
        int entityType = body.get("entityType");
        int entityId = body.get("entityId");
        int entityUserId = body.get("entityUserId");
        int postId = body.get("postId");

        likeService.like(user.getId(), entityType, entityId, entityUserId);

        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", likeCount);
        data.put("likeStatus", likeStatus);

        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(user.getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST) {
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return ApiResponse.ok(data);
    }

}
