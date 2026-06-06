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
 * 私信会话表兼容迁移初始化器。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class ConversationSchemaMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConversationSchemaMigrationInitializer.class);

    private final DataSource dataSource;

    /**
     * 创建私信会话表兼容迁移初始化器。
     *
     * @param dataSource 数据源
     */
    public ConversationSchemaMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 应用启动后补齐会话设置字段。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasTable(connection, "blog_conversation")) {
                return;
            }
            addColumnIfMissing(connection, "a_pinned", "ALTER TABLE blog_conversation "
                + "ADD COLUMN a_pinned tinyint(1) NOT NULL DEFAULT 0 COMMENT 'A方是否置顶' AFTER b_deleted_at");
            addColumnIfMissing(connection, "a_pinned_at", "ALTER TABLE blog_conversation "
                + "ADD COLUMN a_pinned_at datetime NULL COMMENT 'A方置顶时间' AFTER a_pinned");
            addColumnIfMissing(connection, "a_muted", "ALTER TABLE blog_conversation "
                + "ADD COLUMN a_muted tinyint(1) NOT NULL DEFAULT 0 COMMENT 'A方是否免打扰' AFTER a_pinned_at");
            addColumnIfMissing(connection, "b_pinned", "ALTER TABLE blog_conversation "
                + "ADD COLUMN b_pinned tinyint(1) NOT NULL DEFAULT 0 COMMENT 'B方是否置顶' AFTER a_muted");
            addColumnIfMissing(connection, "b_pinned_at", "ALTER TABLE blog_conversation "
                + "ADD COLUMN b_pinned_at datetime NULL COMMENT 'B方置顶时间' AFTER b_pinned");
            addColumnIfMissing(connection, "b_muted", "ALTER TABLE blog_conversation "
                + "ADD COLUMN b_muted tinyint(1) NOT NULL DEFAULT 0 COMMENT 'B方是否免打扰' AFTER b_pinned_at");
        } catch (SQLException ex) {
            log.warn("Conversation schema migration skipped: {}", ex.getMessage());
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
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, "blog_conversation", columnName)) {
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
