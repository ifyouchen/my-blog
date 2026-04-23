package com.myblog.infrastructure.config;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.Category;
import com.myblog.domain.model.aggregate.Tag;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.CategoryRepository;
import com.myblog.domain.repository.TagRepository;
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
     * @param categoryRepository 分类仓储
     * @param tagRepository 标签仓储
     * @param articleRepository 文章仓储
     * @param passwordDomainService 密码领域服务
     * @return 启动数据初始化任务
     */
    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      CategoryRepository categoryRepository,
                                      TagRepository tagRepository,
                                      ArticleRepository articleRepository,
                                      ColumnRepository columnRepository,
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
            User backendAuthor = seedUser(userRepository, passwordDomainService, "lin", "lin@example.com", "林川");
            User frontendAuthor = seedUser(userRepository, passwordDomainService, "may", "may@example.com", "沈未");

            seedCategories(categoryRepository);
            seedTags(tagRepository);

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
            if (!articleRepository.findById(new ArticleId(9001L)).isPresent()) {
                articleRepository.save(Article.create(
                    9001L,
                    new UserId(backendAuthor.getId().getValue()),
                    "Spring Cloud 网关鉴权和灰度发布落地清单",
                    "把网关鉴权、流量染色和灰度发布放进一条工程链路里。",
                    "网关层最怕的是规则散落，先把认证、放行和灰度标记统一进一套过滤链。",
                    "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=900&q=80",
                    "Spring Boot",
                    Arrays.asList("Spring Boot", "权限设计", "Redis"),
                    ArticleStatus.PUBLISHED
                ));
            }
            if (!articleRepository.findById(new ArticleId(9002L)).isPresent()) {
                articleRepository.save(Article.create(
                    9002L,
                    new UserId(frontendAuthor.getId().getValue()),
                    "Vue 3 内容社区首页信息架构怎么搭更顺",
                    "从推荐流、关注流、作者榜和专题入口四个角度整理首页结构。",
                    "内容社区首页不是模块堆砌，而是让用户快速判断哪里值得点进去。",
                    "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80",
                    "Vue",
                    Arrays.asList("Vue 3", "编辑器", "Markdown"),
                    ArticleStatus.PUBLISHED
                ));
            }
            if (!articleRepository.findById(new ArticleId(9003L)).isPresent()) {
                articleRepository.save(Article.create(
                    9003L,
                    new UserId(backendAuthor.getId().getValue()),
                    "Redis 缓存一致性：延时双删到底够不够",
                    "把缓存旁路、消息补偿和热点保护放到同一个讨论里。",
                    "缓存一致性没有银弹，但把写路径和回源策略讲清楚，系统会稳定很多。",
                    "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80",
                    "Redis",
                    Arrays.asList("Redis", "分页", "Spring Boot"),
                    ArticleStatus.PUBLISHED
                ));
            }

            seedColumns(columnRepository, user, backendAuthor, frontendAuthor);
        };
    }

    /**
     * 初始化默认分类。
     *
     * @param categoryRepository 分类仓储
     */
    private void seedCategories(CategoryRepository categoryRepository) {
        seedCategory(categoryRepository, "Java", "Java 后端开发与工程实践", 10);
        seedCategory(categoryRepository, "Spring Boot", "Spring Boot 生态、鉴权与工程配置", 20);
        seedCategory(categoryRepository, "Vue", "Vue 3 与前端工程实践", 30);
        seedCategory(categoryRepository, "MySQL", "MySQL 查询优化与数据库设计", 40);
        seedCategory(categoryRepository, "Redis", "Redis 缓存与性能优化", 50);
        seedCategory(categoryRepository, "AI 工程", "AI 应用落地与工程化实践", 60);
        seedCategory(categoryRepository, "运维", "部署、监控与故障处理", 70);
        seedCategory(categoryRepository, "后端", "服务端架构与接口设计", 80);
        seedCategory(categoryRepository, "前端", "界面交互与前端工程实践", 90);
        seedCategory(categoryRepository, "数据库", "数据库建模与性能优化", 100);
    }

    /**
     * 初始化默认标签。
     *
     * @param tagRepository 标签仓储
     */
    private void seedTags(TagRepository tagRepository) {
        seedTag(tagRepository, "Spring Boot", "Spring Boot 框架实践");
        seedTag(tagRepository, "JWT", "JWT 认证与令牌设计");
        seedTag(tagRepository, "权限设计", "角色、菜单与接口权限控制");
        seedTag(tagRepository, "Vue 3", "Vue 3 组件与状态管理");
        seedTag(tagRepository, "编辑器", "富文本与 Markdown 编辑体验");
        seedTag(tagRepository, "Markdown", "Markdown 渲染与写作体验");
        seedTag(tagRepository, "MySQL", "MySQL 数据库实践");
        seedTag(tagRepository, "索引", "索引设计与查询优化");
        seedTag(tagRepository, "分页", "分页与列表查询优化");
        seedTag(tagRepository, "Redis", "Redis 缓存与数据结构");
    }

    /**
     * 创建单个分类种子。
     *
     * @param categoryRepository 分类仓储
     * @param name 分类名称
     * @param description 分类描述
     * @param sortOrder 排序值
     */
    private void seedCategory(CategoryRepository categoryRepository, String name, String description, int sortOrder) {
        if (categoryRepository.existsByName(name, null)) {
            return;
        }
        Category category = Category.create(categoryRepository.nextId(), name, description, sortOrder);
        categoryRepository.save(category);
    }

    /**
     * 创建单个标签种子。
     *
     * @param tagRepository 标签仓储
     * @param name 标签名称
     * @param description 标签描述
     */
    private void seedTag(TagRepository tagRepository, String name, String description) {
        if (tagRepository.existsByName(name, null)) {
            return;
        }
        Tag tag = Tag.create(tagRepository.nextId(), name, description);
        tagRepository.save(tag);
    }

    /**
     * 创建默认演示用户。
     *
     * @param userRepository 用户仓储
     * @param passwordDomainService 密码服务
     * @param username 用户名
     * @param email 邮箱
     * @param nickname 昵称
     * @return 用户对象
     */
    private User seedUser(UserRepository userRepository,
                          PasswordDomainService passwordDomainService,
                          String username,
                          String email,
                          String nickname) {
        Optional<User> optionalUser = userRepository.findByAccount(username);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }
        User user = User.create(
            userRepository.nextId(),
            username,
            email,
            passwordDomainService.encode("123456")
        );
        user.updateProfile(nickname, null, "长期分享工程实践、架构优化和社区产品设计。");
        userRepository.save(user);
        return user;
    }

    /**
     * 初始化默认专栏。
     *
     * @param columnRepository 专栏仓储
     * @param demoUser 演示作者
     * @param backendAuthor 后端作者
     * @param frontendAuthor 前端作者
     */
    private void seedColumns(ColumnRepository columnRepository,
                             User demoUser,
                             User backendAuthor,
                             User frontendAuthor) {
        seedColumn(
            columnRepository,
            2001L,
            demoUser,
            "认证与权限实战",
            "从登录、鉴权到权限收口，把认证链路拆成一组可复用实践。",
            "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80",
            10,
            Arrays.asList(1L, 9001L)
        );
        seedColumn(
            columnRepository,
            2002L,
            frontendAuthor,
            "内容社区前端设计",
            "围绕博客和社区首页、写作台、排行页，持续整理前端体验设计。",
            "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=900&q=80",
            20,
            Arrays.asList(2L, 9002L)
        );
        seedColumn(
            columnRepository,
            2003L,
            backendAuthor,
            "数据库与缓存优化",
            "查询优化、索引设计、缓存一致性和高频列表场景的经验整理。",
            "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=900&q=80",
            30,
            Arrays.asList(3L, 9003L)
        );
    }

    /**
     * 创建单个专栏种子。
     *
     * @param columnRepository 专栏仓储
     * @param id 专栏 ID
     * @param author 作者
     * @param title 标题
     * @param summary 简介
     * @param coverUrl 封面
     * @param sortOrder 排序值
     * @param articleIds 文章 ID 列表
     */
    private void seedColumn(ColumnRepository columnRepository,
                            Long id,
                            User author,
                            String title,
                            String summary,
                            String coverUrl,
                            int sortOrder,
                            java.util.List<Long> articleIds) {
        if (!columnRepository.findById(new com.myblog.domain.model.valueobject.ColumnId(id)).isPresent()) {
            columnRepository.save(Column.create(id, author.getId(), title, summary, coverUrl, sortOrder));
        }
        for (int index = 0; index < articleIds.size(); index++) {
            columnRepository.bindArticle(new com.myblog.domain.model.valueobject.ColumnId(id), articleIds.get(index), (index + 1) * 10);
        }
    }
}
