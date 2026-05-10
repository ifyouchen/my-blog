#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抓取菜鸟教程 MongoDB 教程并生成 SQL。"""

from __future__ import annotations

from runoob_course_crawler import CourseConfig, run


CONFIG = CourseConfig(
    slug="mongodb",
    seed_path="/mongodb/mongodb-tutorial.html",
    display_name="MongoDB 教程",
    category_name="MongoDB",
    article_id_start=631000,
    column_id=9600012,
    topic_id=9700012,
    tags=("MongoDB", "数据库", "NoSQL", "文档数据库"),
    cover_url="https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800",
    column_title="MongoDB 入门教程",
    topic_title="MongoDB 学习路径",
    intro=(
        "围绕 MongoDB 基础概念、集合与文档、查询、索引、聚合、复制和分片整理的学习路径。"
        "内容来源于菜鸟教程，平台侧以策展索引方式呈现，并保留原文链接。"
    ),
    difficulty="BEGINNER",
    sort_order=42,
    recommend_weight=934,
    section_title="MongoDB 入门",
)


if __name__ == "__main__":
    run(CONFIG)
