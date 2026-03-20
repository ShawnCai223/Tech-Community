package com.shawnidea.community.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnProperty(name = "community.elasticsearch.enabled", havingValue = "true")
public class SearchController {

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(@RequestParam("keyword") String keyword) {
        return "redirect:/app/search?keyword=" + keyword;
    }
}
