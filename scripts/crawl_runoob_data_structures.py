#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抓取菜鸟教程数据结构教程并生成 SQL。"""

from __future__ import annotations

from runoob_course_crawler import CourseConfig, run


CONFIG = CourseConfig(
    slug="data_structures",
    seed_path="/data-structures/data-structures-tutorial.html",
    display_name="数据结构教程",
    category_name="数据结构",
    article_id_start=632000,
    column_id=9600013,
    topic_id=9700013,
    tags=("数据结构", "算法", "计算机基础", "编程基础"),
    cover_url="https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800",
    column_title="数据结构入门教程",
    topic_title="数据结构学习路径",
    intro=(
        "围绕数组、链表、栈、队列、树、图、哈希等基础数据结构整理的学习路径。"
        "内容来源于菜鸟教程，平台侧以策展索引方式呈现，并保留原文链接。"
    ),
    difficulty="BEGINNER",
    sort_order=43,
    recommend_weight=933,
    section_title="数据结构基础",
)


if __name__ == "__main__":
    run(CONFIG)
