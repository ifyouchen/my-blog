# 业务日志打印重构 - 进度

## 目标

统一后端 Service 层日志格式：**谁在哪个地方，干了什么事，参数是什么，结果是什么**。

日志模板：`traceId=xxxx | 用户[{userId}][{nickname}] {action} | 入参({key=val}) | 结果({result}) | 耗时({elapsed}ms)`

## 完成情况

### BizLogHelper 工具类 ✅

新增 `shared/util/BizLogHelper.java`，提供静态方法：

| 方法 | 功能 | 示例输出 |
|---|---|---|
| `trace()` | traceId | `traceId=a1b2c3d4e5f6` |
| `who(id, nickname)` | 用户身份 | `用户[1001][张三]` |
| `who(id)` | 无昵称场景 | `用户[1001]` |
| `params(k, v, ...)` | 参数，自动脱敏 | `articleId=42, password=****, email=tes***@example.com` |
| `contentMeta(text)` | 大文本摘要 | `length=1024, preview="这是..."` |
| `safeEmail(email)` | 邮箱脱敏 | `tes***@example.com` |
| `safeIp(ip)` | IP 脱敏 | `192.168.*.*` |
| `mask()` | 敏感值掩码 | `****` |
| `result(msg)` | 通用结果 | `liked=true` |
| `created(key, id)` | 创建类结果 | `articleId=42, status=DRAFT` |
| `statusChanged(before, after)` | 状态变更 | `DRAFT -> PUBLISHED` |
| `loggedIn(userId)` | 登录结果 | `tokenIssued=true, userId=1001` |
| `paged(page, size, total)` | 分页结果 | `page=1, pageSize=10, total=42` |
| `elapsed(startMs)` | 耗时 | `耗时(45ms)` |

**日志模板更新：** 每个 Service 层日志方法现在统一以 `BizLogHelper.trace()` 作为第一个参数，格式为：
```
log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
    BizLogHelper.trace(),
    BizLogHelper.who(userId, nickname),
    "动作描述",
    BizLogHelper.params("key", val),
    BizLogHelper.result("result"),
    BizLogHelper.elapsed(_start));
```

**脱敏规则**：
- 密码/token/验证码/JWT → `****`
- 邮箱 → 前3字符 + `***` + `@域名`
- IP → 保留前两段，后两段变 `*`
- 大文本(>200字符) → 只输出 `length=N`
- 值含 `@` 自动识别为邮箱脱敏；值符合 `x.x.x.x` 格式自动识别为 IP 脱敏

### application.yml 日志配置 ✅

