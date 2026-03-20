package com.shawnidea.community;

import com.shawnidea.community.support.ManualExplorationTest;
import com.shawnidea.community.entity.DiscussPost;
import com.shawnidea.community.service.DiscussPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@ManualExplorationTest
public class CaffeineTests {

    @Autowired
    private DiscussPostService postService;

    @Test
    public void initDataForTest() {
        for (int i = 0; i < 300000; i++) {
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("互联网求职暖春计划");
            post.setContent("今年的就业形势确实不容乐观。过了个年，仿佛跳水一般，整个讨论区哀鸿遍野。19届真的没人要了吗？18届被优化真的没有出路了吗？大家的哀嚎与遭遇牵动了社区成员的心，于是平台决定做点什么。为了帮助大家度过寒冬，社区联合60+家企业开启互联网求职暖春计划，面向18届和19届，争取帮大家摆脱 0 offer。");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            postService.addDiscussPost(post);
        }
    }

    @Test
    public void testCache() {
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.findDiscussPosts(0, 0, 10, 1));
        System.out.println(postService.findDiscussPosts(0, 0, 10, 0));
    }

}
