package com.myblog.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 评论表兼容迁移初始化器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class CommentSchemaMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommentSchemaMigrationInitializer.class);

    private final DataSource dataSource;

    /**
     * 创建评论表兼容迁移初始化器。
     *
     * @param dataSource 数据源
     */
    public CommentSchemaMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 应用启动后补齐评论引用字段。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasTable(connection, "blog_comment")) {
                return;
            }
            addColumnIfMissing(connection, "quote_text", "ALTER TABLE blog_comment "
                + "ADD COLUMN quote_text varchar(300) NULL COMMENT '引用原文' AFTER content");
            addColumnIfMissing(connection, "quote_prefix", "ALTER TABLE blog_comment "
                + "ADD COLUMN quote_prefix varchar(80) NULL COMMENT '引用前文' AFTER quote_text");
            addColumnIfMissing(connection, "quote_suffix", "ALTER TABLE blog_comment "
                + "ADD COLUMN quote_suffix varchar(80) NULL COMMENT '引用后文' AFTER quote_prefix");
        } catch (SQLException ex) {
            log.warn("Comment schema migration skipped: {}", ex.getMessage());
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, "blog_comment", columnName)) {
            return resultSet.next();
        }
    }

    private void addColumnIfMissing(Connection connection, String columnName, String sql) throws SQLException {
        if (hasColumn(connection, columnName)) {
            return;
        }
        execute(connection, sql);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
