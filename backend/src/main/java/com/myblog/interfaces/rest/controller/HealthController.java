package com.myblog.interfaces.rest.controller;

import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

    private final DataSource dataSource;

    /**
     * 创建健康检查接口。
     *
     * @param dataSource 数据源
     */
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取应用健康状态。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("status", "UP");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            data.put("database", metaData.getDatabaseProductName());
        } catch (Exception ex) {
            data.put("database", "UNKNOWN");
            data.put("status", "DEGRADED");
        }
        return Result.success(data);
    }
}
