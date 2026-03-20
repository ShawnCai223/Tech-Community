package com.shawnidea.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.shawnidea.community.dao.elasticsearch")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        String[] endpoints = uris.split("\\s*,\\s*");
        return ClientConfiguration.builder()
                .connectedTo(endpoints)
                .build();
    }
}
