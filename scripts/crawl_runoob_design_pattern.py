#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抓取菜鸟教程设计模式教程并生成 SQL。"""

from __future__ import annotations

from runoob_course_crawler import CourseConfig, run


CONFIG = CourseConfig(
    slug="design_pattern",
    seed_path="/design-pattern/design-pattern-tutorial.html",
    display_name="设计模式教程",
    category_name="设计模式",
    article_id_start=633000,
    column_id=9600014,
    topic_id=9700014,
    tags=("设计模式", "架构", "面向对象", "代码设计"),
    cover_url="https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800",
    column_title="设计模式入门教程",
    topic_title="设计模式学习路径",
    intro=(
        "围绕创建型、结构型、行为型设计模式以及面向对象设计原则整理的学习路径。"
        "内容来源于菜鸟教程，平台侧以策展索引方式呈现，并保留原文链接。"
    ),
    difficulty="INTERMEDIATE",
    sort_order=44,
    recommend_weight=932,
    section_title="设计模式基础",
)


if __name__ == "__main__":
    run(CONFIG)
