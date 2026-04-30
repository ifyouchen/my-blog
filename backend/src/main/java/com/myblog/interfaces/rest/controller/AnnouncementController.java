package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.AnnouncementDTO;
import com.myblog.application.service.AnnouncementAppService;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告 REST 接口（公开）。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementAppService announcementAppService;

    public AnnouncementController(AnnouncementAppService announcementAppService) {
        this.announcementAppService = announcementAppService;
    }

    /**
     * 获取当前有效公告列表。
     *
     * @return 有效公告列表
     */
    @GetMapping("/active")
    public Result<List<AnnouncementDTO>> listActive() {
        return Result.success(announcementAppService.listActive());
    }
}

