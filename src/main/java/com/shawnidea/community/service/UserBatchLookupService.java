package com.shawnidea.community.service;

import com.shawnidea.community.dao.UserMapper;
import com.shawnidea.community.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserBatchLookupService {

    @Autowired
    private UserMapper userMapper;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public Map<Integer, User> findUsersByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userMapper.selectByIds(ids);
        Map<Integer, User> usersById = new HashMap<>();
        for (User user : users) {
            usersById.put(user.getId(), normalizeUser(user));
        }
        return usersById;
    }

    private User normalizeUser(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.isBlank(user.getHeaderUrl())
                || isLegacyRemoteHeader(user.getHeaderUrl())
                || isLegacyDefaultHeaderUrl(user.getHeaderUrl())) {
            user.setHeaderUrl(defaultHeaderUrl());
        }
        return user;
    }

    private boolean isLegacyDefaultHeaderUrl(String headerUrl) {
        return headerUrl.matches("https?://(?:localhost|127\\.0\\.0\\.1)(?::\\d+)?/community/img/avatar-default\\.svg");
    }

    private boolean isLegacyRemoteHeader(String headerUrl) {
        return headerUrl.matches("https?://(?:image|images|static|www)\\.[^/]+/.*");
    }

    private String defaultHeaderUrl() {
        return domain + contextPath + "/img/avatar-default.svg";
    }
}
