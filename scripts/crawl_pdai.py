#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
pdai.tech 全量文章爬虫 + 分类 SQL 生成脚本 (Python 3.8+ 兼容)

功能：
  - 按分类爬取 pdai.tech 全量文章
  - 进度持久化到 scripts/crawl_progress.json，支持断点续爬
  - 每个分类生成独立的 INSERT SQL 文件到 backend/src/main/resources/db/articles/

用法：
  cd /path/to/my-blog
  python3 scripts/crawl_pdai.py                          # 全量爬取（跳过已完成的）
  python3 scripts/crawl_pdai.py --category Java          # 只爬取指定分类
  python3 scripts/crawl_pdai.py --reset                  # 清空进度，重新开始
  python3 scripts/crawl_pdai.py --dry-run                # 只打印不写文件
  python3 scripts/crawl_pdai.py --delay 2 --timeout 30  # 调整基础间隔
  python3 scripts/crawl_pdai.py --retry 3               # 失败重试次数

依赖（一次安装即可）：
  pip3 install requests beautifulsoup4
"""

from __future__ import annotations

import argparse
import json
import os
import random
import re
import sys
import time
from datetime import datetime
from typing import Dict, List, Optional, Set, Tuple

try:
    import requests
    from bs4 import BeautifulSoup, NavigableString, Tag
except ImportError:
    print("缺少依赖，请先安装：pip3 install requests beautifulsoup4")
    sys.exit(1)

# ---------------------------------------------------------------------------
# 各分类文章列表（从 pdai.tech 导航整理，每条为 (title, url_path)）
# URL 自动补全为 https://pdai.tech{path}
# ---------------------------------------------------------------------------
BASE_URL = "https://pdai.tech"

# 分类定义：每个分类有 id_start（文章ID起始），name（分类名），articles（文章列表）
# articles 每项：(title, url_path)
CATEGORIES: List[Dict] = [
    {
        "name": "Java",
        "file": "articles_java.sql",
        "id_start": 10001,
        "articles": [
            # Java 面向对象和基础
            ("Java 面向对象基础", "/md/java/basic/java-basic-oop.html"),
            ("Java 基础知识体系", "/md/java/basic/java-basic-lan-basic.html"),
            # Java进阶 - 集合框架
            ("Java 集合框架详解", "/md/java/collection/java-collection-all.html"),
            # Java进阶 - 并发框架
            ("Java 并发知识体系", "/md/java/thread/java-thread-x-overview.html"),
            ("Java 并发理论基础", "/md/java/thread/java-thread-x-theorty.html"),
            ("Java 并发线程基础", "/md/java/thread/java-thread-x-thread-basic.html"),
            ("J.U.C 知识体系与详解", "/md/java/thread/java-thread-x-juc-overview.html"),
            # Java进阶 - IO框架
            ("Java IO/NIO/AIO详解", "/md/java/io/java-io-overview.html"),
            # Java进阶 - 新版本特性
            ("Java 8 特性详解", "/md/java/java8/java8.html"),
            ("Java 8 以上版本特性体系", "/md/java/java8up/java-8-up-overview.html"),
            ("Java 8 升Java 11特性必读", "/md/java/java8up/java9-11.html"),
            ("Java 11 升Java 17特性必读", "/md/java/java8up/java12-17.html"),
            # Java进阶 - JVM相关
            ("Java 类加载机制", "/md/java/jvm/java-jvm-classload.html"),
            ("Java 字节码和增强技术", "/md/java/jvm/java-jvm-class.html"),
            ("JVM 内存结构详解", "/md/java/jvm/java-jvm-struct.html"),
            ("JVM 垃圾回收机制", "/md/java/jvm/java-jvm-gc.html"),
            ("Java 调试排错相关", "/md/java/jvm/java-jvm-debug-tools-linux.html"),
        ],
    },
    {
        "name": "算法",
        "file": "articles_algorithm.sql",
        "id_start": 10100,
        "articles": [
            # 算法基础和思想
            ("数据结构基础", "/md/algorithm/alg-basic-overview.html"),
            ("常见排序算法", "/md/algorithm/alg-sort-overview.html"),
            ("算法思想", "/md/algorithm/alg-core-divide-and-conquer.html"),
            # 一些领域算法
            ("安全算法", "/md/algorithm/alg-domain-security-degist.html"),
            ("字符串匹配算法", "/md/algorithm/alg-domain-char-match.html"),
            ("分布式系统算法", "/md/algorithm/alg-domain-distribute-overview.html"),
            ("海量数据处理", "/md/algorithm/alg-domain-bigdata-overview.html"),
            ("负载均衡算法", "/md/algorithm/alg-domain-load-balance.html"),
            ("推荐算法", "/md/algorithm/alg-domain-suggest.html"),
            ("数据挖掘算法", "/md/algorithm/alg-domain-machine.html"),
            ("ID生成算法", "/md/algorithm/alg-domain-id-snowflake.html"),
            # 其它
            ("头脑风暴", "/md/algorithm/alg-other-mind.html"),
        ],
    },
    {
        "name": "数据库",
        "file": "articles_database.sql",
        "id_start": 10200,
        "articles": [
            # 数据库基础
            ("数据库原理", "/md/db/sql/sql-db.html"),
            ("SQL语言", "/md/db/sql-lan/sql-lan.html"),
            # SQL 数据库
            ("MySQL 详解", "/md/db/sql-mysql/sql-mysql-overview.html"),
            # NoSQL 数据库
            ("Redis 详解", "/md/db/nosql-redis/db-redis-overview.html"),
            ("MongoDB 详解", "/md/db/nosql-mongo/mongo.html"),
            ("ElasticSearch 详解", "/md/db/nosql-es/elasticsearch.html"),
        ],
    },
    {
        "name": "开发",
        "file": "articles_develop.sql",
        "id_start": 10300,
        "articles": [
            # 开发 - 常用开发基础
            ("常用类库详解", "/md/develop/package/dev-package-x-overview.html"),
            ("正则表达式详解", "/md/develop/regex/dev-regex-all.html"),
            ("CRON表达式详解", "/md/develop/cron/dev-cron-x-usage.html"),
            ("网络协议和工具详解", "/md/develop/protocol/dev-protocol-overview.html"),
            ("安全相关详解", "/md/develop/security/dev-security-overview.html"),
            # 开发 - 质量保障
            ("单元测试详解", "/md/develop/ut/dev-ut-unit-test.html"),
            ("统一风格详解", "/md/develop/ut/dev-qt-code-style.html"),
            ("质量管理详解", "/md/develop/ut/dev-qt-sonarlint.html"),
            # 开发 - 代码重构
            ("代码重构相关", "/md/develop/refactor/dev-refactor-if-else.html"),
        ],
    },
    {
        "name": "Spring",
        "file": "articles_spring.sql",
        "id_start": 10400,
        "articles": [
            # Spring Framework
            ("Spring框架知识体系", "/md/spring/spring.html"),
            ("Spring框架组成", "/md/spring/spring-x-framework-introduce.html"),
            ("控制反转(IOC)", "/md/spring/spring-x-framework-ioc.html"),
            ("面向切面编程(AOP)", "/md/spring/spring-x-framework-aop.html"),
            ("SpringMVC", "/md/spring/spring-x-framework-springmvc.html"),
            # SpringBoot
            ("SpringBoot知识体系", "/md/spring/springboot/springboot.html"),
            ("SpringBoot入门", "/md/spring/springboot/springboot-x-hello-world.html"),
            ("SpringBoot接口设计和实现", "/md/spring/springboot/springboot-x-interface-response.html"),
            ("SpringBoot集成MySQL(JPA)", "/md/spring/springboot/springboot-x-mysql-jpa.html"),
            ("SpringBoot集成MySQL(MyBatis)", "/md/spring/springboot/springboot-x-mysql-mybatis-xml.html"),
            ("SpringBoot集成ShardingJDBC", "/md/spring/springboot/springboot-x-mysql-shardingjdbc.html"),
            ("SpringBoot集成Redis", "/md/spring/springboot/springboot-x-redis-jedis.html"),
            ("SpringBoot集成Postgre", "/md/spring/springboot/springboot-x-postgre-jpa.html"),
            ("SpringBoot集成ElasticSearch", "/md/spring/springboot/springboot-x-elastic-template.html"),
            ("SpringBoot集成Socket", "/md/spring/springboot/springboot-x-socket-websocket.html"),
            ("SpringBoot定时任务", "/md/spring/springboot/springboot-x-task-timer.html"),
            ("SpringBoot后端视图", "/md/spring/springboot/springboot-x-view-thymeleaf.html"),
            ("SpringBoot监控", "/md/spring/springboot/springboot-x-monitor-actuator.html"),
            ("SpringBoot进阶与自动配置", "/md/spring/springboot/springboot-y-auo-config.html"),
            ("SpringBoot接口幂等性", "/md/spring/springboot/springboot-x-interface-mideng.html"),
            ("SpringBoot接口限流", "/md/spring/springboot/springboot-x-interface-xianliu.html"),
        ],
    },
    {
        "name": "框架",
        "file": "articles_framework.sql",
        "id_start": 10500,
        "articles": [
            # Web 容器
            ("Tomcat 源码详解", "/md/framework/tomcat/tomcat-overview.html"),
            # ORM 框架
            ("MyBatis 源码详解", "/md/framework/orm-mybatis/mybatis-overview.html"),
            # 分表分库框架
            ("ShardingSphere 详解", "/md/framework/ds-sharding/sharding-overview.html"),
        ],
    },
    {
        "name": "架构",
        "file": "articles_architecture.sql",
        "id_start": 10600,
        "articles": [
            # 架构基础和技术点
            ("架构知识体系", "/md/arch/arch-x-overview.html"),
            ("从角色视角看架构", "/md/arch/arch-x-view.html"),
            ("从分层视角看架构", "/md/arch/arch-x-view-2.html"),
            ("从演化视角看架构", "/md/arch/arch-x-evolution.html"),
            ("从模式视角看架构", "/md/arch/arch-x-pattern.html"),
            ("高并发之缓存", "/md/arch/arch-y-cache.html"),
            ("高并发之限流", "/md/arch/arch-y-ratelimit.html"),
            ("高并发之降级和熔断", "/md/arch/arch-y-reduce.html"),
            ("高可用之负载均衡", "/md/arch/arch-y-loadbalance.html"),
            ("高可用之容灾备份", "/md/arch/arch-y-backup.html"),
            # 分布式系统
            ("分布式理论和一致性算法", "/md/arch/arch-z-theory.html"),
            ("全局唯一ID实现方案", "/md/arch/arch-z-id.html"),
            ("分布式锁及实现方案", "/md/arch/arch-z-lock.html"),
            ("分布式事务及实现方案", "/md/arch/arch-z-transection.html"),
            ("分布式任务及实现方案", "/md/arch/arch-z-job.html"),
            ("分布式会话及实现方案", "/md/arch/arch-z-session.html"),
            # 微服务系统
            ("微服务系统和设计", "/md/arch/microservice/arch-m-singleton-2.html"),
            # 系统设计 - 商业业务平台
            ("秒杀抽奖相关设计", "/md/arch/arch-example-seckill.html"),
            ("电商交易相关设计", "/md/arch/arch-example-goods-detail.html"),
            ("仓储物流相关设计", "/md/arch/arch-example-meituan-jishiwuliu.html"),
            ("拉新投放相关设计", "/md/arch/arch-example-xianyu-laxintoufang.html"),
            ("其它综合相关设计", "/md/arch/arch-example-meituan-waimai.html"),
            # 系统设计 - 数据仓库平台
            ("数据库架构相关设计", "/md/arch/arch-example-meituan-db-hp.html"),
            ("数据同步相关设计", "/md/arch/arch-example-meituan-db-binlog.html"),
            ("数据仓库相关设计", "/md/arch/arch-example-meituan-data-cangchu.html"),
            ("数据治理相关设计", "/md/arch/arch-example-meituan-data-zhili.html"),
        ],
    },
    {
        "name": "工具与运维",
        "file": "articles_devops.sql",
        "id_start": 10700,
        "articles": [
            ("开发工具概览", "/md/devops/tool/tool-list-overview.html"),
            ("Git详解", "/md/devops/tool/tool-git.html"),
            ("Linux", "/md/devops/linux/linux.html"),
            ("Docker", "/md/devops/docker/docker-00-overview.html"),
        ],
    },
    {
        "name": "方法论",
        "file": "articles_methodology.sql",
        "id_start": 10800,
        "articles": [
            # 开发理论
            ("开发原则(SOLID)", "/md/dev-spec/spec/dev-th-solid.html"),
            ("分布式理论(CAP)", "/md/dev-spec/spec/dev-th-cap.html"),
            ("分布式理论(BASE)", "/md/dev-spec/spec/dev-th-base.html"),
            ("事务理论(ACID)", "/md/dev-spec/spec/dev-th-acid.html"),
            ("微服务理论(康威定律)", "/md/dev-spec/spec/dev-microservice-kangwei.html"),
            # 开发流程
            ("敏捷开发项目管理理论", "/md/dev-spec/dev-agile/dev-th-agile.html"),
            ("敏捷之极限编程(XP)", "/md/dev-spec/dev-agile/dev-th-agile-xp.html"),
            ("敏捷之Scrum & Kanban", "/md/dev-spec/dev-agile/dev-th-agile-scrum.html"),
            ("敏捷实践之测试驱动开发", "/md/dev-spec/dev-agile/dev-pratice-tdd.html"),
            # 设计模式
            ("设计模式详解", "/md/dev-spec/pattern/1_overview.html"),
        ],
    },
]

# ---------------------------------------------------------------------------
# 进度文件路径
# ---------------------------------------------------------------------------
PROGRESS_FILE = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "crawl_progress.json"
)

# 输出 SQL 目录
SQL_OUTPUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "backend", "src", "main", "resources", "db", "articles"
)

# 图片下载目录（对应后端 /api/uploads/files/pdai/ 访问路径）
IMG_DOWNLOAD_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "backend", "uploads", "pdai"
)
# 图片在前端可访问的 URL 前缀
IMG_URL_PREFIX = "/api/uploads/files/pdai"
# 下载图片时已处理的 URL -> 本地路径 缓存，避免重复下载
_img_cache: Dict[str, str] = {}

# ---------------------------------------------------------------------------
# 反爬：随机 User-Agent 池（模拟不同浏览器 / 系统）
# ---------------------------------------------------------------------------
_UA_POOL: List[str] = [
    # Chrome macOS
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    # Chrome Windows
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
    # Firefox
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:125.0) Gecko/20100101 Firefox/125.0",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
    # Safari
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
    # Edge
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0",
]

# 来源页面池：模拟从站内不同页面点击进入
_REFERER_POOL: List[str] = [
    "https://pdai.tech/",
    "https://pdai.tech/md/java/",
    "https://pdai.tech/md/db/",
    "https://pdai.tech/md/spring/",
    "https://pdai.tech/md/arch/",
    "https://pdai.tech/md/algorithm/",
    "https://www.google.com/search?q=pdai.tech+java",
    "https://www.bing.com/search?q=pdai.tech",
]


def _random_headers(referer: Optional[str] = None) -> Dict[str, str]:
    """每次请求随机生成 Headers，模拟真实浏览器行为"""
    ua = random.choice(_UA_POOL)
    ref = referer or random.choice(_REFERER_POOL)
    accept = random.choice([
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
    ])
    headers: Dict[str, str] = {
        "User-Agent": ua,
        "Accept": accept,
        "Accept-Language": random.choice([
            "zh-CN,zh;q=0.9,en;q=0.8",
            "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        ]),
        "Accept-Encoding": "gzip, deflate",
        "Connection": "keep-alive",
        "Referer": ref,
        "Upgrade-Insecure-Requests": "1",
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "same-origin" if "pdai.tech" in ref else "cross-site",
        "Sec-Fetch-User": "?1",
        "Cache-Control": random.choice(["max-age=0", "no-cache"]),
    }
    if random.random() > 0.5:
        headers["DNT"] = "1"
    return headers


# ---------------------------------------------------------------------------
# 反爬：人工节奏延迟
# ---------------------------------------------------------------------------

def _human_delay(base: float, idx: int) -> None:
    """
    模拟人工阅读节奏：
      - 基础抖动：在 base ± 40% 范围内随机浮动
      - 偶发"思考停顿"：约 20% 概率额外停顿 3-8s
      - 批次大休息：每处理 8-12 篇后，停顿 15-30s
    """
    jitter = base * random.uniform(0.6, 1.4)
    time.sleep(jitter)

    if random.random() < 0.20:
        pause = random.uniform(3.0, 8.0)
        print("  … 模拟阅读停顿 {:.1f}s".format(pause))
        time.sleep(pause)

    batch_size = getattr(_human_delay, "_batch", random.randint(8, 12))
    _human_delay._batch = batch_size  # type: ignore[attr-defined]
    count = getattr(_human_delay, "_count", 0) + 1
    _human_delay._count = count  # type: ignore[attr-defined]
    if count >= batch_size:
        rest = random.uniform(15.0, 30.0)
        print("  ☕ 批次休息 {:.0f}s（已处理 {} 篇）…".format(rest, count))
        time.sleep(rest)
        _human_delay._count = 0  # type: ignore[attr-defined]
        _human_delay._batch = random.randint(8, 12)  # type: ignore[attr-defined]


# ---------------------------------------------------------------------------
# 进度持久化
# ---------------------------------------------------------------------------

def load_progress() -> Dict:
    """加载爬取进度文件，返回 {url: article_dict} 字典"""
    if os.path.exists(PROGRESS_FILE):
        try:
            with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
                print("📂 已加载进度文件：{} 条已完成".format(len(data.get("done", {}))))
                return data
        except Exception as e:
            print("⚠ 进度文件读取失败，将从头开始：{}".format(e))
    return {"done": {}, "failed": []}


def save_progress(progress: Dict) -> None:
    """保存爬取进度到文件"""
    try:
        with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
            json.dump(progress, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print("⚠ 进度保存失败：{}".format(e), file=sys.stderr)


# ---------------------------------------------------------------------------
# 正文选择器（按优先级）
# ---------------------------------------------------------------------------
CONTENT_SELECTORS = [
    "div.theme-default-content",
    "div.content__default",
    ".markdown-body",
    "main article",
    "article",
    "main",
]

# ---------------------------------------------------------------------------
# 爬取
# ---------------------------------------------------------------------------

def download_image(src: str, timeout: int = 15) -> str:
    """
    下载图片到本地 IMG_DOWNLOAD_DIR，返回可访问的本地 URL。
    如果下载失败，返回原始 src（直接引用 pdai.tech）。
    使用内存缓存避免同一图片重复下载。
    """
    if src in _img_cache:
        return _img_cache[src]

    # 构建绝对 URL
    if src.startswith("//"):
        abs_url = "https:" + src
    elif src.startswith("/"):
        abs_url = BASE_URL + src
    elif src.startswith("http"):
        abs_url = src
    else:
        # 相对路径，无法处理，直接返回原路径
        _img_cache[src] = src
        return src

    try:
        os.makedirs(IMG_DOWNLOAD_DIR, exist_ok=True)
        # 从 URL 中取文件名，保留原始文件名以便去重
        url_path = abs_url.split("?")[0].rstrip("/")
        filename = url_path.split("/")[-1]
        if not filename or "." not in filename:
            filename = re.sub(r"[^a-zA-Z0-9_-]", "_", url_path.replace(BASE_URL, ""))
            filename = filename.strip("_")[:80] + ".png"

        local_path = os.path.join(IMG_DOWNLOAD_DIR, filename)
        # 已存在则跳过下载
        if not os.path.exists(local_path):
            headers = {
                "User-Agent": random.choice(_UA_POOL),
                "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
                "Accept-Encoding": "gzip, deflate",
                "Referer": BASE_URL + "/",
            }
            resp = requests.get(abs_url, headers=headers, timeout=timeout)
            resp.raise_for_status()
            with open(local_path, "wb") as f:
                f.write(resp.content)

        local_url = "{}/{}".format(IMG_URL_PREFIX, filename)
        _img_cache[src] = local_url
        return local_url

    except Exception as e:
        # 下载失败：fallback 到 pdai.tech 原始链接
        fallback = abs_url if abs_url.startswith("http") else src
        _img_cache[src] = fallback
        print("  ⚠ 图片下载失败 {}：{}".format(src[:60], e), file=sys.stderr)
        return fallback


def fetch_url(url: str, timeout: int = 20, retry: int = 2,
              prev_url: Optional[str] = None) -> Optional[str]:
    """
    抓取 URL 返回 HTML。每次随机生成 Headers，失败后指数退避重试。
    """
    referer = prev_url or random.choice(_REFERER_POOL)
    for attempt in range(1, retry + 2):
        try:
            headers = _random_headers(referer=referer)
            r = requests.get(url, headers=headers, timeout=timeout)
            r.raise_for_status()
            r.encoding = "utf-8"
            return r.text
        except Exception as e:
            if attempt <= retry:
                backoff = random.uniform(3.0, 6.0) * attempt
                print("  ⚠ 第 {} 次重试（{:.1f}s 后）: {}".format(
                    attempt, backoff, e), file=sys.stderr)
                time.sleep(backoff)
                referer = random.choice(_REFERER_POOL)
            else:
                print("  ✗ 请求失败（已重试 {} 次）: {}".format(retry, e), file=sys.stderr)
    return None


def extract_content(html_text: str, url: str) -> Tuple[str, str]:
    """从 HTML 中提取标题和正文（Markdown 格式）"""
    soup = BeautifulSoup(html_text, "html.parser")

    # 提取标题
    title = ""
    h1 = soup.find("h1")
    if h1:
        title = h1.get_text(strip=True)
    if not title:
        og = soup.find("meta", {"property": "og:title"})
        if og:
            title = og.get("content", "").strip()
    if not title:
        t = soup.find("title")
        if t:
            title = t.get_text(strip=True).split("|")[0].strip()

    # 找正文容器
    content_el = None
    for sel in CONTENT_SELECTORS:
        content_el = soup.select_one(sel)
        if content_el:
            break
    if not content_el:
        content_el = soup.body or soup

    # 移除干扰元素
    for tag in content_el.select(
        "nav, header, footer, .sidebar, script, style, button, "
        "[class*='sidebar'], [class*='nav'], [class*='footer'], "
        "[class*='toc'], [class*='edit-link'], [class*='last-updated']"
    ):
        tag.decompose()

    # 下载图片到本地，并将 img 的 src 替换为本地路径
    for img_tag in content_el.find_all("img"):
        src = img_tag.get("src", "").strip()
        if not src:
            continue
        local_url = download_image(src)
        img_tag["src"] = local_url

    md = _el_to_md(content_el)
    md = re.sub(r"\n{3,}", "\n\n", md).strip()
    if len(md) < 200:
        md = "## {}\n\n> 内容获取失败，请访问原文：{}\n".format(title, url)
    return title, md


# ---------------------------------------------------------------------------
# HTML -> Markdown 转换
# ---------------------------------------------------------------------------

def _el_to_md(el) -> str:
    lines: List[str] = []
    _walk(el, lines)
    return "\n".join(lines)


def _app(lines: List[str], text: str):
    if lines:
        lines[-1] += text
    else:
        lines.append(text)


def _walk(el, lines: List[str]):
    if isinstance(el, NavigableString):
        t = str(el).strip()
        if t:
            _app(lines, t)
        return
    if not isinstance(el, Tag):
        return
    n = el.name.lower() if el.name else ""

    if n in ("h1", "h2", "h3", "h4", "h5", "h6"):
        text = el.get_text(" ", strip=True)
        if text:
            lines.extend(["", "#" * int(n[1]) + " " + text, ""])
        return

    if n == "pre":
        code = el.find("code")
        raw = (code or el).get_text()
        lang = ""
        if code:
            for c in (code.get("class") or []):
                if c.startswith("language-"):
                    lang = c[9:]
                    break
        lines.extend(["", "```" + lang] + raw.splitlines() + ["```", ""])
        return

    if n == "code":
        _app(lines, "`" + el.get_text(strip=True) + "`")
        return

    if n == "p":
        # 如果 p 里含有 img，需要递归处理以保留图片；否则直接取文字
        has_img = bool(el.find("img"))
        if has_img:
            inner: List[str] = []
            for child in el.children:
                _walk(child, inner)
            block = "".join(inner).strip()
            if block:
                lines.extend(["", block, ""])
        else:
            text = el.get_text(" ", strip=True)
            if text:
                lines.extend(["", text, ""])
        return

    if n in ("ul", "ol"):
        lines.append("")
        for i, li in enumerate(el.find_all("li", recursive=False)):
            pfx = "{}. ".format(i + 1) if n == "ol" else "- "
            # li 里也可能有 img
            if li.find("img"):
                inner: List[str] = []
                for child in li.children:
                    _walk(child, inner)
                lines.append(pfx + "".join(inner).strip())
            else:
                lines.append(pfx + li.get_text(" ", strip=True))
        lines.append("")
        return

    if n == "table":
        rows = el.find_all("tr")
        if rows:
            lines.append("")
            for i, row in enumerate(rows):
                cells = [c.get_text(" ", strip=True) for c in row.find_all(["th", "td"])]
                lines.append("| " + " | ".join(cells) + " |")
                if i == 0:
                    lines.append("| " + " | ".join(["---"] * len(cells)) + " |")
            lines.append("")
        return

    if n == "blockquote":
        lines.extend(["", "> " + el.get_text(" ", strip=True), ""])
        return

    if n == "hr":
        lines.extend(["", "---", ""])
        return

    if n == "br":
        lines.append("")
        return

    if n in ("strong", "b"):
        _app(lines, "**" + el.get_text(strip=True) + "**")
        return

    if n in ("em", "i"):
        _app(lines, "*" + el.get_text(strip=True) + "*")
        return

    if n == "img":
        src = el.get("src", "").strip()
        alt = el.get("alt", "").strip() or "图片"
        if src:
            # src 已在 extract_content 中替换为本地路径或 fallback URL
            lines.extend(["", "![{}]({})".format(alt, src), ""])
        return

    if n == "a":
        text = el.get_text(strip=True)
        if text:
            _app(lines, text)
        return

    for child in el.children:
        _walk(child, lines)


# ---------------------------------------------------------------------------
# SQL 生成（INSERT 语句）
# ---------------------------------------------------------------------------

def _esc(s: str) -> str:
    """转义 SQL 字符串"""
    s = s.replace("\\", "\\\\").replace("'", "\\'")
    s = s.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n")
    return s.replace("\t", "\\t")


def _slug(url_path: str) -> str:
    """将 URL 路径转为 slug，如 /md/java/basic/java-basic-oop.html -> java-basic-oop"""
    name = os.path.basename(url_path)
    return re.sub(r"\.html$", "", name)


def _summary(content: str, max_len: int = 250) -> str:
    """提取摘要"""
    parts = []
    for line in content.splitlines():
        s = line.strip()
        if (s and len(s) > 10
                and not s.startswith(("#", "|", "```", ">", "-"))):
            parts.append(re.sub(r"[`*]", "", s))
        if len(parts) >= 4:
            break
    text = " ".join(parts)
    return (text[:max_len] + "...") if len(text) > max_len else (text or "暂无摘要")


# 封面图按分类随机选取（Unsplash 技术类图片，确保可访问）
_COVER_URLS: Dict[str, List[str]] = {
    "Java": [
        "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
        "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800",
    ],
    "算法": [
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
        "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800",
        "https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=800",
    ],
    "数据库": [
        "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=800",
        "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800",
        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800",
    ],
    "开发": [
        "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=800",
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800",
        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
    ],
    "Spring": [
        "https://images.unsplash.com/photo-1550439062-609e1531270e?w=800",
        "https://images.unsplash.com/photo-1607798748738-b15c40d33d57?w=800",
        "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=800",
    ],
    "框架": [
        "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800",
        "https://images.unsplash.com/photo-1596003906949-67221c37965c?w=800",
    ],
    "架构": [
        "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=800",
        "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800",
        "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800",
    ],
    "工具与运维": [
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=800",
        "https://images.unsplash.com/photo-1602576666092-bf6447a729fc?w=800",
        "https://images.unsplash.com/photo-1585079542156-2755d9c8a094?w=800",
    ],
    "方法论": [
        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800",
        "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800",
        "https://images.unsplash.com/photo-1552664730-d307ca884978?w=800",
    ],
}
_DEFAULT_COVERS = [
    "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
    "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
]


def _cover(category: str) -> str:
    pool = _COVER_URLS.get(category, _DEFAULT_COVERS)
    return random.choice(pool)


def generate_category_sql(category_name: str, articles: List[Dict],
                          output_path: str, dry_run: bool = False) -> None:
    """为一个分类生成 INSERT SQL 文件"""
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    published_base = datetime(2025, 6, 1)

    col_names = (
        "`id`,`author_id`,`title`,`summary`,`content`,`cover_url`,"
        "`category`,`offline_reason`,`featured`,`featured_at`,`slug`,"
        "`seo_title`,`seo_description`,`warn_flag`,`status`,"
        "`view_count`,`like_count`,`favorite_count`,`comment_count`,"
        "`published_at`,`created_at`,`updated_at`,`deleted_at`,`version`"
    )

    lines = [
        "-- ============================================================",
        "-- 分类：{}  共 {} 篇  生成时间：{}".format(category_name, len(articles), now),
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "",
        "INSERT INTO `blog_article` ({}) VALUES".format(col_names),
    ]

    rows = []
    for i, art in enumerate(articles):
        aid = art["id"]
        title = _esc(art["title"])
        summary = _esc(_summary(art["content"]))
        content = _esc(art["content"])
        cover = _esc(_cover(category_name))
        category = _esc(category_name)
        slug = _esc(art["slug"])
        seo_title = _esc(art["title"] + " - 技术博客")
        seo_desc = _esc(_summary(art["content"], 120))

        # 发布时间：从 2025-06-01 起，每篇间隔 1-3 天（模拟真实发布节奏）
        days_offset = i * random.randint(1, 3)
        # 简单计算天数偏移
        pub_ts = published_base.timestamp() + days_offset * 86400
        pub_dt = datetime.fromtimestamp(pub_ts).strftime("%Y-%m-%d %H:%M:%S")

        # 随机阅读量/点赞数
        view_count = random.randint(200, 8000)
        like_count = random.randint(10, int(view_count * 0.15))
        fav_count = random.randint(5, int(view_count * 0.08))
        comment_count = random.randint(0, int(view_count * 0.02))

        # 作者：1或2（admin/陈知夏 轮流）
        author_id = 1 if i % 2 == 0 else 2

        row = (
            "({aid},{author_id},'{title}','{summary}','{content}',"
            "'{cover_url}','{category}',NULL,0,NULL,'{slug}',"
            "'{seo_title}','{seo_desc}',0,'PUBLISHED',"
            "{view},{like},{fav},{cmt},"
            "'{pub_at}','{created}','{updated}',NULL,0)"
        ).format(
            aid=aid, author_id=author_id,
            title=title, summary=summary, content=content,
            cover_url=cover, category=category,
            slug=slug, seo_title=seo_title, seo_desc=seo_desc,
            view=view_count, like=like_count, fav=fav_count, cmt=comment_count,
            pub_at=pub_dt, created=pub_dt, updated=now,
        )
        rows.append(row)

    lines.append(",\n".join(rows) + ";")
    lines.append("")
    sql = "\n".join(lines)

    if dry_run:
        print(sql[:2000] + "\n... (dry-run, 仅显示前2000字符)")
    else:
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(sql)
        print("  💾 SQL 已写入：{} ({} 条 INSERT)".format(
            os.path.basename(output_path), len(articles)))


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser(description="爬取 pdai.tech 全量文章，按分类生成 INSERT SQL")
    p.add_argument("--category", default="",
                   help="只处理指定分类，例如：--category Java（多个用逗号分隔）")
    p.add_argument("--dry-run", action="store_true",
                   help="仅打印 SQL，不写文件")
    p.add_argument("--reset", action="store_true",
                   help="清空进度文件，重新爬取所有文章")
    p.add_argument("--delay", type=float, default=2.0,
                   help="每次请求基础间隔秒数（默认 2.0，会随机抖动）")
    p.add_argument("--timeout", type=int, default=20,
                   help="HTTP 超时秒数（默认 20）")
    p.add_argument("--retry", type=int, default=2,
                   help="失败后最大重试次数（默认 2）")
    p.add_argument("--limit", type=int, default=0,
                   help="最多爬取 N 篇（0=不限制，用于测试）")
    args = p.parse_args()

    # 过滤分类
    filter_cats: Set[str] = set()
    if args.category:
        filter_cats = {c.strip() for c in args.category.split(",")}

    # 加载 / 重置进度
    if args.reset and os.path.exists(PROGRESS_FILE):
        os.remove(PROGRESS_FILE)
        print("🗑  已清空进度文件，将重新爬取所有文章")
    progress = load_progress()
    done_urls: Dict[str, Dict] = progress.get("done", {})

    # 确定本次要处理的分类
    categories_to_run = [
        cat for cat in CATEGORIES
        if not filter_cats or cat["name"] in filter_cats
    ]
    if not categories_to_run:
        print("⚠ 未找到匹配的分类：{}".format(args.category))
        sys.exit(1)

    total_articles = sum(len(c["articles"]) for c in categories_to_run)
    pending_articles = sum(
        sum(1 for _, path in c["articles"]
            if BASE_URL + path not in done_urls)
        for c in categories_to_run
    )
    print("📋 分类：{}  总文章：{}  待爬取：{}\n".format(
        len(categories_to_run), total_articles, pending_articles))

    global_idx = 0
    limit = args.limit if args.limit > 0 else 9999999
    global_total = min(pending_articles, limit)
    prev_url: Optional[str] = None

    for cat in categories_to_run:
        cat_name = cat["name"]
        id_start = cat["id_start"]
        sql_file = os.path.join(
            os.path.normpath(SQL_OUTPUT_DIR),
            cat["file"]
        )

        print("\n" + "=" * 55)
        print("📂 分类：{}  ({} 篇)".format(cat_name, len(cat["articles"])))
        print("=" * 55)

        cat_results: List[Dict] = []

        for art_idx, (default_title, url_path) in enumerate(cat["articles"]):
            url = BASE_URL + url_path
            art_id = id_start + art_idx

            # 断点续爬：已完成的直接从进度中读取
            if url in done_urls:
                cached = done_urls[url]
                print("[cached] ID={} {}".format(art_id, default_title))
                cat_results.append(cached)
                continue

            # 达到 limit 时停止爬取
            if global_idx >= limit:
                print("\n[LIMIT] 已达到 --limit={} 限制，停止爬取".format(limit))
                break
            global_idx += 1
            print("[{}/{}] ID={}  {}".format(global_idx, global_total, art_id, url))

            html = fetch_url(url, timeout=args.timeout, retry=args.retry,
                             prev_url=prev_url)
            if html is None:
                print("  ✗ 跳过（将加入失败列表，可重新运行重试）")
                failed_entry = {"url": url, "id": art_id, "title": default_title,
                                "category": cat_name}
                progress.setdefault("failed", [])
                # 避免重复添加
                if url not in [f["url"] for f in progress["failed"]]:
                    progress["failed"].append(failed_entry)
                save_progress(progress)
            else:
                t, c = extract_content(html, url)
                final_title = t or default_title
                print("  ✓ {}（{} 字符）".format(final_title, len(c)))

                art_data: Dict = {
                    "id": art_id,
                    "title": final_title,
                    "category": cat_name,
                    "content": c,
                    "url": url,
                    "slug": _slug(url_path),
                }
                cat_results.append(art_data)
                done_urls[url] = art_data
                progress["done"] = done_urls
                # 每爬完一篇就保存进度（防止中途中断丢失）
                save_progress(progress)
                prev_url = url

            # 人工节奏延迟（非最后一篇）
            if global_idx < global_total:
                _human_delay(args.delay, global_idx)

        # 当前分类爬完，生成 SQL
        if cat_results:
            print("\n  ✍ 生成分类 SQL...")
            generate_category_sql(cat_name, cat_results, sql_file, dry_run=args.dry_run)
        else:
            print("  ⚠ 分类 {} 无可用数据，跳过 SQL 生成".format(cat_name))

    # 汇总
    print("\n" + "=" * 55)
    failed_list = progress.get("failed", [])
    print("✅ 累计完成：{}  ✗ 失败：{}".format(len(done_urls), len(failed_list)))
    if failed_list:
        print("\n失败列表（重新运行脚本会自动重试，或用 --reset 全部重跑）：")
        for f in failed_list:
            print("  ID={}  {}  {}".format(f["id"], f["title"], f["url"]))
    else:
        print("🎉 所有文章爬取成功！")

    if not args.dry_run:
        print("\nSQL 文件位于：{}".format(os.path.normpath(SQL_OUTPUT_DIR)))


if __name__ == "__main__":
    main()

