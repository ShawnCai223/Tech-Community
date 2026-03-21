package com.shawnidea.community.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

@Service
public class R2ObjectStorageService implements ObjectStorageService {

    private final String headerBucket;
    private final String shareBucket;
    private final String headerPublicBaseUrl;
    private final String sharePublicBaseUrl;
    private final S3Client s3Client;

    public R2ObjectStorageService(
            @Value("${community.storage.r2.account-id}") String accountId,
            @Value("${community.storage.r2.access-key-id}") String accessKeyId,
            @Value("${community.storage.r2.secret-access-key}") String secretAccessKey,
            @Value("${community.storage.r2.bucket.header}") String headerBucket,
            @Value("${community.storage.r2.bucket.share}") String shareBucket,
            @Value("${community.storage.r2.public-base-url.header}") String headerPublicBaseUrl,
            @Value("${community.storage.r2.public-base-url.share}") String sharePublicBaseUrl
    ) {
        this.headerBucket = headerBucket;
        this.shareBucket = shareBucket;
        this.headerPublicBaseUrl = trimTrailingSlash(headerPublicBaseUrl);
        this.sharePublicBaseUrl = trimTrailingSlash(sharePublicBaseUrl);
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Override
    public String uploadHeader(String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(headerBucket)
                .key(key)
                .contentType(defaultContentType(contentType))
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        return buildPublicUrl(headerPublicBaseUrl, key);
    }

    @Override
    public void uploadShare(String localPath, String key, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(shareBucket)
                .key(key)
                .contentType(defaultContentType(contentType))
                .build();
        s3Client.putObject(request, RequestBody.fromFile(Path.of(localPath)));
    }

    @Override
    public String uploadShareStream(String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(shareBucket)
                .key(key)
                .contentType(defaultContentType(contentType))
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        return buildPublicUrl(sharePublicBaseUrl, key);
    }

    @Override
    public String getShareUrl(String key) {
        return buildPublicUrl(sharePublicBaseUrl, key);
    }

    @PreDestroy
    public void close() {
        s3Client.close();
    }

    private String defaultContentType(String contentType) {
        return StringUtils.isNotBlank(contentType) ? contentType : "application/octet-stream";
    }

    private String buildPublicUrl(String baseUrl, String key) {
        return baseUrl + "/" + key;
    }

    private String trimTrailingSlash(String value) {
        return StringUtils.removeEnd(StringUtils.trimToEmpty(value), "/");
    }
}
