# QA-T1 / QA-T2：联调与测试方案

> **负责人**：AI-QA-1 / AI-QA-2
> **阶段**：跨 Phase 联调
> **状态**：✅ 设计完成

---

## 1. QA-T1 接口回归

### 1.1 测试覆盖矩阵

| 模块 | 接口 | 幂等验证 | 边界值 | 并发 |
|------|------|---------|-------|------|
| P1 | 经验发放 | ✅ 重复事件 10 次 | 日上限边界 | ✅ |
| P1 | 等级升级 | — | 跨级升级 | — |
| P1 | 规则 CRUD | — | 参数枚举校验 | — |
| P2 | 积分入账 | ✅ 重复 bizNo | 负分扣减 | ✅ |
| P2 | 签到 | ✅ 同日重复 | 连续/断签 | — |
| P2 | 拉新 | ✅ 重复触发 | 风控拦截 | — |
| P2 | 充值回调 | ✅ 重复 20 次 | 验签失败 | ✅ |
| P3 | 解锁 | ✅ 100 并发 | 余额不足 | ✅ |
| P3 | 分账 | — | 守恒验证 | — |

---

### 1.2 关键场景用例

#### 场景 A：经验发放幂等

```
前置：用户 userId=1001 存在，growth_rule_config LIKE/ACTOR 已配置
步骤：
  1. 发布 ArticleLikedEvent(eventId=evt-001, actorUserId=1001, authorUserId=2003, sourceId=2005) 10 次
  2. 查询 event_consume_record WHERE event_id='evt-001'
  3. 查询 user_exp_journal WHERE idempotent_key LIKE 'LIKE:ACTOR:1001:2005:evt-001'
  4. 查询 user_growth_account WHERE user_id=1001
预期：
  event_consume_record 仅 1 条 SUCCESS
  user_exp_journal 仅 1 条
  current_exp 恰好增加 2（LIKE/ACTOR exp_amount）
  author current_exp 恰好增加 5（LIKE/AUTHOR exp_amount）
```

#### 场景 B：积分入账幂等

```
前置：用户 userId=1001 存在，balance=100
步骤：
  1. 调用 addPoints(1001, 50, "SIGN_IN", "signin-1001-20260515") 20 次（并发或串行）
预期：
  user_point_journal 仅 1 条 biz_no=signin-1001-20260515
  balance = 150（仅增加一次）
```

#### 场景 C：解锁扣分幂等（100 并发）

```
前置：用户 balance=200，文章 articleId=2005 unlock_point_price=50，need_unlock=1
步骤：
  1. 100 并发线程同时调用 POST /api/articles/2005/unlock（userId=1001）
预期：
  article_unlock_order 仅 1 条 SUCCESS（uk_user_article 保证）
  article_unlock_relation 仅 1 条
  revenue_share_journal 仅 1 条
  user_point_journal 仅 1 条 delta=-50
  balance = 150
```

#### 场景 D：充值回调幂等

```
前置：payOrderNo=PAY-001，userId=1001，amountFen=10000
步骤：
  1. 发送 POST /api/open/pay/recharge/callback 20 次（携带相同 payOrderNo）
预期：
  recharge_order 仅 1 条 SUCCESS，notify_count=20
  user_point_journal 仅 1 条 delta=100
  balance 仅增加 100
```

#### 场景 E：分账守恒

```
步骤：
  1. 随机选取 10 笔解锁订单
  2. 对每笔订单执行：
     SELECT platform_points + author_points AS sum_check,
            total_points
     FROM revenue_share_journal
     WHERE order_no = ?
预期：sum_check = total_points（全部 10 笔）
```

---

### 1.3 Postman/Apifox 集合结构

```
Growth & Points API Tests/
├── Phase 1 - Experience
│   ├── POST trigger LIKE event (幂等×10)
│   ├── GET /api/growth/me
│   └── GET /api/admin/growth/rules
├── Phase 2 - Points
│   ├── POST /api/points/sign-in (幂等×2)
│   ├── GET /api/points/sign-in/calendar
│   ├── POST /api/open/invite/reward/trigger (幂等×3)
│   └── POST /api/open/pay/recharge/callback (幂等×20)
├── Phase 3 - Unlock
│   ├── GET /api/articles/{id}/unlock-status
│   ├── POST /api/articles/{id}/unlock (幂等×5)
│   └── GET /api/admin/revenue-shares
└── Error Cases
    ├── 积分不足解锁 → 400
    ├── 重复签到 → 400
    └── 验签失败回调 → 400
```

---

## 2. QA-T2 并发与一致性测试

### 2.1 测试场景矩阵

| 场景 | 并发数 | 工具 | 验收指标 |
|------|--------|------|---------|
| 同用户并发解锁同一文章 | 100 | JMeter / Gatling | 无重复扣分，无负余额 |
| 同支付单号并发回调 | 50 | JMeter | 只入账一次，notify_count=50 |
| 同事件并发重复投递 | 50 | 多线程 JUnit | 只消费一次，仅 1 条流水 |
| 多用户并发扣分（不同文章） | 200 | JMeter | 各自账户余额正确，无交叉 |

---

### 2.2 并发测试脚本（JUnit + CountDownLatch）

```java
// 场景：100 并发解锁同一文章
@Test
public void testConcurrentUnlock() throws InterruptedException {
    int threadCount = 100;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount   = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                latch.await();
                articleUnlockAppService.unlock(1001L, 2005L);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }).start();
        latch.countDown();
    }

    Thread.sleep(5000);

    // 验证
    long orderCount = unlockOrderRepository.countByUserAndArticle(1001L, 2005L);
    long relationCount = unlockRelationRepository.countByUserAndArticle(1001L, 2005L);
    long journalCount  = pointJournalRepository.countByBizNoLike("UNLOCK-1001-2005-%");
    long balance = pointAccountRepository.findByUserId(1001L).getBalance();

    assertThat(orderCount).isEqualTo(1);
    assertThat(relationCount).isEqualTo(1);
    assertThat(journalCount).isEqualTo(1);
    assertThat(balance).isEqualTo(INITIAL_BALANCE - UNLOCK_PRICE); // 150
}
```

---

### 2.3 验收指标汇总

| 指标 | 要求 |
|------|------|
| 无重复扣分 | `user_point_journal` 中同 biz_no 仅 1 条 |
| 无超发经验 | `user_exp_journal` 中同 idempotent_key 仅 1 条 |
| 无负余额 | `user_point_account.balance >= 0`（禁负策略下） |
| 分账守恒 | `platform_points + author_points = total_points`（所有记录） |
| 乐观锁成功率 | 并发场景下成功率 > 99%（失败重试 3 次） |

---

## 3. 回归命令

```bash
# 单测全跑（成长模块）
mvn test -pl backend -Dtest="com.myblog.growth.**"

# 并发测试
mvn test -pl backend -Dtest="com.myblog.growth.**Concurrent*"

# 集成测试
mvn test -pl backend -Dtest="com.myblog.growth.**Integration*"
```

---

## 4. 联调顺序建议

```
① P1-T1（建表）→ P1-T4（配置规则）→ P1-T2（触发事件验证经验）→ P1-T3（验证升级）
② P2-T1（建表）→ P2-T2（签到）→ P2-T3（拉新）→ P2-T4（充值）
③ P3-T1（建表+解锁）→ P3-T2（验证分账）→ P3-T3（前端联调）
④ QA-T1（全量回归）→ QA-T2（并发压测）

