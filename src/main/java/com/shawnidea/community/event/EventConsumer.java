package com.shawnidea.community.event;

import com.alibaba.fastjson.JSONObject;
import com.shawnidea.community.entity.Event;
import com.shawnidea.community.util.AppConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "community.kafka.enabled", havingValue = "true")
public class EventConsumer implements AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private EventHandler eventHandler;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event != null) {
            eventHandler.handleEvent(event);
        }
    }

    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event != null) {
            eventHandler.handleEvent(event);
        }
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event != null) {
            eventHandler.handleEvent(event);
        }
    }

    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event != null) {
            eventHandler.handleEvent(event);
        }
    }

    private Event parseEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return null;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return null;
        }
        return event;
    }
}
