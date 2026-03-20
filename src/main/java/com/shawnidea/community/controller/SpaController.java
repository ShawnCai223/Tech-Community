package com.shawnidea.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all /app/** requests to the React SPA's index.html.
 * Spring Boot serves the built React files from static/app/.
 */
@Controller
public class SpaController {

    @RequestMapping("/app/**")
    public String forward() {
        return "forward:/app/index.html";
    }
}
