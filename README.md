# my-blog

面向技术创作者和技术读者的博客社区平台，具备完整的文章发布、阅读、互动、管理后台以及用户成长体系。
预览地址：https://www.gemini4.click/

## 技术栈

**后端：** JDK 8、Spring Boot 2.7.18、MyBatis、MySQL 8.0+、Redis（Caffeine L1 + Redis L2 + Redisson RTopic）、自定义 JWT（HMAC-SHA256）、腾讯云 COS

**前端：** Vue 3.5、Vite 5、Vue Router 4、TipTap 富文本编辑器（highlight.js 代码高亮）、原生 fetch()

## 功能概览

- **用户系统** — 邮箱注册/登录、忘记密码、个人资料、修改密码、换绑邮箱
- **文章系统** — TipTap 富文本编辑、Markdown 模式、封面图、草稿/发布/下线/删除、版本管理、文章导出 ZIP
- **分类与标签** — 分类组 + 多级分类体系、标签系统
- **专栏与专题** — 创作者专栏（系列文章排序）、平台专题（话题聚合）
- **互动系统** — 评论/回复（置顶、编辑、点赞）、文章点赞/收藏、用户关注
- **消息与通知** — 系统通知（评论/点赞/收藏/关注）、私信（基于会话）、SSE 实时推送
- **内容发现** — 首页推荐、全文搜索、排行榜、个性化推荐、关注动态、浏览历史
- **创作者后台（Dashboard）** — 数据概览、趋势图、文章管理、专栏管理、收藏管理、成长面板
- **管理后台（Admin）** — 用户/文章/评论/举报/分类/标签/专栏/专题/广告/公告/敏感词/日志/成长管理
- **成长体系** — 经验值（XP）+ 等级（10级）、积分（可消费）、每日签到（连续奖励）、徽章（16种）、邀请奖励、文章解锁（积分支付）、收益分成（50:50）
- **图片上传** — COS 预签名 URL 直传、代理上传降级、限流

## 快速启动

### 环境要求

- JDK 8+、Maven 3.6+
- Node.js 18+、npm 9+
- MySQL 8.0+、Redis 6.0+

### 后端

```bash
cd backend
mvn spring-boot:run
```

数据库 `my_blog` 自动创建（需 MySQL 账号有建库权限），表结构自动初始化。默认连接 `localhost:3306`，用户 `root`，密码 `123456`。Redis 默认连接 `localhost:6379`，无密码。

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`，Vite 代理 `/api` 到 `http://localhost:8080`。

### 快速验证

```bash
cd backend && mvn test        # 后端单元测试
cd frontend && npm run build  # 前端构建
cd frontend && npm run test:e2e  # 前端 E2E 测试
```

## 项目结构

```
my-blog/
  backend/           — Spring Boot 后端（DDD 四层架构）
  frontend/          — Vue 3 前端
  docs/              — 设计文档
    ├── 01-需求说明书.md
    ├── 02-功能模块设计.md
    ├── 03-页面原型与页面清单.md
    ├── 04-数据库设计.md
    ├── 05-接口设计.md
    ├── 06-权限设计.md
    ├── 07-技术架构.md
    ├── 08-测试用例.md
    ├── 09-部署说明.md
    ├── 10-开发规范.md
    ├── 产品说明书.md
    └── 经验与积分/   — 成长模块详细设计
  scripts/           — 运维脚本
  db/                — 数据迁移和种子数据
```

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
- [产品说明书](docs/产品说明书.md)
- [经验与积分设计](docs/经验与积分/)

## 许可证

MIT
