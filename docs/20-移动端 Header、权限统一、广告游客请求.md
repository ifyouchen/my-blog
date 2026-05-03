# 下一批体验优化计划：移动端 Header、权限统一、广告游客请求

## Summary

本轮继续收敛“看起来粗糙”的体验点：移动端顶部导航减负、需要登录的页面统一前置拦截、广告组件避免游客态无意义鉴权请求。只改前端，不改后端接口、数据库或管理后台业务逻辑；基于当前已完成的编辑器登录保护继续扩展。

## Key Changes

- **移动端 Header 减负**
  - 在 `SiteHeader.vue` 增加移动端搜索入口：小屏隐藏顶部内联搜索框，改为搜索图标按钮。
  - 点击搜索图标打开全屏/顶部搜索面板，包含大输入框、取消按钮、提交按钮；提交后跳转 `/search?keyword=...` 并关闭面板。
  - 小屏隐藏顶部“写文章”按钮，保留抽屉菜单里的“写文章”，避免 390px 宽度拥挤。
  - 在 `global.css` 调整 `@media (max-width: 760px)` 下的 header grid、搜索面板、按钮尺寸，保证品牌、搜索图标、菜单按钮不挤压。

- **权限保护统一**
  - 在 `router/index.js` 将已有 `requiresAuth` 扩展到：
    - `/dashboard/overview`
    - `/dashboard/articles`
    - `/dashboard/favorites`
    - `/dashboard/columns`
    - `/settings/profile`
    - `/messages/:conversationId?`
    - `/notifications`
    - `/admin`
  - 复用现有路由守卫和 `useLoginModal`，未登录访问这些页面时取消导航、回到首页背景并弹登录框。
  - 登录成功后跳回原目标 URL。
  - `admin` 先只做登录保护，不做角色前置判断；管理员权限仍由现有页面/API 兜底，避免误挡合法登录态。

- **广告游客请求优化**
  - 在 `AdBanner.vue` 引入 `getToken` 或 `useSession` 判断登录态。
  - 游客态仍可请求公开广告列表 `getAdsApi(slotCode)`，但不请求 `getDismissedAdIdsApi()`。
  - 游客关闭广告只写本地当日隐藏，不调用 `dismissAdApi()`。
  - 游客广告曝光/点击若后端允许匿名则保留；若请求失败继续静默吞掉，不污染用户界面。
  - 登录态保留现有服务端 dismissed ids、关闭、当天不再提示逻辑。

- **文档记录**
  - 新增 `docs/20-移动端与权限体验优化计划.md`。
  - 文档记录本轮问题背景、改动范围、验收标准和测试步骤。
  - 不覆盖已有 `docs/18-*`、`docs/19-*`。

## Test Plan

- 执行 `frontend/npm run build`。
- 移动端 Header：
  - 390px 宽度访问首页，顶部只保留品牌、搜索图标、菜单按钮，不显示内联搜索框和顶部写文章按钮。
  - 点击搜索图标打开搜索面板，输入 `Redis` 提交后进入 `/search?keyword=Redis`。
  - 打开汉堡菜单，仍能看到“写文章”入口。
- 权限保护：
  - 清空 session 后访问 `/dashboard/articles`、`/settings/profile`、`/messages`、`/notifications`、`/admin`，均不能看到目标页主体，并出现登录弹窗。
  - 登录成功后能跳回原目标 URL。
  - 已登录访问上述页面不被拦截。
- 广告请求：
  - 游客态访问首页/搜索/文章详情，控制台不再出现 `/ads/dismissed-ids` 的“请先登录”业务错误。
  - 游客关闭广告后当日隐藏生效。
  - 登录态广告关闭和 dismissed ids 逻辑保持可用。

## Assumptions

- 本轮优先优化前端体验，不新增后端 API。
- 管理后台本轮只要求登录，不在路由层新增 `ADMIN` 角色判断。
- 移动端“写文章”入口统一放到抽屉菜单，不再占用顶部首行空间。
- 广告曝光/点击接口继续静默容错；游客态重点移除的是 dismissed ids 和 dismiss 写接口的鉴权错误。
