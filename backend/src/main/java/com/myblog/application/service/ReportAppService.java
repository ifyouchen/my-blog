package com.myblog.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.myblog.application.command.CreateReportCommand;
import com.myblog.application.command.ResolveReportCommand;
import com.myblog.application.dto.ReportDTO;
import com.myblog.application.event.ReportSubmittedEvent;
import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.Report;
import com.myblog.domain.model.aggregate.User;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.ReportId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.CommentRepository;
import com.myblog.domain.repository.ReportRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.enums.ReportReasonType;
import com.myblog.shared.enums.ReportResolveAction;
import com.myblog.shared.enums.ReportStatus;
import com.myblog.shared.enums.ReportTargetType;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.enums.UserStatus;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 举报应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class ReportAppService {

    private static final int MAX_REPORTS_PER_WINDOW = 10;
    private static final Logger log = LoggerFactory.getLogger(ReportAppService.class);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final AdminAppService adminAppService;
    private final CommentAppService commentAppService;
    private final Cache<Long, AtomicInteger> reportCreateRateCache;
    private final ApplicationEventPublisher eventPublisher;

    public ReportAppService(ReportRepository reportRepository,
                            UserRepository userRepository,
                            ArticleRepository articleRepository,
                            CommentRepository commentRepository,
                            AdminAppService adminAppService,
                            CommentAppService commentAppService,
                            Cache<Long, AtomicInteger> reportCreateRateCache,
                            ApplicationEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.adminAppService = adminAppService;
        this.commentAppService = commentAppService;
        this.reportCreateRateCache = reportCreateRateCache;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建举报。
     *
     * @param command 创建举报命令
     * @return 举报数据
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportDTO createReport(CreateReportCommand command) {
        long _start = System.currentTimeMillis();
        Long reporterUserId = command.getReporterUserId();
        User reporter = userRepository.findById(new UserId(reporterUserId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (!UserStatus.NORMAL.equals(reporter.getStatus())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "用户状态异常，不能提交举报");
        }

        ReportTargetType targetType = parseTargetType(command.getTargetType());
        ReportReasonType reasonType = parseReasonType(command.getReasonType());
        ensureTargetCanBeReported(targetType, command.getTargetId(), reporterUserId);
        ensureCreateRate(reporterUserId);

        if (reportRepository.existsPendingByReporterAndTarget(
            new UserId(reporterUserId),
            targetType,
            command.getTargetId()
        )) {
            throw new ApplicationException(ErrorCode.CONFLICT, "你已提交过举报，平台会尽快处理");
        }

        Report report = Report.create(
            reportRepository.nextId(),
            new UserId(reporterUserId),
            targetType,
            command.getTargetId(),
            reasonType,
            command.getReasonDetail()
        );
        reportRepository.save(report);
        eventPublisher.publishEvent(new ReportSubmittedEvent(
            report.getId().getValue(), reporterUserId, targetType.name(), command.getTargetId()));

        Map<Long, User> userMap = new HashMap<Long, User>();
        userMap.put(reporter.getId().getValue(), reporter);
        ReportDTO result = toDTO(report, userMap, false);
        log.info("{} | {} 创建举报 | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(reporterUserId),
            BizLogHelper.params("targetType", targetType, "reasonType", reasonType),
            BizLogHelper.result("reportId=" + result.getId()),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 分页查询后台举报列表。
     *
     * @param status 状态筛选
     * @param targetType 目标类型
     * @param reasonType 原因类型
     * @param page 页码
     * @param pageSize 每页数量
     * @return 举报分页
     */
    public PageResult<ReportDTO> pageReports(String status, String targetType, String reasonType,
                                             int page, int pageSize) {
        ReportStatus resolvedStatus = parseNullableStatus(status);
        ReportTargetType resolvedTargetType = parseNullableTargetType(targetType);
        ReportReasonType resolvedReasonType = parseNullableReasonType(reasonType);
        List<Report> reports = reportRepository.findAdminPage(
            resolvedStatus,
            resolvedTargetType,
            resolvedReasonType,
            page,
            pageSize
        );
        long total = reportRepository.countAdminPage(resolvedStatus, resolvedTargetType, resolvedReasonType);
        Map<Long, User> userMap = buildUserMap(reports);
        List<ReportDTO> items = new ArrayList<ReportDTO>(reports.size());
        for (Report report : reports) {
            items.add(toDTO(report, userMap, false));
        }
        return new PageResult<ReportDTO>(items, Math.max(page, 1), Math.max(pageSize, 1), total);
    }

    /**
     * 查询举报详情。
     *
     * @param reportId 举报 ID
     * @return 举报详情
     */
    public ReportDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(new ReportId(reportId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "举报不存在"));
        List<Report> reports = new ArrayList<Report>();
        reports.add(report);
        return toDTO(report, buildUserMap(reports), true);
    }

    /**
     * 处理举报。
     *
     * @param command 处理命令
     * @return 处理后的举报
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportDTO resolveReport(ResolveReportCommand command) {
        long _start = System.currentTimeMillis();
        Report report = reportRepository.findById(new ReportId(command.getReportId()))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "举报不存在"));
        UserId handlerUserId = new UserId(command.getHandlerUserId());
        ReportResolveAction action = parseResolveAction(command.getAction());
        String handleNote = command.getHandleNote();

        if (ReportResolveAction.REJECT.equals(action)) {
            report.reject(handlerUserId, handleNote);
            reportRepository.save(report);
            ReportDTO rejectResult = getReportDetail(report.getId().getValue());
            log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.who(command.getHandlerUserId()),
                "处理举报",
                BizLogHelper.params("reportId", command.getReportId(), "action", command.getAction()),
                BizLogHelper.result("resolved=true"),
                BizLogHelper.elapsed(_start));
            return rejectResult;
        }

        if (ReportResolveAction.OFFLINE_ARTICLE.equals(action)) {
            ensureTargetType(report, ReportTargetType.ARTICLE);
            adminAppService.offlineArticle(report.getTargetId(), handleNote);
        } else if (ReportResolveAction.DELETE_COMMENT.equals(action)) {
            ensureTargetType(report, ReportTargetType.COMMENT);
            commentAppService.deleteComment(report.getTargetId(), command.getHandlerUserId(), UserRole.ADMIN.name());
        } else if (ReportResolveAction.DISABLE_USER.equals(action)) {
            ensureTargetType(report, ReportTargetType.USER);
            adminAppService.disableUser(report.getTargetId(), handleNote);
        }

        report.resolve(handlerUserId, handleNote);
        reportRepository.save(report);
        ReportDTO result = getReportDetail(report.getId().getValue());
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(command.getHandlerUserId()),
            "处理举报",
            BizLogHelper.params("reportId", command.getReportId(), "action", command.getAction()),
            BizLogHelper.result("resolved=true"),
            BizLogHelper.elapsed(_start));
        return result;
    }

    /**
     * 检查举报请求频率，超过时间窗口内上限则抛出异常。
     *
     * @param reporterUserId 举报者用户 ID
     * @throws ApplicationException 举报过于频繁时抛出
     */
    private void ensureCreateRate(Long reporterUserId) {
        AtomicInteger counter = reportCreateRateCache.get(reporterUserId, key -> new AtomicInteger(0));
        if (counter.incrementAndGet() > MAX_REPORTS_PER_WINDOW) {
            throw new ApplicationException(ErrorCode.CONFLICT, "举报提交过于频繁，请稍后再试");
        }
    }

    /**
     * 校验举报目标的合法性，验证目标存在且可被举报。
     *
     * @param targetType     举报目标类型
     * @param targetId       目标 ID
     * @param reporterUserId 举报者用户 ID
     * @throws ApplicationException 目标不存在或不允许举报自己时抛出
     */
    private void ensureTargetCanBeReported(ReportTargetType targetType, Long targetId, Long reporterUserId) {
        if (targetId == null || targetId.longValue() <= 0L) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "举报目标不能为空");
        }
        if (ReportTargetType.ARTICLE.equals(targetType)) {
            Article article = articleRepository.findById(new ArticleId(targetId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在"));
            if (!ArticleStatus.PUBLISHED.equals(article.getStatus())) {
                throw new ApplicationException(ErrorCode.NOT_FOUND, "文章不存在");
            }
            return;
        }
        if (ReportTargetType.COMMENT.equals(targetType)) {
            commentRepository.findById(new CommentId(targetId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "评论不存在"));
            return;
        }
        User user = userRepository.findById(new UserId(targetId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (user.getId().getValue().equals(reporterUserId)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不能举报自己");
        }
    }

    /**
     * 校验举报的目标类型是否与期望一致。
     *
     * @param report       举报领域对象
     * @param expectedType 期望目标类型
     * @throws ApplicationException 类型不匹配时抛出
     */
    private void ensureTargetType(Report report, ReportTargetType expectedType) {
        if (!expectedType.equals(report.getTargetType())) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "处理动作与举报目标不匹配");
        }
    }

    /**
     * 构建举报相关用户映射（举报者和处理人），避免 N+1 查询。
     *
     * @param reports 举报列表
     * @return 用户 ID 到用户领域对象的映射
     */
    private Map<Long, User> buildUserMap(List<Report> reports) {
        Set<Long> userIds = new HashSet<Long>();
        for (Report report : reports) {
            userIds.add(report.getReporterUserId().getValue());
            if (report.getHandlerUserId() != null) {
                userIds.add(report.getHandlerUserId().getValue());
            }
        }
        if (userIds.isEmpty()) {
            return new HashMap<Long, User>();
        }
        List<User> users = userRepository.findByIds(new ArrayList<Long>(userIds));
        Map<Long, User> userMap = new HashMap<Long, User>(users.size() * 2);
        for (User user : users) {
            userMap.put(user.getId().getValue(), user);
        }
        return userMap;
    }

    /**
     * 将举报领域对象转换为 DTO。
     *
     * @param report               举报领域对象
     * @param userMap              用户映射
     * @param includeTargetSummary 是否包含目标摘要信息
     * @return 举报 DTO
     */
    private ReportDTO toDTO(Report report, Map<Long, User> userMap, boolean includeTargetSummary) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId().getValue());
        dto.setReporterUserId(report.getReporterUserId().getValue());
        dto.setTargetType(report.getTargetType().name());
        dto.setTargetId(report.getTargetId());
        dto.setReasonType(report.getReasonType().name());
        dto.setReasonDetail(report.getReasonDetail());
        dto.setStatus(report.getStatus().name());
        dto.setHandlerUserId(report.getHandlerUserId() == null ? null : report.getHandlerUserId().getValue());
        dto.setHandleNote(report.getHandleNote());
        dto.setHandledAt(report.getHandledAt());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        populateUser(dto, userMap);
        if (includeTargetSummary) {
            dto.setTargetSummary(buildTargetSummary(report));
        }
        return dto;
    }

    /**
     * 将举报者和处理人的用户信息填充到 DTO。
     *
     * @param dto     举报 DTO
     * @param userMap 用户映射
     */
    private void populateUser(ReportDTO dto, Map<Long, User> userMap) {
        User reporter = userMap.get(dto.getReporterUserId());
        if (reporter != null) {
            dto.setReporterUsername(reporter.getUsername());
            dto.setReporterNickname(reporter.getNickname());
        }
        if (dto.getHandlerUserId() == null) {
            return;
        }
        User handler = userMap.get(dto.getHandlerUserId());
        if (handler != null) {
            dto.setHandlerUsername(handler.getUsername());
            dto.setHandlerNickname(handler.getNickname());
        }
    }

    /**
     * 构建举报目标的摘要信息（文章、评论或用户）。
     *
     * @param report 举报领域对象
     * @return 目标摘要 Map，包含类型、ID、可用状态及关键内容
     */
    private Map<String, Object> buildTargetSummary(Report report) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("type", report.getTargetType().name());
        summary.put("id", report.getTargetId());
        if (ReportTargetType.ARTICLE.equals(report.getTargetType())) {
            Article article = articleRepository.findById(new ArticleId(report.getTargetId())).orElse(null);
            if (article == null) {
                summary.put("available", false);
                return summary;
            }
            summary.put("available", true);
            summary.put("title", article.getTitle());
            summary.put("status", article.getStatus().name());
            summary.put("authorId", article.getAuthorId().getValue());
            summary.put("offlineReason", article.getOfflineReason());
            return summary;
        }
        if (ReportTargetType.COMMENT.equals(report.getTargetType())) {
            Comment comment = commentRepository.findById(new CommentId(report.getTargetId())).orElse(null);
            if (comment == null) {
                summary.put("available", false);
                return summary;
            }
            summary.put("available", true);
            summary.put("articleId", comment.getArticleId().getValue());
            summary.put("userId", comment.getUserId().getValue());
            summary.put("content", comment.getContent());
            return summary;
        }
        User user = userRepository.findById(new UserId(report.getTargetId())).orElse(null);
        if (user == null) {
            summary.put("available", false);
            return summary;
        }
        summary.put("available", true);
        summary.put("username", user.getUsername());
        summary.put("nickname", user.getNickname());
        summary.put("status", user.getStatus().name());
        summary.put("disableReason", user.getDisableReason());
        return summary;
    }

    /**
     * 解析举报目标类型字符串为枚举，失败时抛出参数异常。
     *
     * @param value 目标类型字符串
     * @return 举报目标类型枚举
     * @throws ApplicationException 字符串为空或不支持时抛出
     */
    private ReportTargetType parseTargetType(String value) {
        try {
            return ReportTargetType.valueOf(normalizeRequired(value, "举报目标类型不能为空"));
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的举报目标类型");
        }
    }

    /**
     * 解析举报原因类型字符串为枚举，失败时抛出参数异常。
     *
     * @param value 原因类型字符串
     * @return 举报原因类型枚举
     * @throws ApplicationException 字符串为空或不支持时抛出
     */
    private ReportReasonType parseReasonType(String value) {
        try {
            return ReportReasonType.valueOf(normalizeRequired(value, "举报原因不能为空"));
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的举报原因");
        }
    }

    /**
     * 解析举报处理动作字符串为枚举，失败时抛出参数异常。
     *
     * @param value 处理动作字符串
     * @return 举报处理动作枚举
     * @throws ApplicationException 字符串为空或不支持时抛出
     */
    private ReportResolveAction parseResolveAction(String value) {
        try {
            return ReportResolveAction.valueOf(normalizeRequired(value, "处理动作不能为空"));
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的处理动作");
        }
    }

    /**
     * 解析可空的举报状态字符串为枚举。
     *
     * @param value 状态字符串，空时返回 null
     * @return 举报状态枚举，或 null
     * @throws ApplicationException 字符串不支持时抛出
     */
    private ReportStatus parseNullableStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ReportStatus.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "不支持的举报状态");
        }
    }

    /**
     * 解析可空的举报目标类型字符串为枚举。
     *
     * @param value 目标类型字符串，空时返回 null
     * @return 举报目标类型枚举，或 null
     */
    private ReportTargetType parseNullableTargetType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseTargetType(value);
    }

    /**
     * 解析可空的举报原因类型字符串为枚举。
     *
     * @param value 原因类型字符串，空时返回 null
     * @return 举报原因类型枚举，或 null
     */
    private ReportReasonType parseNullableReasonType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseReasonType(value);
    }

    /**
     * 规范化必填字符串，为空时抛出参数异常。
     *
     * @param value   原始字符串
     * @param message 为空时的错误信息
     * @return 去除首尾空白后的字符串
     * @throws ApplicationException 字符串为空时抛出
     */
    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }
}
