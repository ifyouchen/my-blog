package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.GrowthJournalContextService;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.growth.domain.model.aggregate.PointAccount;
import com.myblog.growth.domain.model.valueobject.PointJournal;
import com.myblog.growth.interfaces.rest.dto.response.PointAccountVO;
import com.myblog.growth.interfaces.rest.dto.response.PointJournalVO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户积分查询接口.
 *
 * <pre>
 * GET /api/points/account   — 查询我的积分账户
 * GET /api/points/journals  — 分页查询积分流水
 * </pre>
 */
@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointAppService pointAppService;
    private final GrowthJournalContextService journalContextService;

    /**
     * 构造注入.
     *
     * @param pointAppService      积分应用服务
     * @param journalContextService 流水上下文服务
     */
    public PointController(PointAppService pointAppService,
                           GrowthJournalContextService journalContextService) {
        this.pointAppService = pointAppService;
        this.journalContextService = journalContextService;
    }

    /**
     * 查询我的积分账户.
     *
     * @return 积分账户 VO
     */
    @GetMapping("/account")
    public Result<PointAccountVO> getMyAccount() {
        Long userId = AuthContext.getRequiredUserId();
        PointAccount account = pointAppService.getAccount(userId);
        PointAccountVO vo = toAccountVO(account);
        return Result.success(vo);
    }

    /**
     * 分页查询积分流水.
     *
     * @param sourceType 来源类型筛选（可选）
     * @param page       页码（从 1 开始，默认 1）
     * @param size       每页条数（默认 20，最大 50）
     * @return 积分流水分页结果
     */
    @GetMapping("/journals")
    public Result<PageResult<PointJournalVO>> getMyJournals(
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Long userId = AuthContext.getRequiredUserId();
        long total = pointAppService.countJournals(userId, sourceType);
        List<PointJournal> journals = pointAppService.getJournals(userId, sourceType, safePage, safeSize);

        Map<String, GrowthJournalContextService.ArticleContext> articleContexts =
                journalContextService.resolvePointJournalContexts(journals);
        List<PointJournalVO> voList = journals.stream()
                .map(journal -> toJournalVO(journal, articleContexts.get(journal.getBizNo())))
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(voList, safePage, safeSize, total));
    }

    // ────────────────────────── 私有转换方法 ──────────────────────────────

    private PointAccountVO toAccountVO(PointAccount account) {
        PointAccountVO vo = new PointAccountVO();
        vo.setUserId(account.getUserId());
        vo.setBalance(account.getBalance());
        vo.setTotalEarned(account.getTotalEarned());
        vo.setTotalSpent(account.getTotalSpent());
        return vo;
    }

    private PointJournalVO toJournalVO(PointJournal journal,
                                       GrowthJournalContextService.ArticleContext articleContext) {
        PointJournalVO vo = new PointJournalVO();
        vo.setId(journal.getId());
        vo.setDelta(journal.getDelta());
        vo.setBalanceAfter(journal.getBalanceAfter());
        vo.setSourceType(journal.getSourceType());
        vo.setBizNo(journal.getBizNo());
        vo.setRemark(journal.getRemark());
        if (articleContext != null) {
            vo.setArticleId(articleContext.getArticleId());
            vo.setArticleTitle(articleContext.getArticleTitle());
            vo.setArticleStatus(articleContext.getArticleStatus());
            vo.setArticleAccessible(articleContext.isArticleAccessible());
        }
        vo.setCreatedAt(journal.getCreatedAt());
        return vo;
    }
}