新增 log pattern 配置，所有日志均追加 `[%X{traceId}]`：

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] - %msg%n"
```

### Round 1: 核心业务 Service ✅

#### AuthAppService
- `register()` — 注册账号 | 入参(username, email) | 结果(userId)
- `login()` — 登录博客 | 入参(account) | 结果(tokenIssued=true, userId)

#### ArticleAppService
- `createArticle()` — 创建文章 | 入参(title, status) | 结果(articleId, status)
- `updateArticle()` — 更新文章 | 入参(articleId, title, status) | 结果(状态变更)
- `updateArticleStatus()` — 变更文章状态 | 入参(articleId, targetStatus) | 结果(状态变更)

### Round 2: 互动操作 Service ✅

#### CommentAppService
- `createComment()` — 创建评论 | 入参(articleId, parentId, content摘要) | 结果(commentId)
- `deleteComment()` — 删除评论 | 入参(commentId, articleId) | 结果(删除数量)
- `likeComment()` — 点赞评论 | 入参(commentId) | 结果(liked=true)
- `unlikeComment()` — 取消评论点赞 | 入参(commentId) | 结果(liked=false)

#### ArticleLikeAppService
- `likeArticle()` — 点赞文章 | 入参(articleId, title) | 结果(liked=true)
- `unlikeArticle()` — 取消文章点赞 | 入参(articleId) | 结果(liked=false)

#### FollowAppService
- `followUser()` — 关注用户 | 入参(targetUserId) | 结果(followed=true)
- `unfollowUser()` — 取消关注用户 | 入参(targetUserId) | 结果(followed=false)

### Round 3: 收藏/通知/用户/举报服务 ✅

#### ArticleFavoriteAppService
- `favoriteArticle()` — 收藏文章 | 入参(articleId, title) | 结果(favorited=true)
- `unfavoriteArticle()` — 取消收藏文章 | 入参(articleId) | 结果(favorited=false)

#### NotificationAppService
- `createNotification()` — 创建通知 | 入参(type, receiver) | 结果(notificationId)
- `markRead()` — 标记通知已读 | 入参(notificationId) | 结果(OK)
- `markAllRead()` — 标记全部已读 | (无参数) | 结果(OK)

#### UserAppService
- `updateProfile()` — 更新个人资料 | 入参(nickname, bio, website) | 结果(OK)
- `changePassword()` — 修改密码 | 入参(userId) | 结果(changed=true)
- `changeEmail()` — 更换邮箱 | 入参(email) | 结果(userId)
- `forgotPassword()` — 忘记密码 | 入参(email) | 结果(resetEmailSent=true)
- `resetPassword()` — 重置密码 | (无密码参数) | 结果(changed=true)

#### ReportAppService
- `createReport()` — 创建举报 | 入参(targetType, reasonType) | 结果(reportId)
- `resolveReport()` — 处理举报 | 入参(reportId, action) | 结果(resolved=true)

#### MessageAppService
- `sendMessage()` — 发送消息 | 入参(conversationId, contentLength) | 结果(messageId)
- `deleteConversation()` — 删除会话 | 入参(conversationId) | 结果(deleted=true)
- `markAllRead()` — 标记消息已读 | 入参(conversationId) | 结果(updated=N)

### Round 4: 管理后台 Service ✅

#### AdminAppService
- `disableUser()` — 禁用用户 | 入参(userId, reason) | 结果(状态变更)
- `offlineArticle()` — 下架文章 | 入参(articleId, reason) | 结果(状态变更)
- `deleteArticle()` — 删除文章 | 入参(articleId) | 结果(状态变更)
- `batchUpdateArticleStatus()` — 批量变更文章状态 | 入参(articleCount, status) | 结果(processedCount)
- `batchDeleteArticles()` — 批量删除文章 | 入参(articleCount) | 结果(processedCount)
- `deleteComment()` — 删除评论 | 入参(commentId) | 结果(deleted=true)
- `approveComment()` — 审核通过评论 | 入参(commentId) | 结果(approved=true)
- `rejectComment()` — 拒绝评论 | 入参(commentId) | 结果(rejected=true)
- `featureArticle()` — 设为精选 | 入参(articleId, title) | 结果(featured=true)
- `unfeatureArticle()` — 取消精选 | 入参(articleId, title) | 结果(featured=false)
- `updateUserStatus()` — 变更用户状态 | 入参(userId, status) | 结果(状态变更)

#### AnnouncementAppService
- `create()` — 创建公告 | 入参(title) | 结果(announcementId)
- `update()` — 更新公告 | 入参(id, title) | 结果(announcementId)
- `delete()` — 删除公告 | 入参(id) | 结果(deleted=true)
- `publish()` — 发布公告 | 入参(id) | 结果(published=true)
- `unpublish()` — 撤回公告 | 入参(id) | 结果(published=false)

#### AdAppService
- `create()` — 创建广告 | 入参(title, slotCode) | 结果(campaignId)
- `update()` — 更新广告 | 入参(id, title) | 结果(campaignId)
- `delete()` — 删除广告 | 入参(id) | 结果(deleted=true)
- `recordImpression()` — 记录广告曝光 | 入参(campaignId) | 结果(recorded=true)
- `recordClick()` — 记录广告点击 | 入参(campaignId) | 结果(recorded=true)
- `dismiss()` — 关闭广告 | 入参(campaignId) | 结果(dismissed=true)

#### InviteCodeAppService
- `generate()` — 生成邀请码 | (无参数) | 结果(codeId)
- `useCode()` — 使用邀请码 | (无参数) | 结果(used=true)
- `adminDelete()` — 删除邀请码 | 入参(id) | 结果(deleted=true)

#### ColumnAppService
- `subscribeColumn()` — 订阅专栏 | 入参(columnId) | 结果(OK)
- `unsubscribeColumn()` — 取消订阅专栏 | 入参(columnId) | 结果(OK)
- `addColumnArticle()` — 专栏添加文章 | 入参(columnId, articleId) | 结果(OK)
- `removeColumnArticle()` — 专栏移除文章 | 入参(columnId, articleId) | 结果(OK)
- `createMyColumn()` — 创建专栏 | 入参(title) | 结果(columnId)
- `updateMyColumn()` — 更新专栏 | 入参(columnId, title) | 结果(columnId)
- `deleteMyColumn()` — 删除专栏 | 入参(columnId) | 结果(OK)
- `addMyColumnArticle()` — 专栏添加文章 | 入参(columnId, articleId) | 结果(OK)
- `removeMyColumnArticle()` — 专栏移除文章 | 入参(columnId, articleId) | 结果(OK)
- `adminCreateColumn()` — 后台创建专栏 | 入参(title, authorId) | 结果(columnId)
- `adminUpdateColumn()` — 后台更新专栏 | 入参(columnId, title) | 结果(columnId)
- `adminDeleteColumn()` — 后台删除专栏 | 入参(columnId) | 结果(OK)

#### TopicAppService
- `adminCreateTopic()` — 后台创建专题 | 入参(title) | 结果(topicId)
- `adminUpdateTopic()` — 后台更新专题 | 入参(topicId, title) | 结果(topicId)
- `adminDeleteTopic()` — 后台删除专题 | 入参(topicId) | 结果(OK)
- `adminBindArticle()` — 专题绑定文章 | 入参(topicId, articleId) | 结果(OK)
- `adminUnbindArticle()` — 专题解绑文章 | 入参(topicId, articleId) | 结果(OK)

#### SensitiveWordAppService
- `create()` — 创建敏感词 | 入参(category) | 结果(wordId)
- `update()` — 更新敏感词 | 入参(id) | 结果(wordId)
- `delete()` — 删除敏感词 | 入参(id) | 结果(deleted=true)

#### SearchHistoryAppService
- `recordSearch()` — 记录搜索 | 入参(keyword) | 结果(recorded=true)
- `clearUserSearchHistory()` — 清空搜索历史 | (无参数) | 结果(cleared=true)

### Round 5: 异步事件监听器（监控日志）✅

#### NotificationEventListener
- `onArticleLiked()` — 处理点赞事件 | 入参(articleId, userId)
- `onArticleFavorited()` — 处理收藏事件 | 入参(articleId, userId)
- `onCommentCreated()` — 处理评论事件 | 入参(articleId, commentId)
- `onCommentLiked()` — 处理评论点赞事件 | 入参(commentId, userId)
- `onUserFollowed()` — 处理关注事件 | 入参(followerUserId, followingUserId)
- `onArticlePublished()` — 处理发布事件 | 入参(articleId, authorId)

#### ArticleStatsEventListener
- `onArticleViewed/Liked/Unliked/Favorited/Unfavorited()` — 文章计数事件
- `onCommentCreated/Deleted/Liked/Unliked()` — 评论计数事件

### 缓存/定时刷新工具类

#### HomeStatsAppService
- `init()` — 初始化首页缓存 | 结果(OK)
- `refreshStats()` — 定时刷新首页缓存 | 入参(articles, authors, columns)

#### RankingAppService
- `listArticleRankings()` — 查询文章排行榜 | 入参(limit) | 结果(size=N)
- `listAuthorRankings()` — 查询作者排行榜 | 入参(limit, currentUserId) | 结果(size=N)

## 日志示例

```
2026-05-02 21:00:00.123 [http-nio-8080-exec-1] INFO  c.m.a.s.AuthAppService [traceId=abc123] - traceId=abc123 | 用户[1001][张三] 登录博客 | 入参(account=tes***@example.com) | 结果(tokenIssued=true, userId=1001) | 耗时(45ms)
2026-05-02 21:00:01.456 [http-nio-8080-exec-2] INFO  c.m.a.s.ArticleAppService [traceId=def456] - traceId=def456 | 用户[1001][张三] 创建文章 | 入参(title=Spring Boot入门, status=PUBLISHED) | 结果(articleId=42, status=PUBLISHED) | 耗时(120ms)
2026-05-02 21:00:02.789 [http-nio-8080-exec-3] INFO  c.m.a.s.ArticleLikeAppService [traceId=ghi789] - traceId=ghi789 | 用户[1002] 点赞文章 | 入参(articleId=42, title=Spring Boot入门) | 结果(liked=true) | 耗时(8ms)
```

## 涉及文件

### 新增
- `shared/util/BizLogHelper.java` — 日志格式化工具类

### 修改（共 33 个文件）
- `resources/application.yml` — 日志 pattern 增加 traceId 输出
- `application/service/AuthAppService.java` — login/register
- `application/service/ArticleAppService.java` — create/update/status
- `application/service/CommentAppService.java` — 评论 CRUD
- `application/service/ArticleLikeAppService.java` — 点赞
- `application/service/FollowAppService.java` — 关注
- `application/service/ArticleFavoriteAppService.java` — 收藏
- `application/service/NotificationAppService.java` — 通知
- `application/service/UserAppService.java` — 用户资料/密码/邮箱
- `application/service/ReportAppService.java` — 举报
- `application/service/MessageAppService.java` — 私信
- `application/service/AdminAppService.java` — 后台管理操作
- `application/service/AnnouncementAppService.java` — 公告 CRUD
- `application/service/AdAppService.java` — 广告
- `application/service/InviteCodeAppService.java` — 邀请码
- `application/service/ColumnAppService.java` — 专栏
- `application/service/TopicAppService.java` — 专题
- `application/service/SensitiveWordAppService.java` — 敏感词
- `application/service/SearchHistoryAppService.java` — 搜索历史
- `application/service/RegisterEmailCodeAppService.java` — 注册验证码
- `application/service/RankingAppService.java` — 排行榜
- `application/service/HomeStatsAppService.java` — 首页统计缓存
- `application/service/HomeBootstrapAppService.java` — 首页引导
- `application/service/DashboardAppService.java` — 创作者工作台
- `application/listener/NotificationEventListener.java` — 通知事件监听
- `application/listener/ArticleStatsEventListener.java` — 统计事件监听

## 编译验证

- `mvn compile` — ✅ BUILD SUCCESS (all services and listeners)
