package com.shawnidea.community.service;

import com.shawnidea.community.entity.DiscussPost;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchIndexService {

    private static final Set<String> SUPPORTED_ANALYZERS = Set.of("standard", "ik");

    private final ElasticsearchOperations elasticsearchOperations;
    private final ResourceLoader resourceLoader;

    public ElasticsearchIndexService(ElasticsearchOperations elasticsearchOperations,
                                     ResourceLoader resourceLoader) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.resourceLoader = resourceLoader;
    }

    public Map<String, Object> recreateDiscussPostIndex(String analyzer) {
        String normalizedAnalyzer = normalizeAnalyzer(analyzer);
        IndexOperations indexOperations = elasticsearchOperations.indexOps(DiscussPost.class);

        boolean deleted = false;
        if (indexOperations.exists()) {
            deleted = indexOperations.delete();
        }

        if (!indexOperations.create()) {
            throw new IllegalStateException("Failed to create the discusspost index.");
        }

        Document mapping = loadMapping(normalizedAnalyzer);
        if (!indexOperations.putMapping(mapping)) {
            throw new IllegalStateException("Failed to apply the discusspost index mapping.");
        }
        indexOperations.refresh();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("index", indexOperations.getIndexCoordinates().getIndexName());
        data.put("analyzer", normalizedAnalyzer);
        data.put("deletedExistingIndex", deleted);
        return data;
    }

    public String normalizeAnalyzer(String analyzer) {
        String normalized = StringUtils.hasText(analyzer)
                ? analyzer.trim().toLowerCase(Locale.ROOT)
                : "standard";
        if (!SUPPORTED_ANALYZERS.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported analyzer '" + analyzer + "'. Supported values: standard, ik.");
        }
        return normalized;
    }

    private Document loadMapping(String analyzer) {
        String resourcePath = "classpath:elasticsearch/discusspost-mapping-" + analyzer + ".json";
        Resource resource = resourceLoader.getResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return Document.parse(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Elasticsearch mapping: " + resourcePath, e);
        }
    }
}
