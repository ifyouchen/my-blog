package com.myblog.application.service;

import com.myblog.application.dto.AnnouncementDTO;
import com.myblog.infrastructure.repository.persistence.entity.AnnouncementDO;
import com.myblog.infrastructure.repository.persistence.mapper.AnnouncementMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公告应用服务。
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class AnnouncementAppService {

    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm[:ss]")
            .toFormatter();

    private static final Logger log = LoggerFactory.getLogger(AnnouncementAppService.class);

    private final AnnouncementMapper announcementMapper;

    public AnnouncementAppService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /**
     * 获取当前有效公告列表。
     *
     * @return 有效公告 DTO 列表
     */
    public List<AnnouncementDTO> listActive() {
        List<AnnouncementDO> list = announcementMapper.selectActive();
        List<AnnouncementDTO> result = new ArrayList<>();
        for (AnnouncementDO item : list) {
            result.add(toDTO(item));
        }
        return result;
    }

    /**
     * 管理员分页查询公告。
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public PageResult<AnnouncementDTO> pageAll(int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        int offset = (page - 1) * pageSize;
        List<AnnouncementDO> list = announcementMapper.selectPage(offset, pageSize);
        long total = announcementMapper.countAll();
        List<AnnouncementDTO> items = new ArrayList<>();
        for (AnnouncementDO item : list) {
            items.add(toDTO(item));
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    /**
     * 创建公告。
     *
     * @param params 请求参数 Map
     * @return 创建后的公告 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDTO create(Map<String, Object> params) {
        long _start = System.currentTimeMillis();
        AnnouncementDO d = new AnnouncementDO();
        String title = getString(params, "title");
        d.setTitle(title);
        d.setContent(getString(params, "content"));
        String target = getString(params, "target");
        d.setTarget(target != null ? target : "ALL");
        d.setPublished(0);
        String expiresAt = getString(params, "expiresAt");
        if (expiresAt != null && !expiresAt.isEmpty()) {
            d.setExpiresAt(LocalDateTime.parse(expiresAt, FMT));
        }
        announcementMapper.insert(d);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "创建公告",
            BizLogHelper.trace(),
            BizLogHelper.params("title", title),
            BizLogHelper.result("announcementId=" + d.getId()),
            BizLogHelper.elapsed(_start));
        return toDTO(d);
    }

    /**
     * 更新公告。
     *
     * @param id     公告 ID
     * @param params 请求参数 Map
     * @return 更新后的公告 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDTO update(Long id, Map<String, Object> params) {
        long _start = System.currentTimeMillis();
        AnnouncementDO existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        AnnouncementDO d = new AnnouncementDO();
        d.setId(id);
        String title = getString(params, "title");
        d.setTitle(title);
        d.setContent(getString(params, "content"));
        d.setTarget(getString(params, "target"));
        String expiresAt = getString(params, "expiresAt");
        if (expiresAt != null && !expiresAt.isEmpty()) {
            d.setExpiresAt(LocalDateTime.parse(expiresAt, FMT));
        }
        announcementMapper.updateByPrimaryKeySelective(d);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "更新公告",
            BizLogHelper.trace(),
            BizLogHelper.params("announcementId", id, "title", title),
            BizLogHelper.result("announcementId=" + id),
            BizLogHelper.elapsed(_start));
        return toDTO(announcementMapper.selectById(id));
    }

    /**
     * 删除公告（软删除）。
     *
     * @param id 公告 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long _start = System.currentTimeMillis();
        AnnouncementDO existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.softDelete(id);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "删除公告",
            BizLogHelper.trace(),
            BizLogHelper.params("announcementId", id),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 发布公告。
     *
     * @param id 公告 ID
     * @return 更新后的公告 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDTO publish(Long id) {
        long _start = System.currentTimeMillis();
        AnnouncementDO existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        AnnouncementDO d = new AnnouncementDO();
        d.setId(id);
        d.setPublished(1);
        d.setPublishedAt(LocalDateTime.now());
        announcementMapper.updateByPrimaryKeySelective(d);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "发布公告",
            BizLogHelper.trace(),
            BizLogHelper.params("announcementId", id),
            BizLogHelper.result("published=true"),
            BizLogHelper.elapsed(_start));
        return toDTO(announcementMapper.selectById(id));
    }

    /**
     * 撤回公告。
     *
     * @param id 公告 ID
     * @return 更新后的公告 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDTO unpublish(Long id) {
        long _start = System.currentTimeMillis();
        AnnouncementDO existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new ApplicationException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        AnnouncementDO d = new AnnouncementDO();
        d.setId(id);
        d.setPublished(0);
        announcementMapper.updateByPrimaryKeySelective(d);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "撤回公告",
            BizLogHelper.trace(),
            BizLogHelper.params("announcementId", id),
            BizLogHelper.result("published=false"),
            BizLogHelper.elapsed(_start));
        return toDTO(announcementMapper.selectById(id));
    }

    private AnnouncementDTO toDTO(AnnouncementDO d) {
        if (d == null) {
            return null;
        }
        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setId(d.getId());
        dto.setTitle(d.getTitle());
        dto.setContent(d.getContent());
        dto.setTarget(d.getTarget());
        dto.setPublished(d.getPublished() != null && d.getPublished() == 1);
        dto.setPublishedAt(d.getPublishedAt() != null ? d.getPublishedAt().format(FMT) : null);
        dto.setExpiresAt(d.getExpiresAt() != null ? d.getExpiresAt().format(FMT) : null);
        dto.setCreatedAt(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : null);
        dto.setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt().format(FMT) : null);
        return dto;
    }

    private String getString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val instanceof String ? (String) val : null;
    }
}

