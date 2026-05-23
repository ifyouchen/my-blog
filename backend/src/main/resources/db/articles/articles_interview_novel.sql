-- ============================================================
-- 面试 & 小说 分类种子数据
-- 生成时间：2026-05-23
-- ============================================================
SET NAMES utf8mb4;
USE `my_blog`;
START TRANSACTION;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('简历','面试','简历撰写与优化',100,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('项目经验','面试','项目介绍与亮点总结',101,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('面试题','面试','各技术栈面试真题',102,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('面试技巧','面试','面试流程与沟通策略',103,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('职场进阶','面试','晋升、跳槽与职业规划',104,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('玄幻奇幻','小说','玄幻、奇幻类小说',110,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('武侠仙侠','小说','武侠、仙侠、修真',111,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('都市生活','小说','都市、职场、现实题材',112,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('历史军事','小说','历史、架空、军事题材',113,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('科幻未来','小说','科幻、末世、未来题材',114,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('悬疑推理','小说','悬疑、侦探、推理',115,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('游戏竞技','小说','游戏、电竞、体育',116,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

INSERT INTO `blog_category` (`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)
VALUES ('轻小说','小说','轻小说、二次元',117,1,'NORMAL','2026-05-23 08:00:00','2026-05-23 08:00:00',NULL,0)
ON DUPLICATE KEY UPDATE `group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;

COMMIT;
