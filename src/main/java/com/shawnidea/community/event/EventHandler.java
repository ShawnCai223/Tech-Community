package com.shawnidea.community.event;

import com.alibaba.fastjson.JSONObject;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.entity.Event;
import com.shawnidea.community.entity.Message;
import com.shawnidea.community.service.DiscussPostService;
import com.shawnidea.community.service.ElasticsearchService;
import com.shawnidea.community.service.MessageService;
import com.shawnidea.community.service.ObjectStorageService;
import com.shawnidea.community.util.AppConstants;
import com.shawnidea.community.websocket.MessageWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventHandler implements AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ObjectProvider<ElasticsearchService> elasticsearchServiceProvider;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private MessageWebSocketHandler messageWebSocketHandler;

    public void handleEvent(Event event) {
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        String topic = event.getTopic();
        if (TOPIC_COMMENT.equals(topic) || TOPIC_LIKE.equals(topic) || TOPIC_FOLLOW.equals(topic)) {
            handleNoticeEvent(event);
            return;
        }
        if (TOPIC_PUBLISH.equals(topic)) {
            handlePublishEvent(event);
            return;
        }
        if (TOPIC_DELETE.equals(topic)) {
            handleDeleteEvent(event);
            return;
        }
        if (TOPIC_SHARE.equals(topic)) {
            handleShareEvent(event);
            return;
        }

        logger.warn("忽略未知事件主题: {}", topic);
    }

    private void handleNoticeEvent(Event event) {
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
        messageWebSocketHandler.sendNoticeCreated(event.getEntityUserId(), resolveNoticeCategory(event));
    }

    private void handlePublishEvent(Event event) {
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        if (post == null) {
            logger.error("同步ES失败, 帖子不存在: id={}", event.getEntityId());
            return;
        }

        ElasticsearchService elasticsearchService = elasticsearchServiceProvider.getIfAvailable();
        if (elasticsearchService == null) {
            logger.debug("Elasticsearch未启用, 跳过发帖同步: id={}", event.getEntityId());
            return;
        }

        elasticsearchService.saveDiscussPost(post);
    }

    private void handleDeleteEvent(Event event) {
        ElasticsearchService elasticsearchService = elasticsearchServiceProvider.getIfAvailable();
        if (elasticsearchService == null) {
            logger.debug("Elasticsearch未启用, 跳过删帖同步: id={}", event.getEntityId());
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    private void handleShareEvent(Event event) {
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: {}", cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: {}", e.getMessage());
        }

        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements Runnable {

        private final String fileName;
        private final String suffix;
        private Future future;
        private final long startTime;
        private int uploadTimes;

        UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长,终止任务: {}", fileName);
                future.cancel(true);
                return;
            }
            if (uploadTimes >= 3) {
                logger.error("上传次数过多,终止任务: {}", fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (!file.exists()) {
                logger.info("等待图片生成[{}].", fileName);
                return;
            }

            logger.info("开始第{}次上传[{}].", ++uploadTimes, fileName);
            try {
                objectStorageService.uploadShare(path, fileName, "image/" + suffix.replaceFirst("^\\.", ""));
                logger.info("第{}次上传成功[{}].", uploadTimes, fileName);
                future.cancel(true);
            } catch (RuntimeException e) {
                logger.info("第{}次上传失败[{}].", uploadTimes, fileName);
            }
        }
    }

    private String resolveNoticeCategory(Event event) {
        if (TOPIC_COMMENT.equals(event.getTopic())) {
            return event.getEntityType() == ENTITY_TYPE_COMMENT ? "reply" : "comment";
        }
        return event.getTopic();
    }
}
