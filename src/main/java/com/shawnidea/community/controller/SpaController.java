package com.shawnidea.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards SPA client-side routes (paths without file extensions)
 * to index.html so React Router can handle them.
 *
 * Static files like /app/index.html, /app/assets/*.css, /app/assets/*.js
 * are served directly by Spring Boot's default static resource handler
 * because they are NOT matched by these mappings.
 */
@Controller
public class SpaController {

    @RequestMapping({
            "/app",
            "/app/login",
            "/app/register",
            "/app/search",
            "/app/messages",
            "/app/settings",
            "/app/post/{id}",
            "/app/profile/{id}",
            "/app/followees/{id}",
            "/app/followers/{id}",
            "/app/messages/{id}",
            "/app/notices/{topic}"
    })
    public String forward() {
        return "forward:/app/index.html";
    }
}
