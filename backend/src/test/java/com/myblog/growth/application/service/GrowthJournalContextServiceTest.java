package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.aggregate.UnlockOrder;
import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.domain.repository.ArticleUnlockInfoRepository;
import com.myblog.growth.domain.repository.UnlockOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthJournalContextServiceTest {

    @Mock
    private UnlockOrderRepository unlockOrderRepository;

    @Mock
    private ArticleUnlockInfoRepository articleUnlockInfoRepository;

    @Test
    void resolvesPointJournalArticleContextsByUnlockOrder() {
        GrowthJournalContextService service =
                new GrowthJournalContextService(unlockOrderRepository, articleUnlockInfoRepository);
        String unlockOrderNo = "UNLOCK-1-950011-1";
        String shareOrderNo = "UNLOCK-2-950038-2";
        PointJournal unlockJournal = PointJournal.create(
                1L, -20, 80, "UNLOCK", unlockOrderNo, "解锁文章[950011]", null);
        PointJournal shareJournal = PointJournal.create(
                2L, 10, 110, "REVENUE_SHARE", "REVENUE_SHARE:" + shareOrderNo,
                "文章解锁分成[950038]", "system");
        LocalDateTime now = LocalDateTime.now();

        when(unlockOrderRepository.findByOrderNos(anyCollection())).thenReturn(Arrays.asList(
                UnlockOrder.restore(1L, unlockOrderNo, 1L, 950011L, 20,
                        UnlockOrder.STATUS_SUCCESS, null, now, now, 0),
                UnlockOrder.restore(2L, shareOrderNo, 2L, 950038L, 20,
                        UnlockOrder.STATUS_SUCCESS, null, now, now, 0)
        ));
        Map<Long, ArticleUnlockInfo> articleInfos = new HashMap<>();
        articleInfos.put(950011L, ArticleUnlockInfo.of(
                950011L, true, 20, 9L, "PUBLISHED", "Java 并发基础"));
        articleInfos.put(950038L, ArticleUnlockInfo.of(
                950038L, true, 20, 9L, "OFFLINE", "已下架文章"));
        when(articleUnlockInfoRepository.findByArticleIds(anyCollection())).thenReturn(articleInfos);

        Map<String, GrowthJournalContextService.ArticleContext> contexts =
                service.resolvePointJournalContexts(Arrays.asList(unlockJournal, shareJournal));

        GrowthJournalContextService.ArticleContext unlockContext = contexts.get(unlockOrderNo);
        assertThat(unlockContext.getArticleId()).isEqualTo(950011L);
        assertThat(unlockContext.getArticleTitle()).isEqualTo("Java 并发基础");
        assertThat(unlockContext.isArticleAccessible()).isTrue();

        GrowthJournalContextService.ArticleContext shareContext =
                contexts.get("REVENUE_SHARE:" + shareOrderNo);
        assertThat(shareContext.getArticleId()).isEqualTo(950038L);
        assertThat(shareContext.getArticleTitle()).isEqualTo("已下架文章");
        assertThat(shareContext.isArticleAccessible()).isFalse();
    }
}
