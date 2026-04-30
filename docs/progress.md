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
| 后端: POST /api/users/me/email（绑定邮箱） | ❌ | UserController 无此接口 |
| 后端: GET /api/users/me/security | ✅ | UserController 已实现 |
| 前端: /auth/forgot-password 页面 | ✅ | ForgotPasswordView.vue 已存在 |
| 前端: /auth/reset-password 页面 | ✅ | ResetPasswordView.vue 已存在 |
| 前端: 设置页账号安全 Tab（改密码、绑定邮箱、最近登录） | ✅ | ProfileSettingsView.vue 已有 security tab |
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
| 前端: /dashboard/columns 页面 | ❌ | 路由不存在 |

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
| 前端: 正文区最大宽度 720px | ❌ | 未确认 |
| 前端: 阅读进度条 | ❌ | 未实现 |
| 前端: 分享按钮（复制链接、微博、生成图片）| ❌ | 未实现 |
| 前端: 文章底部上一篇/下一篇 | ❌ | 未实现 |
| 前端: 代码块复制按钮 | ❌ | 未实现 |
| 前端: 回到顶部按钮 | ❌ | 未实现 |
| 前端: 沉浸阅读模式 | ❌ | 未实现 |

### 3.2 首页内容分发优化
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/articles sort=featured/recommended | ✅ | ArticleMapper.xml 已实现 hot/featured 排序 |
| 前端: 首页 Tab 切换（最新/热门/精选） | ✅ | HomeView.vue 已实现 |
| 前端: 精选文章⭐角标 | ❌ | 未确认 |
| 前端: 侧边栏"本周精选"小组件 | ❌ | 未确认 |

### 3.3 搜索能力增强
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_article FULLTEXT INDEX（ngram） | ❌ | schema.sql 无全文索引 |
| 后端: 搜索 FULLTEXT + 回退 LIKE | ❌ | 未实现 |
| 前端: 关键词高亮 | ❌ | 未实现 |
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
| DB: blog_announcement 表 | ❌ | schema.sql 无此表 |
| 后端: GET /api/announcements/active | ❌ | 无此接口 |
| 后端: GET /api/notifications/stream（SSE） | ❌ | 无此接口 |
| 后端: GET/POST/PUT/DELETE /api/admin/announcements | ❌ | AdminController 无此接口 |
| 前端: Header 通知角标 SSE 实时更新 | ❌ | 未实现 |
| 前端: 通知列表"系统通知"Tab | ❌ | 未实现 |
| 前端: 首页公告横幅 | ❌ | 未实现 |
| 前端: 后台公告管理页 /admin/announcements | ❌ | 无此页面 |

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
| 后端: GET /api/users/{id}/followers | ❌ | UserController 无此接口 |
| 后端: GET /api/users/{id}/following | ❌ | UserController 无此接口（只有 /me/following）|
| 后端: GET /api/users/{id}/follow-status | ❌ | UserController 无此接口 |
| 前端: 用户主页粉丝数可点击弹窗 | ❌ | 未实现 |
| 前端: 用户主页关注数可点击弹窗 | ❌ | 未实现 |
| 前端: 互关标识 | ❌ | 未实现 |

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
| DB: blog_sensitive_word 表 | ❌ | schema.sql 无此表 |
| 后端: GET/POST/DELETE/PUT /api/admin/sensitive-words | ❌ | 无此接口 |
| 后端: POST /api/articles/validate 敏感词检测 | ✅ | ArticleController 已有 /validate 接口 |
| 前端: 后台敏感词管理页 /admin/sensitive-words | ❌ | 无此页面 |
| 前端: 文章待审核 Tab | ❌ | 未实现 |
| 前端: 后台文章快速预览抽屉 | ❌ | 未实现 |

### 5.3 用户增长工具
| 条目 | 状态 | 备注 |
|---|---|---|
| DB: blog_invite_code 表 | ❌ | schema.sql 无此表 |
| 后端: POST /api/users/me/invite-codes | ❌ | 无此接口 |
| 后端: GET /api/users/me/invite-codes | ❌ | 无此接口 |
| 后端: POST /api/auth/register 支持 inviteCode | ❌ | 未扩展 |
| 后端: GET /api/admin/invite-codes | ❌ | 无此接口 |
| 前端: 设置页"邀请好友"Tab | ❌ | 未实现 |
| 前端: 注册页支持 invite 参数 | ❌ | 未实现 |

### 5.4 数据导出
| 条目 | 状态 | 备注 |
|---|---|---|
| 后端: GET /api/users/me/export/articles（ZIP） | ❌ | 无此接口 |
| 后端: GET /api/admin/export/articles（CSV） | ❌ | 无此接口 |
| 后端: GET /api/admin/export/users（CSV） | ❌ | 无此接口 |
| 前端: Dashboard"导出文章"按钮 | ❌ | 未实现 |
| 前端: 后台文章/用户列表导出 CSV | ❌ | 未实现 |

---

## 实施记录

| 日期 | 完成条目 |
|---|---|
| 2026-04-30 | 初始状态盘点，创建 progress.md |
| 2026-04-30 | Phase 3.4 后端：CategoryController GET /{id}/articles，TagController GET /{id}/articles + GET /hot，TagRepository.findHot()，TagMapper.xml selectHot |
| 2026-04-30 | Phase 4.2 后端：schema.sql blog_comment 新增 edited_at/edit_count，Comment.edit() 10分钟时限逻辑，CommentAppService.editComment()，CommentController PUT /comments/{id}，CommentDO/CommentDTO/CommentAssembler/CommentMapper.xml 同步更新 |
| 2026-04-30 | Phase 3.4 前端：CategoryDetailView.vue、TagDetailView.vue、ExploreView.vue，api/categories.js、api/tags.js，路由 /explore、/categories/:id、/tags/:id |
| 2026-04-30 | Phase 4.2 前端：CommentRootItem.vue 添加编辑功能（内联编辑框、canEdit 逻辑、已编辑标记），transformers.js 添加 canEdit/editCount 字段，api/comments.js 添加 editCommentApi，后端 canEdit 权限方法 + CommentResponse/CommentDTO/RestDtoMapper 同步 |

