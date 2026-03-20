package com.shawnidea.community.event;

import com.alibaba.fastjson.JSONObject;
import com.shawnidea.community.entity.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventHandler eventHandler;

    @Value("${community.kafka.enabled:true}")
    private boolean kafkaEnabled;

    // 处理事件
    public void fireEvent(Event event) {
        if (kafkaEnabled) {
            kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
            return;
        }

        logger.debug("Kafka未启用, 使用本地事件处理: topic={}", event.getTopic());
        eventHandler.handleEvent(event);
    }

}
