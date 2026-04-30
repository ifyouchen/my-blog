# 12-设计v3.md 实施进度追踪

> 每完成一个条目后在此文档更新状态，避免每次重新扫描全部代码。
> 状态：✅ 已完成 | 🔧 进行中 | ❌ 未开始

---

## Phase 1：核心路径体验修补

### 1.1 账号与认证
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_user 新增 email/email_verified/password_reset_token/password_reset_expire/last_login_at/last_login_ip | ✅ | schema.sql 已有 |
| 后端: POST /api/auth/password/forgot | ✅ | AuthController 已实现 |
| 后端: POST /api/auth/password/reset | ✅ | AuthController 已实现 |
| 后端: POST /api/users/me/password | ✅ | UserController 已实现 |
| 后端: POST /api/users/me/email（绑定邮箱） | ✅ | User.changeEmail()，UserAppService.changeEmail()，UserController POST /me/email |
| 后端: GET /api/users/me/security | ✅ | UserController 已实现 |
| 前端: /auth/forgot-password 页面 | ✅ | ForgotPasswordView.vue 已存在 |
| 前端: /auth/reset-password 页面 | ✅ | ResetPasswordView.vue 已存在 |
| 前端: 设置页账号安全 Tab（改密码、绑定邮箱、最近登录） | ✅ | ProfileSettingsView.vue 绑定邮箱表单+changeEmailApi 已实现 |
| 前端: 注册成功欢迎引导弹窗 | ❌ | AuthView.vue 中未实现 |

### 1.2 个人资料完善
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_user 新增 website/github/twitter/location | ✅ | schema.sql 已有 |
| 后端: PUT /api/users/me/profile 支持新字段 | ✅ | UserController 已实现 |
| 前端: /settings/profile 扩展字段（website/github/twitter/location） | ✅ | ProfileSettingsView.vue 已实现 |
| 前端: 用户主页展示新字段 | ✅ | UserProfileView.vue 已实现 |

### 1.3 移动端响应式
| 条目 | 状态 | 备注 |
|---|---|---|
| 前端: 编辑器工具栏移动端折叠 | ❌ | 未实现 |
| 前端: 管理后台表格移动端卡片布局 | ❌ | 未实现 |
| 前端: 首页侧边栏 v-if 小屏不渲染 | ❌ | 未实现 |
| 前端: 搜索页筛选底部 bottom sheet | ❌ | 未实现 |

### 1.4 页面加载体验
| 条目 | 状态 | 备注 |
|---|---|---|
| 前端: 文章详情骨架屏 | ❌ | 未实现 |
| 前端: 列表页骨架屏 | ❌ | 未实现 |
| 前端: img lazy 和 decoding async | ❌ | 未全局统一 |
| 前端: 路由切换 nprogress 进度条 | ❌ | 未实现 |

---

## Phase 2：创作者能力增强

### 2.1 Markdown 编辑器增强
| 条目 | 状态 | 备注 |
|---|---|---|
| 前端: 工具栏插入表格 | ❌ | 未实现 |
| 前端: 粘贴图片自动上传 | ❌ | 未实现 |
| 前端: 代码块语言下拉菜单 | ❌ | 未实现 |
| 前端: 字数统计 + 阅读时间 | ❌ | 未实现 |
| 前端: TOC 折叠/展开 | ❌ | 未实现 |
| 前端: 快捷键 Tooltip | ❌ | 未实现 |

### 2.2 文章草稿与版本
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_article_version 表 | ✅ | schema.sql 已有 |
| 后端: GET /api/articles/{id}/versions | ✅ | ArticleController 已实现 |
| 后端: GET /api/articles/{id}/versions/{versionNo} | ✅ | ArticleController 已实现 |
| 后端: POST /api/articles/{id}/versions/{versionNo}/restore | ✅ | ArticleController 已实现 |
| 前端: 编辑器历史版本侧边抽屉 | ❌ | EditorView.vue 未实现 |
| 前端: 自动保存状态标识 | ❌ | 未实现 |

### 2.3 专栏管理（创作者侧）
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/users/me/columns | ✅ | UserController 已实现（/me/columns 通过 DashboardColumnController）|
| 后端: POST /api/columns（创作者创建）| ✅ | DashboardColumnController 已实现 |
| 后端: PUT /api/columns/{id}（创作者更新）| ✅ | DashboardColumnController 已实现 |
| 后端: DELETE /api/columns/{id}（创作者删除）| ✅ | DashboardColumnController 已实现 |
| 后端: POST /api/columns/{id}/articles | ✅ | DashboardColumnController 已实现 |
| 后端: DELETE /api/columns/{id}/articles/{articleId} | ✅ | DashboardColumnController 已实现 |
| 前端: /dashboard/columns 页面 | ✅ | DashboardColumnsView.vue 专栏列表+创建/编辑弹窗+文章管理抽屉，路由+侧边栏导航已添加 |
| 前端: Dashboard 概览页"我的专栏"卡片 | ✅ | DashboardView.vue 添加专栏入口卡片+专栏数统计 |

