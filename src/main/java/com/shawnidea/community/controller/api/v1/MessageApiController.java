package com.shawnidea.community.controller.api.v1;

import com.alibaba.fastjson.JSONObject;
import com.shawnidea.community.dto.ApiResponse;
import com.shawnidea.community.dto.PageResponse;
import com.shawnidea.community.entity.Message;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.MessageService;
import com.shawnidea.community.service.UserService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.util.HostHolder;
import com.shawnidea.community.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageApiController implements AppConstants {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageWebSocketHandler messageWebSocketHandler;

    @GetMapping("/letters")
    public ApiResponse<PageResponse<Map<String, Object>>> letterList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        int offset = page * limit;

        List<Message> conversations = messageService.findConversations(user.getId(), offset, limit);
        int totalRows = messageService.findConversationCount(user.getId());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Message message : conversations) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("conversation", message);
            vo.put("letterCount", messageService.findLetterCount(message.getConversationId()));
            vo.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
            int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
            vo.put("target", userService.findUserById(targetId));
            list.add(vo);
        }

        int totalPages = (totalRows + limit - 1) / limit;
        return ApiResponse.ok(new PageResponse<>(list, page, totalPages, totalRows));
    }

    @GetMapping("/letters/{conversationId}")
    public ApiResponse<PageResponse<Map<String, Object>>> letterDetail(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        User user = hostHolder.getUser();
        int offset = page * limit;

        List<Message> letters = messageService.findLetters(conversationId, offset, limit);
        int totalRows = messageService.findLetterCount(conversationId);

        // Mark as read
        List<Integer> unreadIds = new ArrayList<>();
        for (Message letter : letters) {
            if (letter.getToId() == user.getId() && letter.getStatus() == 0) {
                unreadIds.add(letter.getId());
            }
        }
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds);
            messageWebSocketHandler.sendSummaryUpdate(user.getId());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Message letter : letters) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("letter", letter);
            vo.put("fromUser", userService.findUserById(letter.getFromId()));
            list.add(vo);
        }

        int totalPages = (totalRows + limit - 1) / limit;
        return ApiResponse.ok(new PageResponse<>(list, page, totalPages, totalRows));
    }

    @PostMapping("/letters")
    public ApiResponse<Void> sendLetter(@RequestBody Map<String, String> body) {
        String toName = body.get("toName");
        String content = body.get("content");

        User target = userService.findUserByName(toName);
        if (target == null) {
            return ApiResponse.error(400, "Target user does not exist.");
        }

        User user = hostHolder.getUser();
        Message message = new Message();
        message.setFromId(user.getId());
        message.setToId(target.getId());
        String conversationId = user.getId() < target.getId()
                ? user.getId() + "_" + target.getId()
                : target.getId() + "_" + user.getId();
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        messageWebSocketHandler.sendLetterCreated(target.getId(), message, user);

        return ApiResponse.ok();
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Integer>> summary() {
        User user = hostHolder.getUser();
        return ApiResponse.ok(messageService.buildUnreadSummary(user.getId()));
    }

    @GetMapping("/notices")
    public ApiResponse<Map<String, Object>> noticeList() {
        User user = hostHolder.getUser();
        Map<String, Object> data = new HashMap<>();

        // Comment notices
        data.put("comment", buildNoticeInfo(user.getId(), TOPIC_COMMENT));
        data.put("like", buildNoticeInfo(user.getId(), TOPIC_LIKE));
        data.put("follow", buildNoticeInfo(user.getId(), TOPIC_FOLLOW));

        data.put("letterUnreadCount", messageService.findLetterUnreadCount(user.getId(), null));
        data.put("noticeUnreadCount",
                messageService.findNoticeUnreadCount(user.getId(), null));

        return ApiResponse.ok(data);
    }

    @GetMapping("/notices/{topic}")
    public ApiResponse<PageResponse<Map<String, Object>>> noticeDetail(
            @PathVariable String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        User user = hostHolder.getUser();
        int offset = page * limit;

        List<Message> notices = messageService.findNotices(user.getId(), topic, offset, limit);
        int totalRows = messageService.findNoticeCount(user.getId(), topic);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Message notice : notices) {
            Map<String, Object> vo = new HashMap<>();
            vo.put("notice", notice);
            Map<String, Object> content = JSONObject.parseObject(notice.getContent(), HashMap.class);
            vo.put("user", userService.findUserById((Integer) content.get("userId")));
            vo.put("entityType", content.get("entityType"));
            vo.put("entityId", content.get("entityId"));
            vo.put("postId", content.get("postId"));
            list.add(vo);
        }

        // Mark as read
        List<Integer> unreadIds = new ArrayList<>();
        for (Message notice : notices) {
            if (notice.getToId() == user.getId() && notice.getStatus() == 0) {
                unreadIds.add(notice.getId());
            }
        }
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds);
            messageWebSocketHandler.sendSummaryUpdate(user.getId());
        }

        int totalPages = (totalRows + limit - 1) / limit;
        return ApiResponse.ok(new PageResponse<>(list, page, totalPages, totalRows));
    }

    private Map<String, Object> buildNoticeInfo(int userId, String topic) {
        Map<String, Object> info = new HashMap<>();
        Message latest = messageService.findLatestNotice(userId, topic);
        if (latest != null) {
            info.put("message", latest);
            Map<String, Object> content = JSONObject.parseObject(latest.getContent(), HashMap.class);
            info.put("user", userService.findUserById((Integer) content.get("userId")));
            info.put("entityType", content.get("entityType"));
            info.put("entityId", content.get("entityId"));
            info.put("count", messageService.findNoticeCount(userId, topic));
            info.put("unread", messageService.findNoticeUnreadCount(userId, topic));
        }
        return info;
    }

}
