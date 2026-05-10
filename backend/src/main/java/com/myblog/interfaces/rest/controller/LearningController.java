package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.LearningProgressDTO;
import com.myblog.application.service.LearningProgressAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.UpdateLearningProgressRequest;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习进度 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningProgressAppService learningProgressAppService;

    public LearningController(LearningProgressAppService learningProgressAppService) {
        this.learningProgressAppService = learningProgressAppService;
    }

    @PostMapping("/progress")
    public Result<LearningProgressDTO> updateProgress(@RequestBody UpdateLearningProgressRequest request) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return Result.success(learningProgressAppService.updateProgress(
            userId,
            request.getAssetType(),
            request.getAssetId(),
            request.getArticleId(),
            request.getCompleted()
        ));
    }
}