### 2.4 创作者数据增强
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/dashboard/articles/{id}/stats（单篇趋势） | ❌ | DashboardController 未实现 |
| 前端: 文章详情趋势图抽屉 | ❌ | 未实现 |

---

## Phase 3：内容消费与发现优化

### 3.1 文章详情页体验
| 条目 | 状态 | 备注 |
|---|---|---|
| 前端: 正文区最大宽度 720px | ✅ | 已有 .article-main 宽度限制 |
| 前端: 阅读进度条 | ✅ | ArticleDetailView.vue readingProgress + .reading-progress-bar |
| 前端: 分享按钮（复制链接、微博）| ✅ | ArticleDetailView.vue 分享菜单（复制链接+微博），document click 关闭 |
| 前端: 文章底部上一篇/下一篇 | ✅ | 后端 GET /articles/{id}/neighbors + 前端 fetchNeighbors + .article-neighbors 导航卡片 |
| 前端: 代码块复制按钮 | ✅ | MarkdownPreview.vue copyCode() 已实现 |
| 前端: 回到顶部按钮 | ✅ | ArticleDetailView.vue showBackToTop + .back-to-top |
| 前端: 沉浸阅读模式 | ✅ | ArticleDetailView.vue toggleImmersive() + .detail-immersive CSS，Esc 退出 |

### 3.2 首页内容分发优化
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/articles sort=featured/recommended | ✅ | ArticleMapper.xml 已实现 hot/featured 排序 |
| 前端: 首页 Tab 切换（最新/热门/精选） | ✅ | HomeView.vue 已实现 |
| 前端: 精选文章⭐角标 | ✅ | ArticleFeed.vue post-featured-badge + global.css 样式 |
| 前端: 侧边栏"本周精选"小组件 | ✅ | HomeSidebar.vue featured prop + 精选列表，HomeView 传 featuredArticles |

### 3.3 搜索能力增强
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_article FULLTEXT INDEX（ngram） | ❌ | schema.sql 无全文索引 |
| 后端: 搜索 FULLTEXT + 回退 LIKE | ❌ | 未实现 |
| 前端: 关键词高亮 | ✅ | ArticleFeed.vue highlightKeyword prop + highlightHtml()，SearchView 传入 keyword |
| 前端: 搜索 URL 保留 tab 参数 | ❌ | 未确认 |
| 前端: 无结果引导（热门关键词） | ❌ | 未确认 |

### 3.4 标签页与分类页
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/categories/{id}/articles | ✅ | CategoryController 已实现，复用 ArticleAppService |
| 后端: GET /api/tags/{id}/articles | ✅ | TagController 已实现，复用 ArticleAppService |
| 后端: GET /api/tags/hot | ✅ | TagController + TagAppService + MyBatisTagRepository 已实现 |
| 前端: /categories/:id 分类详情页 | ✅ | CategoryDetailView.vue + api/categories.js，路由 categoryDetail |
| 前端: /tags/:id 标签详情页 | ✅ | TagDetailView.vue + api/tags.js，路由 tagDetail |
| 前端: /explore 内容探索页 | ✅ | ExploreView.vue，路由 explore，展示分类网格+热门标签云 |

---

## Phase 4：社区互动深化

### 4.1 通知系统增强
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_announcement 表 | ✅ | schema.sql 已有 |
| 后端: GET /api/announcements/active | ✅ | AnnouncementController 已实现 |
| 后端: GET /api/notifications/stream（SSE） | ✅ | NotificationController.stream()，支持 query token |
| 后端: GET/POST/PUT/DELETE /api/admin/announcements | ✅ | AdminController 已添加公告 CRUD + publish/unpublish |
| 前端: Header 通知角标 SSE 实时更新 | ✅ | SiteHeader.vue 用 subscribeNotificationStream + 60s 兜底轮询 |
| 前端: 通知列表"系统通知"Tab | ✅ | NotificationsView.vue 新增 system filter + 公告列表 |
| 前端: 首页公告横幅 | ✅ | HomeView.vue 公告横幅，支持按 id 关闭（存 localStorage） |
| 前端: 后台公告管理页 /admin/announcements | ✅ | AdminAnnouncementsView.vue，路由和导航已添加 |

