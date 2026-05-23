SET NAMES utf8mb4;
USE `my_blog`;

-- Assign group_name to all existing categories based on their content area
-- Run this AFTER all article seed files have been imported

-- 编程语言
UPDATE blog_category SET group_name = '编程语言' WHERE name IN ('Java 基础','Java 集合框架','Java 多线程与并发','Java IO/NIO/AIO','Java8 特性详解','Java8 以上特性概述','Java其它相关') AND deleted_at IS NULL;

-- JVM
UPDATE blog_category SET group_name = 'JVM' WHERE name = 'JVM相关' AND deleted_at IS NULL;

-- 后端
UPDATE blog_category SET group_name = '后端' WHERE name IN ('Spring Framework 5基础','SpringBoot 2.5.x系列','Web容器 - Tomcat知识体系详解','ORM框架 - MyBatis知识体系详解','分表分库 - ShardingSphere详解','后端开发') AND deleted_at IS NULL;

-- 数据库
UPDATE blog_category SET group_name = '数据库' WHERE name IN ('数据库基础和原理','SQL 语言','SQL - MySQL','NoSQL - Redis','NoSQL - MongoDB','NoSQL - ElasticSearch','MongoDB','PostgreSQL') AND deleted_at IS NULL;

-- 数据结构与算法
UPDATE blog_category SET group_name = '数据结构与算法' WHERE name IN ('数据结构基础','常见排序算法','算法思想','一些领域算法','其它算法相关','数据结构') AND deleted_at IS NULL;

-- 架构
UPDATE blog_category SET group_name = '架构' WHERE name IN ('架构基础','分布式系统','微服务系统与设计','系统设计之商业业务平台','系统设计之日志系统平台','系统设计之网关设计') AND deleted_at IS NULL;

-- 开发工具
UPDATE blog_category SET group_name = '开发工具' WHERE name IN ('工具清单','开发工具详解','Linux','容器化 - Docker') AND deleted_at IS NULL;

-- 方法论
UPDATE blog_category SET group_name = '方法论' WHERE name IN ('开发理论','开源协议','代码规范','开发流程','设计模式','系统认证') AND deleted_at IS NULL;

-- AI
UPDATE blog_category SET group_name = 'AI' WHERE name IN ('AI Agent') AND deleted_at IS NULL;

-- 其他
UPDATE blog_category SET group_name = '其他' WHERE name IN ('开发之常用类库','开发之代码质量','开发之网络协议','开发之安全相关','开发之正则表达式','开发之CRON表达式','开发之常见重构技巧','开发之随手记','精华区','企业信息化其他','其他分类','项目总结') AND deleted_at IS NULL;

-- 面试 (already set by articles_interview_novel.sql, but ensure coverage)
UPDATE blog_category SET group_name = '面试' WHERE name IN ('简历','项目经验','面试题','面试技巧','职场进阶') AND deleted_at IS NULL;

-- 小说 (already set by articles_interview_novel.sql)
UPDATE blog_category SET group_name = '小说' WHERE name IN ('玄幻奇幻','武侠仙侠','都市生活','历史军事','科幻未来','悬疑推理','游戏竞技','轻小说') AND deleted_at IS NULL;

-- Ensure every category has a group name before creating first-class groups
UPDATE blog_category
SET group_name = '其他'
WHERE (group_name IS NULL OR group_name = '')
AND deleted_at IS NULL;

-- Create first-class category groups from existing group_name values
INSERT INTO blog_category_group (name, description, sort_order, enabled, created_at, updated_at, deleted_at, version)
SELECT grouped.group_name,
       CONCAT(grouped.group_name, ' 分类组'),
       grouped.sort_order,
       1,
       NOW(),
       NOW(),
       NULL,
       0
FROM (
    SELECT group_name,
           MIN(sort_order) AS sort_order
    FROM blog_category
    WHERE deleted_at IS NULL
      AND group_name IS NOT NULL
      AND group_name != ''
    GROUP BY group_name
) grouped
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    enabled = 1,
    updated_at = NOW(),
    deleted_at = NULL;

-- Backfill category.group_id from the first-class group table
UPDATE blog_category c
INNER JOIN blog_category_group g ON g.name = c.group_name AND g.deleted_at IS NULL
SET c.group_id = g.id,
    c.updated_at = NOW()
WHERE c.deleted_at IS NULL;

-- Keep ID generator in sync for installations that use the atomic sequence table
INSERT INTO id_sequence (`name`, `next_id`)
SELECT 'blog_category_group', COALESCE(MAX(id), 99) + 1
FROM blog_category_group
ON DUPLICATE KEY UPDATE next_id = GREATEST(next_id, VALUES(next_id));
