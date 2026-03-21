package com.shawnidea.community.config;

import com.shawnidea.community.util.AppConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "community.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig implements AppConstants {

    @Bean
    public NewTopic commentTopic() {
        return TopicBuilder.name(TOPIC_COMMENT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic likeTopic() {
        return TopicBuilder.name(TOPIC_LIKE).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic followTopic() {
        return TopicBuilder.name(TOPIC_FOLLOW).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic publishTopic() {
        return TopicBuilder.name(TOPIC_PUBLISH).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deleteTopic() {
        return TopicBuilder.name(TOPIC_DELETE).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic shareTopic() {
        return TopicBuilder.name(TOPIC_SHARE).partitions(1).replicas(1).build();
    }
}