### 4.2 评论体验优化
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_comment 新增 edited_at/edit_count | ✅ | schema.sql 已添加字段，CommentDO/Comment 已同步 |
| 后端: PUT /api/comments/{id}（编辑评论） | ✅ | CommentController 已实现，10分钟时限逻辑在 Comment.edit() |
| 后端: GET /api/articles/{id}/comments?sort=hot | ✅ | CommentAppService.normalizeSort 已支持 hot 参数 |
| 前端: 评论编辑功能（10分钟内） | ✅ | CommentRootItem.vue 添加 editCommentApi + 内联编辑框 + canEdit 逻辑 |
| 前端: 评论排序切换（最新/最热） | ✅ | CommentList.vue 已有排序 Tab（最新/最热） |
| 前端: 评论框未登录引导 | ✅ | CommentList.vue/CommentRootItem.vue 均有 loginModal.requireLogin 引导 |

### 4.3 关注与社交功能
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/users/{id}/followers | ✅ | UserController + FollowAppService.listFollowers() + UserFollowRepository.findFollowerUserIds() |
| 后端: GET /api/users/{id}/following | ✅ | UserController + FollowAppService.listUserFollowing() |
| 后端: GET /api/users/{id}/follow-status | ✅ | UserController + FollowAppService.getFollowStatus()，返回 followed/followedBack/mutual |
| 前端: 用户主页粉丝数可点击弹窗 | ✅ | UserProfileView.vue + UserProfileSummary stat-click 事件，openFollowDialog('followers') |
| 前端: 用户主页关注数可点击弹窗 | ✅ | UserProfileView.vue openFollowDialog('following')，Teleport 弹窗列表 |
| 前端: 互关标识 | ✅ | isMutualFollow computed + mutual-follow-badge 绿色标签，getFollowStatusApi 加载 |

---

## Phase 5：平台运营与数据能力

### 5.1 后台概览仪表盘增强
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/admin/stats 扩展（今日新增、7日趋势、分类分布、Top作者） | ❌ | AdminController 只有基础 /stats |
| 前端: 后台概览重构（趋势折线图、饼图、Top作者） | ❌ | 未实现 |

### 5.2 内容审核辅助
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_sensitive_word 表 | ✅ | schema.sql blog_sensitive_word 表已添加 |
| 后端: GET/POST/DELETE/PUT /api/admin/sensitive-words | ✅ | AdminController + SensitiveWordAppService + Mapper 完整实现 |
| 后端: POST /api/articles/validate 敏感词检测 | ✅ | ArticleController 已有 /validate 接口 |
| 前端: 后台敏感词管理页 /admin/sensitive-words | ✅ | AdminSensitiveWordsView.vue 已完整实现 |
| 前端: 文章待审核 Tab | ✅ | AdminArticlesView.vue REVIEW_PENDING Tab 已实现 |
| 前端: 后台文章快速预览抽屉 | ✅ | AdminArticlesView.vue 预览按钮+Teleport 抽屉+动画 |

### 5.3 用户增长工具
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_invite_code 表 | ✅ | schema.sql 已有 blog_invite_code 表 |
| 后端: POST /api/invite-codes/generate | ✅ | InviteCodeController.generate() |
| 后端: GET /api/invite-codes/my | ✅ | InviteCodeController.myList() |
| 后端: POST /api/auth/register 支持 inviteCode | ✅ | RegisterRequest 已有 inviteCode 字段 |
| 后端: GET /api/admin/invite-codes | ✅ | InviteCodeController.adminPage() |
| 前端: 设置页"邀请好友"Tab | ✅ | ProfileSettingsView.vue 邀请好友Tab+列表+复制按钮 |
| 前端: 注册页支持 invite 参数 | ✅ | AuthView.vue inviteCode computed from route.query.invite，session.js透传 |

### 5.4 数据导出
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/users/me/export/articles（CSV） | ✅ | UserController.exportMyArticles() + UserAppService.exportMyArticlesCsv() |
| 后端: GET /api/admin/export/articles（CSV） | ✅ | AdminController.exportArticles() + AdminAppService.exportArticlesCsv() |
| 后端: GET /api/admin/export/users（CSV） | ✅ | AdminController.exportUsers() + AdminAppService.exportUsersCsv() |
| 前端: Dashboard"导出文章"按钮 | ✅ | DashboardView.vue exportMyArticles() + 导出 CSV 按钮 |
| 前端: 后台文章/用户列表导出 CSV | ✅ | AdminArticlesView.vue + AdminUsersView.vue 导出 CSV 按钮（已有） |

---

## 实施记录

