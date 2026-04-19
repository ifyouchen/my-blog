package com.myblog.infrastructure.config;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.domain.service.PasswordDomainService;
import com.myblog.shared.enums.ArticleStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Optional;

/**
 * 本地开发种子数据。
 *
 * @author Codex
 * @since 1.0.0
 */
@Configuration
public class SeedDataConfig {

    /**
     * 初始化本地开发种子数据。
     *
     * @param userRepository 用户仓储
     * @param articleRepository 文章仓储
     * @param passwordDomainService 密码领域服务
     * @return 启动数据初始化任务
     */
    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      ArticleRepository articleRepository,
                                      PasswordDomainService passwordDomainService) {
        return args -> {
            Optional<User> optionalUser = userRepository.findByAccount("demo");
            User user = optionalUser.orElseGet(() -> {
                User createdUser = User.create(
                    userRepository.nextId(),
                    "demo",
                    "demo@example.com",
                    passwordDomainService.encode("123456")
                );
                userRepository.save(createdUser);
                return createdUser;
            });

            if (!articleRepository.findById(new ArticleId(1L)).isPresent()) {
                articleRepository.save(Article.create(
                    1L,
                    new UserId(user.getId().getValue()),
                    "Spring Boot 登录鉴权从 0 到 1：JWT、权限拦截和异常返回",
                    "把登录态、角色权限、接口拦截、统一错误码串成完整闭环。",
                    "登录鉴权的第一步不是选择 Token 还是 Session，而是先明确接口权限矩阵。",
                    "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80",
                    "后端",
                    Arrays.asList("Spring Boot", "JWT", "权限设计"),
                    ArticleStatus.PUBLISHED
                ));
            }
            if (!articleRepository.findById(new ArticleId(2L)).isPresent()) {
                articleRepository.save(Article.create(
                    2L,
                    new UserId(user.getId().getValue()),
                    "Vue 3 博客编辑器体验设计：草稿、预览、标签和封面上传",
                    "文章编辑不是一个表单，而是一条创作路径。",
                    "博客编辑器需要让创作者尽快进入写作状态，草稿能力能显著降低发布压力。",
                    "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=700&q=80",
                    "前端",
                    Arrays.asList("Vue 3", "编辑器", "Markdown"),
                    ArticleStatus.PUBLISHED
                ));
            }
            if (!articleRepository.findById(new ArticleId(3L)).isPresent()) {
                articleRepository.save(Article.create(
                    3L,
                    new UserId(user.getId().getValue()),
                    "MySQL 文章列表查询优化：分类、标签、排序和分页索引怎么建",
                    "第一版不用上搜索引擎，也能通过清晰索引撑住常见列表场景。",
                    "首页推荐列表可以优先按发布时间排序，分类页可以使用 category 加时间索引。",
                    "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=700&q=80",
                    "数据库",
                    Arrays.asList("MySQL", "索引", "分页"),
                    ArticleStatus.PUBLISHED
                ));
            }
        };
    }
}
