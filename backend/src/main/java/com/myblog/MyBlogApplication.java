package com.myblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 博客后端启动类。
 *
 * @author Codex
 * @since 1.0.0
 */
@SpringBootApplication
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
