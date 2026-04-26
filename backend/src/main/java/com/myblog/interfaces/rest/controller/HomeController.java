package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.service.HomeBootstrapAppService;
import com.myblog.application.service.HomeStatsAppService;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeStatsAppService homeStatsAppService;
    private final HomeBootstrapAppService homeBootstrapAppService;

    /**
     * 创建首页控制器。
     *
     * @param homeStatsAppService 首页统计应用服务
     */
    public HomeController(HomeStatsAppService homeStatsAppService,
                          HomeBootstrapAppService homeBootstrapAppService) {
        this.homeStatsAppService = homeStatsAppService;
        this.homeBootstrapAppService = homeBootstrapAppService;
    }

    /**
     * 获取首页统计数据。
     *
     * @return 首页统计数据
     */
    @GetMapping("/stats")
    public Result<HomeStatsAppService.HomeStats> getStats() {
        return Result.success(homeStatsAppService.getStats());
    }

    @GetMapping("/bootstrap")
    public Result<HomeBootstrapDTO> getBootstrap() {
        return Result.success(homeBootstrapAppService.getBootstrap());
    }
}
