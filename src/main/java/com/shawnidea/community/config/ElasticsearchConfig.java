package com.shawnidea.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.URI;
import java.util.Arrays;

@Configuration
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.shawnidea.community.dao.elasticsearch")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        String[] endpoints = Arrays.stream(uris.split("\\s*,\\s*"))
                .map(this::normalizeEndpoint)
                .toArray(String[]::new);
        return ClientConfiguration.builder()
                .connectedTo(endpoints)
                .build();
    }

    private String normalizeEndpoint(String endpoint) {
        String value = endpoint.trim();
        if (!value.contains("://")) {
            return value;
        }

        URI uri = URI.create(value);
        int port = uri.getPort();
        if (port < 0) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
        return uri.getHost() + ":" + port;
    }
}
