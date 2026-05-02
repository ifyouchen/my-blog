package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.SensitiveWordAppService;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台敏感词提示接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/sensitive-words")
public class SensitiveWordController {

    private final SensitiveWordAppService sensitiveWordAppService;

    public SensitiveWordController(SensitiveWordAppService sensitiveWordAppService) {
        this.sensitiveWordAppService = sensitiveWordAppService;
    }

    /**
     * 查询 WARN 级别敏感词列表，用于前端提交前提示。
     *
     * @return WARN 级别敏感词列表
     */
    @GetMapping("/warn")
    public Result<List<String>> listWarnWords() {
        return Result.success(sensitiveWordAppService.listWarnWords());
    }
}
