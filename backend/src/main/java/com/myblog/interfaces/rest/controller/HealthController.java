package com.myblog.interfaces.rest.controller;

import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 获取应用健康状态。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("status", "UP");
        data.put("database", "in-memory");
        return Result.success(data);
    }
}
