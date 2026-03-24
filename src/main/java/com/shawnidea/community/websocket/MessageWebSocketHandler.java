package com.shawnidea.community.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shawnidea.community.entity.Message;
import com.shawnidea.community.entity.User;
import com.shawnidea.community.service.MessageService;
import com.shawnidea.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final Map<Integer, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();
    private final Map<String, Integer> userIdBySessionId = new ConcurrentHashMap<>();

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing authenticated user."));
            return;
        }

        sessionsByUserId.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        userIdBySessionId.put(session.getId(), userId);
        sendSummaryUpdate(userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void sendSummaryUpdate(int userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "summary.updated");
        payload.put("summary", messageService.buildUnreadSummary(userId));
        sendToUser(userId, payload);
    }

    public void sendLetterCreated(int recipientUserId, Message message, User fromUser) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", message.getConversationId());
        data.put("letter", buildLetterMap(message));
        data.put("fromUser", buildUserMap(fromUser));

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "letter.created");
        payload.put("data", data);
        payload.put("summary", messageService.buildUnreadSummary(recipientUserId));
        sendToUser(recipientUserId, payload);
    }

    public void sendNoticeCreated(int userId, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("category", category);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "notice.created");
        payload.put("data", data);
        payload.put("summary", messageService.buildUnreadSummary(userId));
        sendToUser(userId, payload);
    }

    private void sendToUser(int userId, Map<String, Object> payload) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize WebSocket payload.", e);
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                unregister(session);
            }
        }
    }

    private void unregister(WebSocketSession session) {
        Integer userId = userIdBySessionId.remove(session.getId());
        if (userId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId);
        }
    }

    private Map<String, Object> buildLetterMap(Message message) {
        Map<String, Object> letter = new HashMap<>();
        letter.put("id", message.getId());
        letter.put("fromId", message.getFromId());
        letter.put("toId", message.getToId());
        letter.put("conversationId", message.getConversationId());
        letter.put("content", message.getContent());
        letter.put("status", message.getStatus());
        letter.put("createTime", message.getCreateTime());
        return letter;
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("headerUrl", user.getHeaderUrl());
        data.put("type", user.getType());
        data.put("createTime", user.getCreateTime());
        return data;
    }
}
