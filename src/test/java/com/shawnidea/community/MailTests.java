package com.shawnidea.community;

import com.shawnidea.community.support.ExternalDependencyTest;
import com.shawnidea.community.util.MailClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExternalDependencyTest
@Disabled("Requires external SMTP service and real credentials.")
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("lihonghe@shawnidea.local", "TEST", "Welcome.");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "sunday");

        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);

        mailClient.sendMail("lihonghe@shawnidea.local", "HTML", content);
    }

}
