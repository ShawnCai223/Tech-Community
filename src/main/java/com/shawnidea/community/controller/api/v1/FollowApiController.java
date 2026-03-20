package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.entity.Event;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.event.EventProducer;
import com.shawnidea.community.service.FollowService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/follows")
public class FollowApiController implements AppConstants {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @PostMapping
    public ApiResponse<Void> follow(@RequestBody Map<String, Integer> body) {
        User user = hostHolder.getUser();
        int entityType = body.get("entityType");
        int entityId = body.get("entityId");

        followService.follow(user.getId(), entityType, entityId);

        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> unfollow(@RequestBody Map<String, Integer> body) {
        User user = hostHolder.getUser();
        int entityType = body.get("entityType");
        int entityId = body.get("entityId");

        followService.unfollow(user.getId(), entityType, entityId);

        return ApiResponse.ok();
    }

    @GetMapping("/followees/{userId}")
    public ApiResponse<List<Map<String, Object>>> followees(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> list = followService.findFollowees(userId, offset, limit);
        if (list != null) {
            for (Map<String, Object> map : list) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        return ApiResponse.ok(list);
    }

    @GetMapping("/followers/{userId}")
    public ApiResponse<List<Map<String, Object>>> followers(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> list = followService.findFollowers(userId, offset, limit);
        if (list != null) {
            for (Map<String, Object> map : list) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        return ApiResponse.ok(list);
    }

    private boolean hasFollowed(int userId) {
        User currentUser = hostHolder.getUser();
        if (currentUser == null) {
            return false;
        }
        return followService.hasFollowed(currentUser.getId(), ENTITY_TYPE_USER, userId);
    }

}