| 日期 | 完成条目 |
|---|---|
| 2026-04-30 | 初始状态盘点，创建 progress.md |
| 2026-04-30 | Phase 3.4 后端：CategoryController GET /{id}/articles，TagController GET /{id}/articles + GET /hot，TagRepository.findHot()，TagMapper.xml selectHot |
| 2026-04-30 | Phase 4.2 后端：schema.sql blog_comment 新增 edited_at/edit_count，Comment.edit() 10分钟时限逻辑，CommentAppService.editComment()，CommentController PUT /comments/{id}，CommentDO/CommentDTO/CommentAssembler/CommentMapper.xml 同步更新 |
| 2026-04-30 | Phase 3.4 前端：CategoryDetailView.vue、TagDetailView.vue、ExploreView.vue，api/categories.js、api/tags.js，路由 /explore、/categories/:id、/tags/:id |
| 2026-04-30 | Phase 4.2 前端：CommentRootItem.vue 添加编辑功能（内联编辑框、canEdit 逻辑、已编辑标记），transformers.js 添加 canEdit/editCount 字段，api/comments.js 添加 editCommentApi，后端 canEdit 权限方法 + CommentResponse/CommentDTO/RestDtoMapper 同步 |
| 2026-04-30 | Phase 4.3 前端：UserProfileView.vue 粉丝/关注弹窗（openFollowDialog + Teleport modal + getUserFollowersApi/getUserFollowingListApi）、互关标识（isMutualFollow + mutual-follow-badge），UserProfileSummary.vue 支持 stat-click 事件，following.js 新增 API |
| 2026-04-30 | Phase 4.1 后端：JwtAuthenticationInterceptor 支持 query token（SSE 用），NotificationController.stream() SSE 接口，AdminController 公告 CRUD + publish/unpublish |
| 2026-04-30 | Phase 4.1 前端：notifications.js 添加 subscribeNotificationStream + getActiveAnnouncementsApi，SiteHeader.vue 改用 SSE（兜底轮询），NotificationsView.vue 新增"系统通知"Tab，HomeView.vue 公告横幅，AdminAnnouncementsView.vue 后台公告管理页，路由和导航更新 |
| 2026-04-30 | Phase 1.1 后端：User.changeEmail()，UserAppService.changeEmail()（验证密码+查重），UserController POST /api/users/me/email |
| 2026-04-30 | Phase 1.1 前端：auth.js 添加 changeEmailApi，ProfileSettingsView.vue 安全 Tab 新增绑定/换邮箱表单 |
| 2026-04-30 | Phase 3.1 前端：ArticleDetailView.vue 新增分享菜单（复制链接+微博）、沉浸阅读模式（Esc退出）；分享菜单 document click 关闭 |
| 2026-04-30 | Phase 3.1 后端：ArticleMapper.xml selectPrevPublished/selectNextPublished，ArticleMapper 接口添加方法，ArticleRepository 接口+MyBatisArticleRepository 实现，ArticleAppService.getArticleNeighbors()，ArticleController GET /articles/{id}/neighbors |
| 2026-04-30 | Phase 3.1 前端：articles.js getArticleNeighborsApi，ArticleDetailView.vue fetchNeighbors()+watch触发，模板 .article-neighbors 上下篇导航卡片 + CSS |
| 2026-04-30 | Phase 3.2 前端：ArticleFeed.vue post-featured-badge 角标，global.css featured-post 边框增强+角标样式，HomeSidebar.vue 本周精选小组件（featured prop），HomeView.vue 传 featuredArticles |
| 2026-04-30 | Phase 3.3 前端：ArticleFeed.vue highlightKeyword prop + highlightHtml()（正则高亮+XSS safe）+ .search-highlight 样式，SearchView.vue 传入 :highlight-keyword="keyword" |
| 2026-04-30 | Phase 5.2 DB：schema.sql blog_sensitive_word 表；后端已有完整实现（SensitiveWordAppService+Mapper+AdminController）；前端：AdminArticlesView.vue 快速预览抽屉（previewId/openPreview/Teleport drawer+CSS动画） |
| 2026-04-30 | Phase 5.3 前端：auth.js generateInviteCodeApi+getMyInviteCodesApi，session.js register透传inviteCode，AuthView.vue inviteCode computed，ProfileSettingsView.vue 邀请好友Tab（生成/复制/状态展示） |
| 2026-04-30 | Phase 5.4 后端：UserController GET /me/export/articles+UserAppService.exportMyArticlesCsv()，AdminController GET /export/articles+/export/users（CSV）；前端：DashboardView.vue exportMyArticles()+"导出CSV"按钮 |
| 2026-04-30 | Phase 2.3 前端：DashboardColumnsView.vue（专栏列表+创建/编辑弹窗+文章管理抽屉+选择文章面板），api/columns.js（创作者 CRUD API），路由 dashboardColumns，CreatorSidebar.vue 添加"我的专栏"导航，DashboardView.vue 概览页添加"我的专栏"统计卡片+管理入口 |

