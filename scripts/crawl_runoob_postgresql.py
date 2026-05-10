#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抓取菜鸟教程 PostgreSQL 教程并生成 SQL。"""

from __future__ import annotations

from runoob_course_crawler import CourseConfig, run


CONFIG = CourseConfig(
    slug="postgresql",
    seed_path="/postgresql/postgresql-tutorial.html",
    display_name="PostgreSQL 教程",
    category_name="PostgreSQL",
    article_id_start=630000,
    column_id=9600011,
    topic_id=9700011,
    tags=("PostgreSQL", "数据库", "SQL", "关系型数据库"),
    cover_url="https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=800",
    column_title="PostgreSQL 入门教程",
    topic_title="PostgreSQL 学习路径",
    intro=(
        "围绕 PostgreSQL 安装、基础语法、数据类型、表操作、查询、函数和高级特性整理的学习路径。"
        "内容来源于菜鸟教程，平台侧以策展索引方式呈现，并保留原文链接。"
    ),
    difficulty="BEGINNER",
    sort_order=41,
    recommend_weight=935,
    section_title="PostgreSQL 入门",
)


if __name__ == "__main__":
    run(CONFIG)
