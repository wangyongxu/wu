package com.github.wu.spring;

import com.github.wu.spring.biz.AdminService;
import com.github.wu.spring.biz.EmailService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @author wangyongxu
 */
@SpringBootTest(classes = TestApplication.class, properties = "wu.registry.port=52322")
public class RpcAnnotationTest {
    private static final Logger logger = LoggerFactory.getLogger(RpcAnnotationTest.class);
    private static TestingServer server;
    //    @WuInject
    private EmailService emailService;
    //    @WuInject
    private AdminService adminService;

    @BeforeAll
    static void setup() throws Exception {
        server = new TestingServer(52322, true);
        server.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        logger.info("close server: {}", server.getConnectString());
        server.close();
    }

    //    @WuInject
    public void setEmailService(@WuInject EmailService emailService, @WuInject AdminService adminService) {
        this.emailService = emailService;
        this.adminService = adminService;
    }

    @Test
    public void testWuService() {
        String echo = emailService.echo("wu", "hello world");
        Assertions.assertEquals("wu hello world", echo);
    }


    @Test
    public void testRandomLength() {
        int random = RandomUtils.nextInt(0, 10000);
        String msg = RandomStringUtils.random(random);
        String echo = emailService.echo("wu", msg);
        logger.info("success: {}", server.getConnectString());
        Assertions.assertEquals("wu " + msg, echo);

    }


    @Test
    public void testAdmin() {
        String print = adminService.admin("print");
        Assertions.assertEquals("admin print", print);
    }

    @Test
    public void testWuServiceEcho() {
        String echo = emailService.echo("wu", "hello world");
        String print = adminService.admin("print");
        Assertions.assertEquals("wu hello world", echo);
        Assertions.assertEquals("admin print", print);
    }
}
