package com.myblog.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 分类组历史数据迁移初始化器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class CategoryGroupMigrationInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    /**
     * 创建分类组历史数据迁移初始化器。
     *
     * @param dataSource 数据源
     */
    public CategoryGroupMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 应用启动后补齐分类组数据。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/category_groups_migration.sql"));
        populator.execute(dataSource);
    }
}
