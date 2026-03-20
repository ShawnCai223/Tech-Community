package com.shawnidea.community.service;

import java.io.InputStream;

public interface ObjectStorageService {

    String uploadHeader(String key, InputStream inputStream, long contentLength, String contentType);

    void uploadShare(String localPath, String key, String contentType);

    String getShareUrl(String key);
}
