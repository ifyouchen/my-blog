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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InviteCodeAppService {

    private static final int MAX_ACTIVE = 3;
    private static final int VALID_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(InviteCodeAppService.class);

    private final InviteCodeMapper mapper;

    public InviteCodeAppService(InviteCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(Long userId) {
        long _start = System.currentTimeMillis();
        if (mapper.countActiveByCreatorId(userId) >= MAX_ACTIVE) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "最多同时持有 3 个有效邀请码");
        }
        InviteCodeDO row = new InviteCodeDO();
        row.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setCreatorId(userId);
        row.setExpiredAt(LocalDateTime.now().plusDays(VALID_DAYS));
        row.setMaxUses(1);
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

    public List<Map<String, Object>> listByUser(Long userId) {
        List<InviteCodeDO> rows = mapper.selectByCreatorId(userId);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (InviteCodeDO r : rows) result.add(toMap(r));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void useCode(String code, Long userId) {
        long _start = System.currentTimeMillis();
        if (code == null || code.trim().isEmpty()) return;
        InviteCodeDO row = mapper.selectByCode(code.trim());
        if (row == null) return;
        if (row.getExpiredAt() != null && row.getExpiredAt().isBefore(LocalDateTime.now())) return;
        if (row.getUseCount() != null && row.getMaxUses() != null
                && row.getUseCount() >= row.getMaxUses()) return;
        mapper.useCode(row.getId(), userId, row.getVersion());
        log.info("{} | {} {} | 入参({}) | 结果({}) | {}",
            BizLogHelper.trace(),
            BizLogHelper.who(userId),
            "使用邀请码",
            BizLogHelper.params(),
            BizLogHelper.result("used=true"),
            BizLogHelper.elapsed(_start));
    }

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

