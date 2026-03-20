package com.shawnidea.community.controller.api.v1;

import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.FollowService;
import com.shawnidea.community.service.LikeService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.AppUtil;
import com.shawnidea.community.util.HostHolder;
import com.shawnidea.community.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserApiController implements AppConstants {

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private ObjectStorageService objectStorageService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(401, "You are not signed in.");
        }
        return ApiResponse.ok(buildUserProfile(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> profile(@PathVariable int id) {
        User user = userService.findUserById(id);
        if (user == null) {
            return ApiResponse.error(404, "User not found.");
        }
        return ApiResponse.ok(buildUserProfile(user));
    }

    @PostMapping("/me/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("headerImage") MultipartFile headerImage) {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(401, "You are not signed in.");
        }
        if (headerImage == null || headerImage.isEmpty()) {
            return ApiResponse.error(400, "No file selected.");
        }

        String filename = headerImage.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return ApiResponse.error(400, "Invalid file format.");
        }
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (!".png".equals(suffix) && !".jpg".equals(suffix) && !".jpeg".equals(suffix)) {
            return ApiResponse.error(400, "Only PNG and JPG files are supported.");
        }

        try {
            String fileName = "headers/" + AppUtil.generateUUID() + suffix.toLowerCase();
            String headerUrl = objectStorageService.uploadHeader(
                    fileName,
                    headerImage.getInputStream(),
                    headerImage.getSize(),
                    headerImage.getContentType()
            );
            userService.updateHeader(user.getId(), headerUrl);

            Map<String, String> data = new HashMap<>();
            data.put("headerUrl", headerUrl);
            return ApiResponse.ok(data);
        } catch (IOException e) {
            return ApiResponse.error(500, "Upload failed.");
        }
    }

    private Map<String, Object> buildUserProfile(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("headerUrl", user.getHeaderUrl());
        data.put("type", user.getType());
        data.put("createTime", user.getCreateTime());
        data.put("likeCount", likeService.findUserLikeCount(user.getId()));
        data.put("followeeCount", followService.findFolloweeCount(user.getId(), ENTITY_TYPE_USER));
        data.put("followerCount", followService.findFollowerCount(ENTITY_TYPE_USER, user.getId()));

        User currentUser = hostHolder.getUser();
        data.put("hasFollowed", currentUser != null &&
                followService.hasFollowed(currentUser.getId(), ENTITY_TYPE_USER, user.getId()));

        return data;
    }

}
