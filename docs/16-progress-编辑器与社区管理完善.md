# 编辑器与社区管理功能完善 - 进度

## 完成情况

### Step 1: 评论审核队列（端到端） ✅
- [x] Comment.java - 修改 create() 初始状态为 PENDING（由 PUBLISHED 改为 PENDING）
- [x] CommentRepository.java - 管理查询已支持 status 过滤（findAdminPage 已有 status 参数）
- [x] CommentAppService.java - 新增 approveComment()/rejectComment()
- [x] AdminController.java - 新增 PUT /comments/{id}/approve、PUT /comments/{id}/reject 端点
- [x] CommentMapper.xml - 管理查询 SQL 已支持 status 过滤
- [x] 前端 AdminCommentsView.vue - 增加状态筛选、状态列、通过/拒绝操作按钮
- [x] 前端路由 - 已有 /admin/comments 路由

### Step 2: 文章下架原因展示 + 风控标记 ✅
- [x] ArticleDTO.java - 添加 offlineReason, warnFlag 字段
- [x] ArticleAssembler.java - 映射 offlineReason 和 warnFlag
- [x] ArticleAppService.java - 新增 applyWarnFlag()，在创建/更新/发布文章时检测敏感词并设置 warnFlag
- [x] ArticleResponse.java / RestDtoMapper.java - 映射 offlineReason 和 warnFlag
- [x] AdminAppService.java - 文章列表增加 disableReason 字段输出
- [x] AdminArticlesView.vue - 已有 warnFlag 标记列（status-pill review-pending）

### Step 3: 用户禁言/封禁增强 ✅
- [x] AdminUsersView.vue - 新增禁用原因输入弹窗、显示禁用原因列
- [x] admin.js - 新增 disableAdminUserApi
- [x] AdminAppService.java - getUsers 返回 disableReason 字段

### Step 4: 操作日志细化 ✅
- [x] 评论审核（approve/reject）已有操作日志记录（AdminController）
- [x] 用户禁言已有操作日志记录（AdminModerationController）
- [x] 用户状态变更已有操作日志（AdminController.updateUserStatus）
- [x] 文章状态变更已有操作日志（AdminController.updateArticleStatus）

### Step 5: SEO 预览增强 ✅
- [x] EditorView.vue - 新增 Google 搜索预览模拟卡片（蓝色标题、绿色 URL、灰色描述）
- [x] EditorView.vue - 新增 SEO 健康检查面板（6 项：标题/描述/Slug/小标题/配图/内容长度）
- [x] EditorView.vue - 新增关键词密度统计标签，按出现频率排序展示 Top 10

## 涉及文件清单

### 后端
- `domain/model/aggregate/Comment.java` — 初始状态 PENDING
- `application/service/CommentAppService.java` — 新增 approveComment/rejectComment
- `application/service/AdminAppService.java` — getComments 支持 status 参数, getUsers 返回 disableReason
- `application/service/ArticleAppService.java` — 新增 applyWarnFlag() 方法
- `application/dto/ArticleDTO.java` — 新增 offlineReason, warnFlag
- `application/assembler/ArticleAssembler.java` — 映射新字段
- `interfaces/rest/controller/AdminController.java` — 评论审核端点
- `interfaces/rest/dto/response/ArticleResponse.java` — 新增 offlineReason, warnFlag
- `interfaces/rest/mapper/RestDtoMapper.java` — 映射新字段

### 前端
- `src/views/admin/AdminCommentsView.vue` — 状态筛选/展示、通过/拒绝操作
- `src/views/admin/AdminUsersView.vue` — 禁用原因输入框、列表展示原因
- `src/api/admin.js` — 新增 approve/reject/disable API
- `src/styles/global.css` — 新增 status-pill.success 样式
