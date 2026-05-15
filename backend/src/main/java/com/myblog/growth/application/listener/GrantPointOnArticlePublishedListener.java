package com.myblog.growth.application.listener;

import com.myblog.application.event.ArticlePublishedEvent;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.UserPointJournalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 文章发布积分奖励监听器.
 * <p>
 * 监听 {@link ArticlePublishedEvent}，读取 {@code point_rule_config.PUBLISH} 规则，
 * 按 {@code dailyLimit} 控制作者每日奖励次数，使用 {@code PUBLISH_POINT:{articleId}}
 * 作为幂等键避免重复入账。
 * </p>
 */
@Component
public class GrantPointOnArticlePublishedListener {

    private static final Logger log = LoggerFactory.getLogger(GrantPointOnArticlePublishedListener.class);

    private static final String SOURCE_PUBLISH = "PUBLISH";

    private final PointRuleRepository pointRuleRepository;
    private final PointAppService pointAppService;
    private final UserPointJournalMapper pointJournalMapper;

    public GrantPointOnArticlePublishedListener(PointRuleRepository pointRuleRepository,
                                                PointAppService pointAppService,
                                                UserPointJournalMapper pointJournalMapper) {
        this.pointRuleRepository = pointRuleRepository;
        this.pointAppService = pointAppService;
        this.pointJournalMapper = pointJournalMapper;
    }

    /**
     * 文章发布时发放 PUBLISH 积分奖励.
     */
    @Async
    @EventListener
    public void onArticlePublished(ArticlePublishedEvent event) {
        try {
            // 读取 PUBLISH 规则
            PointRule rule = pointRuleRepository.findBySourceType(SOURCE_PUBLISH).orElse(null);
            if (rule == null || !rule.isEffective()) {
                log.info("[发布积分] PUBLISH 积分规则未启用或未配置，跳过。articleId={}", event.getArticleId());
                return;
            }

            int pointAmount = rule.getPointAmount();
            int dailyLimit = rule.getDailyLimit();

            // 检查每日上限
            if (dailyLimit > 0) {
                int todayCount = pointJournalMapper.countTodayByUserIdAndSourceType(
                        event.getAuthorId(), SOURCE_PUBLISH);
                if (todayCount >= dailyLimit) {
                    log.info("[发布积分] 作者今日 PUBLISH 积分已达上限（{}/{}），跳过。authorId={}, articleId={}",
                            todayCount, dailyLimit, event.getAuthorId(), event.getArticleId());
                    return;
                }
            }

            // 发放积分（bizNo 保证幂等）
            String bizNo = "PUBLISH_POINT:" + event.getArticleId();
            pointAppService.addPoints(event.getAuthorId(), pointAmount, SOURCE_PUBLISH, bizNo,
                    "发布文章积分奖励（文章ID:" + event.getArticleId() + "）", null);

            log.info("[发布积分] 发放成功。authorId={}, articleId={}, pointAmount={}",
                    event.getAuthorId(), event.getArticleId(), pointAmount);
        } catch (Exception e) {
            log.error("[发布积分] 发放失败，articleId={}, 原因={}", event.getArticleId(), e.getMessage(), e);
        }
    }
}
