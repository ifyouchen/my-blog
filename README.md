# my-blog

`my-blog` 是一个面向技术创作者和技术读者的博客社区项目。第一版定位为具备商业化雏形的技术博客 MVP，先完成文章发布、阅读、互动和基础管理后台能力。

## 功能范围

第一版包含：

- 用户注册、登录、退出。
- 用户个人资料和个人主页。
- 文章发布、保存草稿、编辑、删除。
- Markdown 写作和文章详情展示。
- 分类和标签。
- 首页文章列表。
- 文章搜索和筛选。
- 评论和回复。
- 点赞和收藏。
- 阅读量统计。
- 我的文章和我的收藏。
- 基础管理员后台。

第一版不包含：

- 付费文章。
- 积分系统。
- 资源下载。
- 会员体系。
- 第三方登录。
- 复杂推荐算法。
- 移动 App。
- 小程序。

## 技术栈

- 前端：Vue 3、Vue Router、Pinia、Axios。
- 后端：JDK 8、Spring Boot 2.7.18、Spring Web、Spring Security、MyBatis。
- 数据库：MySQL。
- 缓存：Redis。
- 认证：JWT。
- 部署：Nginx + Spring Boot + MySQL。

## 开发规范

本项目后端开发必须遵循 DDD 四层架构和阿里巴巴 Java 开发手册。详细规范见：

- [开发规范](docs/10-开发规范.md)

## 推荐目录结构

```text
my-blog/
  backend/
  frontend/
  docs/
    01-需求说明书.md
    02-功能模块设计.md
    03-页面原型与页面清单.md
    04-数据库设计.md
    05-接口设计.md
    06-权限设计.md
    07-技术架构.md
    08-测试用例.md
    09-部署说明.md
  README.md
```

## 快速启动

当前仓库先产出第一版设计文档。前后端工程创建后，建议按以下方式启动。

后端：

```bash
cd backend
mvn spring-boot:run
```

后端默认连接：

- MySQL：`localhost:3306/my_blog`，默认用户 `root`，默认密码 `123456`。
- Redis：`192.168.80.128:6379`，默认密码 `123456`。

启动后端时会通过 MySQL 连接参数自动创建 `my_blog` 数据库，并通过 `backend/src/main/resources/db/schema.sql` 自动建表。

前端：

```bash
cd frontend
npm install
npm run dev
```

默认访问地址建议：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- API 前缀：`/api`

本地联调建议：

1. 启动后端：`cd backend && mvn spring-boot:run`
2. 启动前端：`cd frontend && npm run dev`
3. 前端请求会通过 Vite 代理转发到 `http://localhost:8080/api`

后端当前阶段：

- Spring Boot 2.7.18 + Java 8。
- 已按 DDD 四层结构创建 `interfaces / application / domain / infrastructure / shared`。
- 已实现用户注册、登录、退出、当前用户、文章列表、文章详情、发布文章接口。
- 默认使用 MySQL + MyBatis 仓储和启动种子数据；如需临时切回内存仓储，可启用 `memory` profile。

当前前端首页已迁移为 Vue 3 工程，入口位于：

- `frontend/src/views/HomeView.vue`
- `frontend/src/components/`
- `frontend/src/styles/global.css`

当前已实现的前端页面：

- 首页：`/`
- 登录：`/login`
- 注册：`/register`
- 文章详情：`/articles/:id`
- 发布文章：`/editor/new`
- 个人主页：`/users/:id`
- 搜索结果：`/search`
- 我的文章：`/dashboard/articles`
- 我的收藏：`/dashboard/favorites`
- 管理后台：`/admin`

## 文档索引

- [需求说明书](docs/01-需求说明书.md)
- [功能模块设计](docs/02-功能模块设计.md)
- [页面原型与页面清单](docs/03-页面原型与页面清单.md)
- [数据库设计](docs/04-数据库设计.md)
- [接口设计](docs/05-接口设计.md)
- [权限设计](docs/06-权限设计.md)
- [技术架构](docs/07-技术架构.md)
- [测试用例](docs/08-测试用例.md)
- [部署说明](docs/09-部署说明.md)
- [开发规范](docs/10-开发规范.md)

## 当前版本

当前版本：`v0.1.0`

当前阶段：第一版 MVP 前后端联调。

## 后续规划

- 创建 Spring Boot 后端工程。
- 创建 Vue 3 前端工程。
- 完成数据库初始化脚本。
- 完成用户认证和权限控制。
- 完成文章发布、浏览和互动闭环。
- 完成基础管理后台。

## 项目截图

待前端页面实现后补充。

## 在线演示

待部署后补充。
