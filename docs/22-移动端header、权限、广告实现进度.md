# 22 - 移动端 Header、权限、广告实现进度

## 实施日期

2026-05-03

## 实施依据

- `docs/20-移动端 Header、权限统一、广告游客请求.md` — 背景与验收标准
- `docs/21.移动端header、权限、广告实现步骤.md` — 详细实施步骤

## 变更文件清单

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/components/SiteHeader.vue` | 修改 |
| `frontend/src/router/index.js` | 修改 |
| `frontend/src/components/AdBanner.vue` | 修改 |
| `frontend/src/styles/global.css` | 修改 |

## 1. 移动端 Header 减负 — 已完成

### 改动内容

**SiteHeader.vue：**

1. 新增 `mobileSearchOpen` 响应式状态。
2. 新增 `openMobileSearch()` 方法：打开搜索面板，关闭用户菜单/通知菜单/抽屉。
3. 新增 `closeMobileSearch()` 方法：关闭搜索面板。
4. 修改 `submitSearch()`：搜索前 trim 关键词，关闭搜索面板和抽屉后跳转。
5. 在 header actions 中新增移动端搜索按钮（`mobile-search-toggle`），包含放大镜 SVG 图标。
6. 给顶部写文章按钮新增 `desktop-write-action` 类，小屏时隐藏。
7. 修复抽屉内"写文章"按钮的点击 bug：原来 `@click="writeArticle; mobileMenuOpen = false"` 没有实际调用 `writeArticle()`，改为 `@click="mobileMenuOpen = false; writeArticle()"`。
8. 新增移动端搜索面板模板：使用 `Teleport to="body"`，包含遮罩、搜索表单（取消/输入框/搜索按钮），输入框和按钮均有 `data-testid`。

**global.css：**

1. 新增 `.mobile-search-toggle` 样式（默认隐藏）。
2. 新增移动端搜索面板相关样式：`.mobile-search-overlay`、`.mobile-search-panel`、`.mobile-search-form`、`.mobile-search-cancel`、`.mobile-search-input`、`.mobile-search-submit`。
3. 在 `@media (max-width: 760px)` 中：
   - 隐藏 `.search-box` 和 `.desktop-write-action`。
   - 显示 `.mobile-search-toggle`。

### 验收要点

- 390px 宽度：顶部只显示品牌、搜索图标、菜单按钮。
- 点击搜索图标打开浮层搜索面板，输入关键词提交后跳转 `/search?keyword=...`。
- 抽屉内"写文章"点击后可正常触发登录或进入编辑器。

---

## 2. 权限保护统一 — 已完成

### 改动内容

**router/index.js：**

1. 给以下路由新增 `meta.requiresAuth = true`：
   - `/messages/:conversationId?`
   - `/notifications`
   - `/dashboard/overview`
   - `/dashboard/articles`
   - `/dashboard/favorites`
   - `/dashboard/columns`
   - `/settings/profile`
   - `/admin`（父路由）
2. `/admin` 只做登录保护，不加角色判断；管理员权限仍由页面/API 兜底。
3. 路由守卫弹窗文案从编辑器相关改为通用版本：
   - title: `登录后继续访问`
   - message: `登录后可以继续访问个人内容、通知、私信和管理功能。`
   - actionText: `登录并继续`

### 验收要点

- 未登录访问上述页面时，被重定向回首页并弹出登录框。
- 登录成功后跳回原目标 URL。
- 已登录访问不被拦截。

---

## 3. 广告游客请求优化 — 已完成

### 改动内容

**AdBanner.vue：**

1. 引入 `getToken` from `@/api/http`。
2. 新增 `isAuthenticated = () => Boolean(getToken())` 函数。
3. 修改 `fetchDismissedIds()`：游客态直接 `serverDismissedIds.value = new Set()` 并 return，不调用 `getDismissedAdIdsApi()`。
4. 修改 `dismissSession()`：游客只做本地 Set 记录，不调用 `dismissAdApi()`。
5. 修改 `dismissToday()`：游客只写 localStorage 当天隐藏，不调用 `dismissAdApi()`。
6. 曝光/点击接口保持原有 `.catch(() => {})` 静默容错。

### 验收要点

- 游客访问首页/搜索/文章详情，Network 不再出现 `/ads/dismissed-ids` 请求。
- 游客关闭广告后当日隐藏仍生效（本地 localStorage）。
- 登录态 dismissed ids、关闭逻辑保持不变。

---

## 构建验证

```
cd frontend && npm run build
```

构建成功，无报错。

---

## 后续可选优化

- 移动端搜索面板打开时自动聚焦输入框（已加 `autofocus`，部分浏览器需手动 focus）。
- admin 路由后续可按需增加角色判断（当前只做登录保护）。
