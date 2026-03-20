package com.shawnidea.community;

import com.shawnidea.community.support.ExternalDependencyTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@ExternalDependencyTest
@Disabled("Requires an external Kafka broker.")
public class KafkaTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testKafka() {
        kafkaTemplate.send("test", "你好");
        kafkaTemplate.send("test", "在吗");

        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
