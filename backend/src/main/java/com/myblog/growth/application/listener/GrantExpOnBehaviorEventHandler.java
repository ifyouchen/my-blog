package com.myblog.growth.application.listener;

import com.myblog.application.event.ArticleFavoritedEvent;
import com.myblog.application.event.ArticleLikedEvent;
import com.myblog.application.event.ArticlePublishedEvent;
import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.growth.application.event.BehaviorExpEvent;
import com.myblog.growth.application.service.ExpGrantAppService;
import com.myblog.growth.shared.enums.GrowthEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 行为经验发放事件监听器.
 * <p>
 * 订阅博客侧发布的各类行为事件，将其适配为 {@link BehaviorExpEvent}，
 * 再委托给 {@link ExpGrantAppService} 完成经验入账。
 * </p>
 *
 * <p><b>各事件对应经验发放规则：</b></p>
 * <ul>
 *   <li>{@code LIKE}     — actorUserId（点赞者）+ authorUserId（被赞文章作者）</li>
 *   <li>{@code COMMENT}  — actorUserId（评论者）+ authorUserId（文章作者）</li>
 *   <li>{@code FAVORITE} — actorUserId（收藏者）+ authorUserId（文章作者）</li>
 *   <li>{@code PUBLISH}  — actorUserId = authorUserId（作者本人发布文章）</li>
 *   <li>{@code READ}     — 仅 authorUserId（作者被阅读，actorUserId 无法在事件中传递）</li>
 * </ul>
 *
 * <p>所有监听器方法均通过 {@code @Async} 异步执行，避免阻塞主流程。</p>
 */
@Component
public class GrantExpOnBehaviorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(GrantExpOnBehaviorEventHandler.class);

    /** 经验发放应用服务. */
    private final ExpGrantAppService expGrantAppService;

    /**
     * 构造注入.
     *
     * @param expGrantAppService 经验发放应用服务
     */
    public GrantExpOnBehaviorEventHandler(ExpGrantAppService expGrantAppService) {
        this.expGrantAppService = expGrantAppService;
    }

    /**
     * 监听文章点赞事件 → 发放 LIKE 经验.
     * <p>actorUserId = 点赞者，authorUserId = 文章作者（需要额外查询或从事件补充）</p>
     *
     * @param event 文章点赞事件
     */
    @Async
    @EventListener
    public void onArticleLiked(ArticleLikedEvent event) {
        // ArticleLikedEvent 只有 articleId 和 userId（=actorUserId），不含 authorUserId。
        // AUTHOR 角色的经验发放由下游补充 authorUserId，当前只处理 ACTOR。
        BehaviorExpEvent expEvent = BehaviorExpEvent.builder()
                .eventId(buildEventId(GrowthEventType.LIKE.name(), event.getUserId(), event.getArticleId()))
                .eventType(GrowthEventType.LIKE.name())
                .actorUserId(event.getUserId())
                .authorUserId(null)   // 暂无 authorId，AUTHOR 经验跳过
                .sourceId(event.getArticleId())
                .occurredAt(event.getOccurredOn())
                .build();
        grantSafely(expEvent);
    }

    /**
     * 监听文章发布事件 → 发放 PUBLISH 经验.
     * <p>actorUserId = authorUserId = 文章作者</p>
     *
     * @param event 文章发布事件
     */
    @Async
    @EventListener
    public void onArticlePublished(ArticlePublishedEvent event) {
        BehaviorExpEvent expEvent = BehaviorExpEvent.builder()
                .eventId(buildEventId(GrowthEventType.PUBLISH.name(), event.getAuthorId(), event.getArticleId()))
                .eventType(GrowthEventType.PUBLISH.name())
                .actorUserId(event.getAuthorId())
                .authorUserId(null)  // PUBLISH 行为中 AUTHOR 与 ACTOR 相同，不重复发放
                .sourceId(event.getArticleId())
                .occurredAt(event.getOccurredOn())
                .build();
        grantSafely(expEvent);
    }

    /**
     * 监听文章收藏事件 → 发放 FAVORITE 经验.
     *
     * @param event 文章收藏事件
     */
    @Async
    @EventListener
    public void onArticleFavorited(ArticleFavoritedEvent event) {
        BehaviorExpEvent expEvent = BehaviorExpEvent.builder()
                .eventId(buildEventId(GrowthEventType.FAVORITE.name(), event.getUserId(), event.getArticleId()))
                .eventType(GrowthEventType.FAVORITE.name())
                .actorUserId(event.getUserId())
                .authorUserId(null)   // 暂无 authorId
                .sourceId(event.getArticleId())
                .occurredAt(event.getOccurredOn())
                .build();
        grantSafely(expEvent);
    }

    /**
     * 监听评论创建事件 → 发放 COMMENT 经验.
     * <p>
     * {@link CommentCreatedEvent} 包含评论者和文章作者，可同时支持 ACTOR / AUTHOR 规则。
     * </p>
     *
     * @param event 评论创建事件
     */
    @Async
    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        Long actorUserId = event.getCommentAuthorId();
        BehaviorExpEvent expEvent = BehaviorExpEvent.builder()
                .eventId(buildCommentEventId(actorUserId, event.getCommentId()))
                .eventType(GrowthEventType.COMMENT.name())
                .actorUserId(actorUserId)
                .authorUserId(event.getArticleAuthorId())
                .sourceId(event.getCommentId())
                .occurredAt(event.getOccurredOn())
                .build();
        grantSafely(expEvent);
    }

    /**
     * 安全地调用经验发放（捕获异常，防止异步线程崩溃影响主流程）.
     *
     * @param event 行为经验事件
     */
    private void grantSafely(BehaviorExpEvent event) {
        try {
            expGrantAppService.grantExpForBehavior(event);
        } catch (Exception e) {
            log.error("[经验发放监听] 发放失败，eventId={}, 原因={}", event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 构建全局唯一事件 ID.
     * <p>
     * 格式：{@code evt-{eventType}-{userId}-{sourceId}}，
     * 同一用户对同一内容的同一行为，在幂等键层面保证只发放一次。
     * </p>
     *
     * @param eventType 行为类型
     * @param userId    用户 ID
     * @param sourceId  来源 ID
     * @return 唯一事件 ID
     */
    private String buildEventId(String eventType, Long userId, Long sourceId) {
        return "evt-" + eventType + "-" + userId + "-" + sourceId;
    }

    /**
     * 构建评论经验事件 ID.
     * <p>
     * COMMENT 早期实现曾将评论者误当作作者写入消费记录，旧键格式为
     * {@code evt-COMMENT-{userId}-{commentId}}。升级为 v2 可避免新逻辑被旧幂等记录误跳过。
     * </p>
     *
     * @param userId    评论者用户 ID
     * @param commentId 评论 ID
     * @return 评论经验事件 ID
     */
    private String buildCommentEventId(Long userId, Long commentId) {
        return "evt-COMMENT-v2-" + userId + "-" + commentId;
    }
}

