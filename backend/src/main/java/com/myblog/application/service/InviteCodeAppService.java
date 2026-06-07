package com.myblog.application.service;

import com.myblog.infrastructure.repository.persistence.entity.InviteCodeDO;
import com.myblog.infrastructure.repository.persistence.mapper.InviteCodeMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 邀请码应用服务。
 * <p>
 * 每个用户拥有一个可复用的邀请码，永久有效。
 * 可随时重新生成，旧码自动失效。
 * </p>
 */
@Service
public class InviteCodeAppService {

    private static final Logger log = LoggerFactory.getLogger(InviteCodeAppService.class);

    private final InviteCodeMapper mapper;

    public InviteCodeAppService(InviteCodeMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取或创建用户的邀请码。
     * <p>
     * 每个用户只有一个可复用邀请码，如有活跃码则直接返回，否则创建新码。
     * </p>
     *
     * @param userId 用户 ID
     * @return 邀请码详情 Map
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(Long userId) {
        long _start = System.currentTimeMillis();
        // 先查是否有活跃码
        InviteCodeDO existing = mapper.selectActiveByCreatorId(userId);
        if (existing != null) {
            log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
                BizLogHelper.trace(),
                BizLogHelper.who(userId),
                "获取已有邀请码",
                BizLogHelper.params(),
                BizLogHelper.result("codeId=" + existing.getId()),
                BizLogHelper.elapsed(_start));
            return toMap(existing);
        }
        // 无活跃码则创建
        InviteCodeDO row = new InviteCodeDO();
        row.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setCreatorId(userId);
        row.setExpiredAt(null);
        row.setMaxUses(9999);
        row.setUseCount(0);
        mapper.insert(row);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "生成邀请码",
            BizLogHelper.params(),
            BizLogHelper.result("codeId=" + row.getId()),
            BizLogHelper.elapsed(_start));
        return toMap(row);
    }

    /**
     * 重新生成邀请码（旧码软删除，创建新码）。
     *
     * @param userId 用户 ID
     * @return 新邀请码详情 Map
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> regenerate(Long userId) {
        long _start = System.currentTimeMillis();
        // 软删除旧码
        InviteCodeDO old = mapper.selectActiveByCreatorId(userId);
        if (old != null) {
            mapper.softDelete(old.getId());
        }
        // 创建新码
        InviteCodeDO row = new InviteCodeDO();
        row.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setCreatorId(userId);
        row.setExpiredAt(null);
        row.setMaxUses(9999);
        row.setUseCount(0);
        mapper.insert(row);
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "重新生成邀请码",
            BizLogHelper.params(),
            BizLogHelper.result("codeId=" + row.getId()),
            BizLogHelper.elapsed(_start));
        return toMap(row);
    }

    /**
     * 查询用户的邀请码（返回单个码的列表，兼容前端旧逻辑）。
     *
     * @param userId 用户 ID
     * @return 邀请码详情 Map 列表
     */
    public List<Map<String, Object>> listByUser(Long userId) {
        InviteCodeDO code = mapper.selectActiveByCreatorId(userId);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (code != null) result.add(toMap(code));
        return result;
    }

    /**
     * 使用邀请码（注册时调用）。
     * <p>
     * 如果邀请码不存在、已过期或已用完，将静默进行处理（不抛异常）。
     * </p>
     *
     * @param code   邀请码字符串
     * @param userId 使用者用户 ID
     * @return 邀请码创建者（邀请人）的 userId，码无效时返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public Long useCode(String code, Long userId) {
        long _start = System.currentTimeMillis();
        if (code == null || code.trim().isEmpty()) return null;
        InviteCodeDO row = mapper.selectByCode(code.trim());
        if (row == null) return null;
        if (row.getExpiredAt() != null && row.getExpiredAt().isBefore(LocalDateTime.now())) return null;
        if (row.getUseCount() != null && row.getMaxUses() != null
                && row.getUseCount() >= row.getMaxUses()) return null;
        mapper.useCode(row.getId(), userId, row.getVersion());
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "使用邀请码",
            BizLogHelper.params(),
            BizLogHelper.result("used=true"),
            BizLogHelper.elapsed(_start));
        return row.getCreatorId();
    }

    /**
     * 管理员分页查询邀请码列表。
     *
     * @param keyword  搜索关键字，可为 null
     * @param page     页码
     * @param pageSize 每页数量（最大 100）
     * @return 邀请码分页结果
     */
    public PageResult<Map<String, Object>> adminPage(String keyword, int page, int pageSize) {
        int ps = Math.max(1, Math.min(pageSize, 100));
        int p = Math.max(1, page);
        int offset = (p - 1) * ps;
        List<InviteCodeDO> rows = mapper.selectPage(keyword, offset, ps);
        long total = mapper.countPage(keyword);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (InviteCodeDO r : rows) items.add(toMap(r));
        return new PageResult<Map<String, Object>>(items, p, ps, total);
    }

    /**
     * 管理员软删除邀请码。
     *
     * @param id 邀请码 ID
     * @throws ApplicationException 邀请码不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Long id) {
        long _start = System.currentTimeMillis();
        if (mapper.selectById(id) == null) throw new ApplicationException(ErrorCode.NOT_FOUND, "邀请码不存在");
        mapper.softDelete(id);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "删除邀请码",
            BizLogHelper.trace(),
            BizLogHelper.params("inviteCodeId", id),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 将邀请码 DO 转换为 Map 表示，并计算当前状态。
     *
     * @param r 邀请码 DO
     * @return 包含邀请码信息和状态的 Map
     */
    private Map<String, Object> toMap(InviteCodeDO r) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("creatorId", r.getCreatorId());
        m.put("usedBy", r.getUsedBy());
        m.put("usedAt", r.getUsedAt());
        m.put("expiredAt", r.getExpiredAt());
        m.put("maxUses", r.getMaxUses());
        m.put("useCount", r.getUseCount());
        m.put("createdAt", r.getCreatedAt());
        boolean expired = r.getExpiredAt() != null && r.getExpiredAt().isBefore(LocalDateTime.now());
        boolean used = r.getUseCount() != null && r.getMaxUses() != null && r.getUseCount() >= r.getMaxUses();
        m.put("status", used ? "USED" : (expired ? "EXPIRED" : "ACTIVE"));
        return m;
    }
}

