package com.myblog.application.service;

import com.myblog.application.dto.AnnualCreatorCandidateDTO;
import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.RecommendationApplicationDTO;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.ArticleRecommendationApplication;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRecommendationApplicationRepository;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.growth.application.service.PointAppService;
import com.myblog.growth.application.service.UserPrivilegeAppService;
import com.myblog.growth.domain.model.aggregate.GrowthAccount;
import com.myblog.growth.domain.model.valueobject.UserPrivilegeEntitlement;
import com.myblog.growth.domain.repository.GrowthAccountRepository;
import com.myblog.growth.domain.repository.UserPrivilegeEntitlementRepository;
import com.myblog.growth.shared.constant.GrowthPrivilegeCodes;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页推荐申请应用服务.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Service
public class RecommendationApplicationAppService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArticleRecommendationApplicationRepository applicationRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final UserPrivilegeAppService userPrivilegeAppService;
    private final UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository;
    private final GrowthAccountRepository growthAccountRepository;
    private final AdminAppService adminAppService;

    public RecommendationApplicationAppService(ArticleRecommendationApplicationRepository applicationRepository,
                                               ArticleRepository articleRepository,
                                               UserRepository userRepository,
                                               UserPrivilegeAppService userPrivilegeAppService,
                                               UserPrivilegeEntitlementRepository userPrivilegeEntitlementRepository,
                                               GrowthAccountRepository growthAccountRepository,
                                               AdminAppService adminAppService) {
        this.applicationRepository = applicationRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.userPrivilegeAppService = userPrivilegeAppService;
        this.userPrivilegeEntitlementRepository = userPrivilegeEntitlementRepository;
        this.growthAccountRepository = growthAccountRepository;
        this.adminAppService = adminAppService;
    }

    /**
     * 提交首页推荐申请.
     *
     * @param articleId 文章ID
     * @param userId    当前用户ID
     * @return 申请记录
     */
    @Transactional(rollbackFor = Exception.class)
    public RecommendationApplicationDTO apply(Long articleId, Long userId) {
        Article article = articleRepository.findById(new ArticleId(articleId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
        if (!article.getAuthorId().getValue().equals(userId)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "只能为自己的文章申请首页推荐");
        }
        if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "仅已发布文章可申请首页推荐");
        }
        if (!userPrivilegeAppService.hasPrivilege(userId, GrowthPrivilegeCodes.HOMEPAGE_RECOMMEND_ELIGIBLE)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "当前等级未解锁首页推荐申请资格");
        }
        if (applicationRepository.findPendingByArticleId(articleId).isPresent()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "该文章已有待审核推荐申请");
        }
        ArticleRecommendationApplication application = ArticleRecommendationApplication.create(articleId, userId);
        applicationRepository.save(application);
        return toDTO(application, article, userRepository.findById(new UserId(userId)).orElse(null));
    }

    /**
     * 管理端分页查询推荐申请.
     *
     * @param status 状态
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    public PageResult<RecommendationApplicationDTO> pageForAdmin(String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        long total = applicationRepository.countForAdmin(status);
        List<ArticleRecommendationApplication> items = applicationRepository.findPageForAdmin(status, offset, safeSize);
        return new PageResult<RecommendationApplicationDTO>(toDTOList(items), safePage, safeSize, total);
    }

    /**
     * 审批通过首页推荐申请.
     *
     * @param applicationId 申请ID
     * @param reviewerId    审核人ID
     * @param reviewNote    审核备注
     * @return 审批结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RecommendationApplicationDTO approve(Long applicationId, Long reviewerId, String reviewNote) {
        ArticleRecommendationApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "推荐申请不存在"));
        if (!"PENDING".equals(application.getStatus())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "仅待审核申请可通过");
        }
        application.approve(reviewerId, reviewNote);
        if (!applicationRepository.update(application)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "推荐申请状态更新失败，请刷新后重试");
        }
        adminAppService.featureArticle(application.getArticleId(), null);
        return toDTO(application, null, null);
    }

    /**
     * 审批拒绝首页推荐申请.
     *
     * @param applicationId 申请ID
     * @param reviewerId    审核人ID
     * @param reviewNote    审核备注
     * @return 审批结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RecommendationApplicationDTO reject(Long applicationId, Long reviewerId, String reviewNote) {
        ArticleRecommendationApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "推荐申请不存在"));
        if (!"PENDING".equals(application.getStatus())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "仅待审核申请可拒绝");
        }
        application.reject(reviewerId, reviewNote);
        if (!applicationRepository.update(application)) {
            throw new ApplicationException(ErrorCode.CONFLICT, "推荐申请状态更新失败，请刷新后重试");
        }
        return toDTO(application, null, null);
    }

    /**
     * 为文章列表填充推荐申请状态.
     *
     * @param articles 文章列表
     */
    public void fillRecommendationStatus(List<ArticleDTO> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Long> articleIds = new ArrayList<Long>(articles.size());
        for (ArticleDTO article : articles) {
            articleIds.add(article.getId());
        }
        Map<Long, ArticleRecommendationApplication> latestMap = new HashMap<Long, ArticleRecommendationApplication>();
        for (ArticleRecommendationApplication application : applicationRepository.findByArticleIds(articleIds)) {
            if (!latestMap.containsKey(application.getArticleId())) {
                latestMap.put(application.getArticleId(), application);
            }
        }
        for (ArticleDTO article : articles) {
            ArticleRecommendationApplication application = latestMap.get(article.getId());
            if (application != null) {
                article.setRecommendationApplicationId(application.getId());
                article.setRecommendationApplicationStatus(application.getStatus());
            }
        }
    }

    /**
     * 查询年度创作者候选列表.
     *
     * @return 候选列表
     */
    public List<AnnualCreatorCandidateDTO> listAnnualCreatorCandidates() {
        return listAnnualCreatorCandidates(null);
    }

    /**
     * 查询年度创作者候选列表.
     *
     * @param userKeyword 用户名或邮箱（可选）
     * @return 候选列表
     */
    public List<AnnualCreatorCandidateDTO> listAnnualCreatorCandidates(String userKeyword) {
        String normalizedKeyword = userKeyword == null ? null : userKeyword.trim().toLowerCase();
        List<UserPrivilegeEntitlement> entitlements =
            userPrivilegeEntitlementRepository.findActiveByPrivilegeCode(GrowthPrivilegeCodes.ANNUAL_CREATOR_ELIGIBLE);
        List<Long> userIds = new ArrayList<Long>(entitlements.size());
        Map<Long, UserPrivilegeEntitlement> entitlementByUserId = new HashMap<Long, UserPrivilegeEntitlement>();
        for (UserPrivilegeEntitlement entitlement : entitlements) {
            userIds.add(entitlement.getUserId());
            entitlementByUserId.put(entitlement.getUserId(), entitlement);
        }
        Map<Long, User> userMap = new HashMap<Long, User>();
        for (User user : userRepository.findByIds(userIds)) {
            userMap.put(user.getId().getValue(), user);
        }
        Map<Long, GrowthAccount> growthMap = growthAccountRepository.findByUserIds(userIds);
        List<AnnualCreatorCandidateDTO> items = new ArrayList<AnnualCreatorCandidateDTO>(userIds.size());
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            if (!matchesUserKeyword(user, normalizedKeyword)) {
                continue;
            }
            GrowthAccount account = growthMap.get(userId);
            UserPrivilegeEntitlement entitlement = entitlementByUserId.get(userId);
            AnnualCreatorCandidateDTO dto = new AnnualCreatorCandidateDTO();
            dto.setUserId(userId);
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setCurrentLevel(account == null ? 1 : account.getCurrentLevel());
            dto.setCurrentExp(account == null ? 0 : account.getCurrentExp());
            dto.setGrantedAt(entitlement != null && entitlement.getGrantedAt() != null
                ? DATETIME_FORMATTER.format(entitlement.getGrantedAt())
                : null);
            items.add(dto);
        }
        return items;
    }

    private boolean matchesUserKeyword(User user, String userKeyword) {
        if (userKeyword == null || userKeyword.isEmpty()) {
            return true;
        }
        String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
        String email = user.getEmail() == null || user.getEmail().getValue() == null
            ? ""
            : user.getEmail().getValue().toLowerCase();
        return username.contains(userKeyword) || email.contains(userKeyword);
    }

    private List<RecommendationApplicationDTO> toDTOList(List<ArticleRecommendationApplication> applications) {
        List<RecommendationApplicationDTO> items = new ArrayList<RecommendationApplicationDTO>(applications.size());
        List<Long> articleIds = new ArrayList<Long>(applications.size());
        List<Long> authorIds = new ArrayList<Long>(applications.size());
        for (ArticleRecommendationApplication application : applications) {
            articleIds.add(application.getArticleId());
            authorIds.add(application.getAuthorId());
        }
        Map<Long, Article> articleMap = new HashMap<Long, Article>();
        for (Article article : articleRepository.findByIds(articleIds)) {
            articleMap.put(article.getId().getValue(), article);
        }
        Map<Long, User> userMap = new HashMap<Long, User>();
        for (User user : userRepository.findByIds(authorIds)) {
            userMap.put(user.getId().getValue(), user);
        }
        for (ArticleRecommendationApplication application : applications) {
            items.add(toDTO(application, articleMap.get(application.getArticleId()), userMap.get(application.getAuthorId())));
        }
        return items;
    }

    private RecommendationApplicationDTO toDTO(ArticleRecommendationApplication application, Article article, User author) {
        RecommendationApplicationDTO dto = new RecommendationApplicationDTO();
        dto.setId(application.getId());
        dto.setArticleId(application.getArticleId());
        dto.setArticleTitle(article == null ? null : article.getTitle());
        dto.setAuthorId(application.getAuthorId());
        dto.setAuthorName(author == null ? null : (author.getNickname() == null ? author.getUsername() : author.getNickname()));
        dto.setStatus(application.getStatus());
        dto.setAppliedAt(application.getAppliedAt() == null ? null : DATETIME_FORMATTER.format(application.getAppliedAt()));
        dto.setReviewedAt(application.getReviewedAt() == null ? null : DATETIME_FORMATTER.format(application.getReviewedAt()));
        dto.setReviewedBy(application.getReviewedBy());
        dto.setReviewNote(application.getReviewNote());
        return dto;
    }
}
