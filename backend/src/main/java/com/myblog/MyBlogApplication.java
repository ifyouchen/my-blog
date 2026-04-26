package com.myblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 博客后端启动类。
 *
 * @author Codex
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MyBlogApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MyBlogApplication.class, args);
    }
}
