package com.shawnidea.community.service;

import com.alibaba.fastjson.JSONObject;
import com.shawnidea.community.dao.MessageMapper;
import com.shawnidea.community.entity.Message;
import com.shawnidea.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.shawnidea.community.util.AppConstants.ENTITY_TYPE_COMMENT;
import static com.shawnidea.community.util.AppConstants.ENTITY_TYPE_POST;
import static com.shawnidea.community.util.AppConstants.TOPIC_COMMENT;
import static com.shawnidea.community.util.AppConstants.TOPIC_FOLLOW;
import static com.shawnidea.community.util.AppConstants.TOPIC_LIKE;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    public Message findMessageById(int id) {
        return messageMapper.selectMessageById(id);
    }

    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }

    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic, null);
    }

    public int findNoticeCount(int userId, String topic, Integer entityType) {
        return messageMapper.selectNoticeCount(userId, topic, entityType);
    }

    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, null, offset, limit);
    }

    public List<Message> findNotices(int userId, String topic, Integer entityType, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, entityType, offset, limit);
    }

    public Map<String, Integer> buildUnreadSummary(int userId) {
        int directMessageUnreadCount = findLetterUnreadCount(userId, null);
        int likeUnreadCount = findNoticeUnreadCount(userId, TOPIC_LIKE);
        int[] commentCounters = countCommentNoticeTypes(userId);
        int commentUnreadCount = commentCounters[0];
        int replyUnreadCount = commentCounters[1];
        int followUnreadCount = findNoticeUnreadCount(userId, TOPIC_FOLLOW);

        Map<String, Integer> summary = new HashMap<>();
        summary.put("directMessageUnreadCount", directMessageUnreadCount);
        summary.put("likeUnreadCount", likeUnreadCount);
        summary.put("commentUnreadCount", commentUnreadCount);
        summary.put("replyUnreadCount", replyUnreadCount);
        summary.put("followUnreadCount", followUnreadCount);
        summary.put("noticeUnreadCount", likeUnreadCount + commentUnreadCount + replyUnreadCount + followUnreadCount);
        summary.put("totalUnreadCount", directMessageUnreadCount + likeUnreadCount + commentUnreadCount + replyUnreadCount + followUnreadCount);
        return summary;
    }

    private int[] countCommentNoticeTypes(int userId) {
        int commentUnreadCount = 0;
        int replyUnreadCount = 0;

        List<Message> unreadCommentNotices = messageMapper.selectUnreadNotices(userId, TOPIC_COMMENT);
        for (Message notice : unreadCommentNotices) {
            String rawContent = HtmlUtils.htmlUnescape(notice.getContent());
            Integer entityType = JSONObject.parseObject(rawContent).getInteger("entityType");
            if (entityType == null) {
                continue;
            }
            if (entityType == ENTITY_TYPE_POST) {
                commentUnreadCount++;
            } else if (entityType == ENTITY_TYPE_COMMENT) {
                replyUnreadCount++;
            }
        }

        return new int[]{commentUnreadCount, replyUnreadCount};
    }

}
