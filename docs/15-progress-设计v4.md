# 私信功能开发进度 v4

## 总览

| Phase | 描述 | 状态 |
|-------|------|------|
| Phase 1 | 后端 Domain 层（值对象、聚合根、仓储接口） | ✅ 完成 |
| Phase 2 | 后端 Infrastructure 层（持久化） | ✅ 完成 |
| Phase 3 | 后端 Application + Interface 层（服务 + 控制器 + SSE） | ✅ 完成 |
| Phase 4 | 前端 API + 路由 | ⏳ 进行中 |
| Phase 5 | 前端 UI 页面 | ⬜ 待开始 |
| Phase 6 | 前端入口 + SSE 集成 | ✅ 完成 |
| Phase 7 | 打磨 | ⬜ 待开始 |
| Phase 7 | 打磨 | ⬜ 待开始 |

---

## Phase 1：后端 Domain 层 ✅

| 文件 | 状态 |
|------|------|
| `domain/model/valueobject/ConversationId.java` | ✅ |
| `domain/model/valueobject/MessageId.java` | ✅ |
| `domain/model/aggregate/Conversation.java` | ✅ |
| `domain/model/aggregate/Message.java` | ✅ |
| `domain/repository/ConversationRepository.java` | ✅ |
| `domain/repository/MessageRepository.java` | ✅ |
| `mvn compile` | ✅ |

## Phase 2：后端 Infrastructure 层 ✅

| 文件 | 状态 |
|------|------|
| `entity/ConversationDO.java` | ✅ |
| `entity/MessageDO.java` | ✅ |
| `converter/ConversationPersistenceConverter.java` | ✅ |
| `converter/MessagePersistenceConverter.java` | ✅ |
| `mapper/ConversationMapper.java` | ✅ |
| `mapper/MessageMapper.java` | ✅ |
| `mapper/ConversationMapper.xml` | ✅ |
| `mapper/MessageMapper.xml` | ✅ |
| `repository/MyBatisConversationRepository.java` | ✅ |
| `repository/MyBatisMessageRepository.java` | ✅ |
| `mvn compile` | ✅ |

## Phase 3：后端 Application + Interface 层 ✅

| 文件 | 状态 |
|------|------|
| `dto/ConversationDTO.java` | ✅ |
| `dto/MessageDTO.java` | ✅ |
| `request/CreateConversationRequest.java` | ✅ |
| `request/SendMessageRequest.java` | ✅ |
| `service/MessageAppService.java` | ✅ |
| `controller/MessageController.java` | ✅ |
| `mvn compile` | ✅ |

## Phase 4：前端 API + 路由 ✅

| 文件 | 状态 |
|------|------|
| `src/api/messages.js` | ✅ |
| `src/router/index.js` 路由 | ✅ |
| `npm run build` | ✅ |

## Phase 5：前端 UI 页面 ✅

| 文件 | 状态 |
|------|------|
| `MessagesView.vue`（含列表、详情、输入一体） | ✅ |

## Phase 6：前端入口 + SSE 集成 ✅

- [x] `SiteHeader.vue` 私信入口（私信图标 + 未读红点 + SSE 订阅未读计数）
- [x] 用户主页"发私信"按钮（点击创建会话并跳转私信页面）
- [x] 数据库 DDL `blog_conversation` + `blog_message` 表追加至 `schema.sql`
- [x] `mvn compile` + `npm run build` 均通过

## Phase 7：打磨 ⬜

- [ ] 空状态、加载状态、错误处理
- [ ] 消息发送中状态
- [ ] 下拉加载更多
- [ ] 删除会话
