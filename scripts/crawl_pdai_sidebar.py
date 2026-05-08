#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
pdai.tech 侧栏目录全量爬虫。

旧版 crawl_pdai.py 使用手工维护的 CATEGORIES 列表，只覆盖了每个大类的入口/概览文章。
这个版本会先解析 pdai VuePress 页面里的 ul.sidebar-items，再按侧栏中的真实文章链接抓取正文。

用法：
  python scripts/crawl_pdai_sidebar.py --catalog-only
  python scripts/crawl_pdai_sidebar.py --category Java --limit 3 --dry-run
  python scripts/crawl_pdai_sidebar.py --category Java
  python scripts/crawl_pdai_sidebar.py --reset --delay 2 --timeout 30 --retry 3

依赖：
  python -m pip install requests beautifulsoup4
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import random
import re
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Sequence, Set, Tuple
from urllib.parse import unquote, urljoin, urlsplit

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

try:
    import requests
    from bs4 import BeautifulSoup, NavigableString, Tag
except ImportError:
    print("缺少依赖，请先安装：python -m pip install requests beautifulsoup4")
    sys.exit(1)


BASE_URL = "https://pdai.tech"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.normpath(os.path.join(SCRIPT_DIR, ".."))

PROGRESS_FILE = os.path.join(SCRIPT_DIR, "crawl_pdai_sidebar_progress.json")
CATALOG_FILE = os.path.join(SCRIPT_DIR, "pdai_sidebar_catalog.json")
SQL_OUTPUT_DIR = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "db", "articles")
IMG_DOWNLOAD_DIR = os.path.join(PROJECT_ROOT, "backend", "uploads", "pdai")
IMG_URL_PREFIX = "/api/uploads/files/pdai"
PDAI_COLUMN_ID_BASE = 9200000
PDAI_TOPIC_ID_BASE = 9300000
PDAI_TOPIC_ID_STRIDE = 1000


@dataclass(frozen=True)
class CategorySpec:
    name: str
    file: str
    id_start: int
    seed_path: str
    prefixes: Tuple[str, ...]


# 每个分类只维护一个入口页；真实文章列表从该页左侧 sidebar-items 自动发现。
# id_start 使用大段范围，避免完整侧栏文章数量超过旧版每类 100 的空间后发生主键重叠。
CATEGORY_SPECS: Tuple[CategorySpec, ...] = (
    CategorySpec(
        "Java",
        "articles_pdai_full_java.sql",
        100000,
        "/md/java/basic/java-basic-oop.html",
        ("/md/java/",),
    ),
    CategorySpec(
        "算法",
        "articles_pdai_full_algorithm.sql",
        200000,
        "/md/algorithm/alg-basic-overview.html",
        ("/md/algorithm/",),
    ),
    CategorySpec(
        "数据库",
        "articles_pdai_full_database.sql",
        300000,
        "/md/db/sql/sql-db.html",
        ("/md/db/",),
    ),
    CategorySpec(
        "开发",
        "articles_pdai_full_develop.sql",
        400000,
        "/md/develop/package/dev-package-x-overview.html",
        ("/md/develop/",),
    ),
    CategorySpec(
        "Spring",
        "articles_pdai_full_spring.sql",
        500000,
        "/md/spring/spring.html",
        ("/md/spring/",),
    ),
    CategorySpec(
        "框架",
        "articles_pdai_full_framework.sql",
        600000,
        "/md/framework/tomcat/tomcat-overview.html",
        ("/md/framework/",),
    ),
    CategorySpec(
        "架构",
        "articles_pdai_full_architecture.sql",
        700000,
        "/md/arch/arch-x-overview.html",
        ("/md/arch/",),
    ),
    CategorySpec(
        "工具与运维",
        "articles_pdai_full_devops.sql",
        800000,
        "/md/devops/tool/tool-list-overview.html",
        ("/md/devops/",),
    ),
    CategorySpec(
        "方法论",
        "articles_pdai_full_methodology.sql",
        900000,
        "/md/dev-spec/spec/dev-th-solid.html",
        ("/md/dev-spec/",),
    ),
)


UA_POOL: Tuple[str, ...] = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
)

REFERER_POOL: Tuple[str, ...] = (
    "https://pdai.tech/",
    "https://pdai.tech/md/java/",
    "https://pdai.tech/md/db/",
    "https://pdai.tech/md/spring/",
    "https://pdai.tech/md/arch/",
)

CONTENT_SELECTORS: Tuple[str, ...] = (
    "main .theme-default-content",
    "div.theme-default-content",
    "div.content__default",
    ".markdown-body",
    "main article",
    "article",
    "main",
)

COVER_URLS: Dict[str, Tuple[str, ...]] = {
    "Java": (
        "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
        "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800",
    ),
    "算法": (
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800",
        "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800",
        "https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=800",
    ),
    "数据库": (
        "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=800",
        "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800",
        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800",
    ),
    "开发": (
        "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=800",
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800",
        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
    ),
    "Spring": (
        "https://images.unsplash.com/photo-1550439062-609e1531270e?w=800",
        "https://images.unsplash.com/photo-1607798748738-b15c40d33d57?w=800",
        "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=800",
    ),
    "框架": (
        "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800",
        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800",
        "https://images.unsplash.com/photo-1596003906949-67221c37965c?w=800",
    ),
    "架构": (
        "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=800",
        "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800",
        "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=800",
    ),
    "工具与运维": (
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=800",
        "https://images.unsplash.com/photo-1602576666092-bf6447a729fc?w=800",
        "https://images.unsplash.com/photo-1585079542156-2755d9c8a094?w=800",
    ),
    "方法论": (
        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800",
        "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800",
        "https://images.unsplash.com/photo-1552664730-d307ca884978?w=800",
    ),
}

DEFAULT_COVERS: Tuple[str, ...] = (
    "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
    "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
)

TAG_RULES: Tuple[Tuple[str, Tuple[str, ...]], ...] = (
    ("面向对象", ("面向对象", "oop")),
    ("泛型", ("泛型", "generic")),
    ("注解", ("注解", "annotation")),
    ("异常", ("异常", "exception")),
    ("反射", ("反射", "reflection")),
    ("SPI", ("spi",)),
    ("集合框架", ("集合", "collection", "arraylist", "linkedlist", "hashmap", "hashset", "treemap", "treeset")),
    ("并发编程", ("并发", "thread", "concurrent", "synchronized", "volatile", "lock", "aqs")),
    ("JUC", ("juc", "atomic", "futuretask", "threadpoolexecutor", "blockingqueue", "copyonwrite")),
    ("线程池", ("线程池", "executor", "threadpool")),
    ("JVM", ("jvm", "classload", "gc", "字节码", "内存结构")),
    ("IO", (" io", "/io/", "java-io", "bio", "nio", "aio", "netty", "零拷贝")),
    ("Java 8", ("java8", "java 8", "lambda", "stream", "optional", "localdatetime", "completablefuture")),
    ("Java 新版本", ("java9", "java10", "java11", "java12", "java17", "java 11", "java 17")),
    ("源码解析", ("源码", "source", "code", "解析")),
    ("设计模式", ("设计模式", "pattern")),
    ("MySQL", ("mysql",)),
    ("Redis", ("redis",)),
    ("MongoDB", ("mongodb", "mongo")),
    ("ElasticSearch", ("elasticsearch", "elastic")),
    ("Spring", ("spring",)),
    ("Spring Boot", ("springboot", "spring boot")),
    ("Docker", ("docker",)),
    ("Linux", ("linux",)),
    ("算法", ("算法", "algorithm", "sort", "tree", "graph")),
    ("架构设计", ("架构", "arch", "高并发", "高可用", "分布式")),
)

_img_cache: Dict[str, str] = {}


class RateLimitedError(Exception):
    def __init__(self, url: str, retry_after: Optional[int] = None):
        super().__init__("429 Too Many Requests: {}".format(url))
        self.url = url
        self.retry_after = retry_after


def random_headers(referer: Optional[str] = None, image: bool = False) -> Dict[str, str]:
    accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8" if image else (
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    )
    ref = referer or random.choice(REFERER_POOL)
    return {
        "User-Agent": random.choice(UA_POOL),
        "Accept": accept,
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Accept-Encoding": "gzip, deflate",
        "Connection": "keep-alive",
        "Referer": ref,
        "Upgrade-Insecure-Requests": "1",
        "Cache-Control": random.choice(("max-age=0", "no-cache")),
    }


def fetch_url(session: requests.Session, url: str, timeout: int, retry: int,
              referer: Optional[str] = None, stop_on_429: bool = True,
              cooldown_429: int = 0) -> Optional[str]:
    last_referer = referer or random.choice(REFERER_POOL)
    for attempt in range(1, retry + 2):
        try:
            resp = session.get(url, headers=random_headers(last_referer), timeout=timeout)
            if resp.status_code == 429:
                retry_after = parse_retry_after(resp.headers.get("Retry-After"))
                wait_seconds = retry_after if retry_after is not None else cooldown_429
                if wait_seconds and wait_seconds > 0:
                    print("  ⚠ 命中 429，按限流等待 {}s 后停止本轮抓取".format(wait_seconds))
                    time.sleep(wait_seconds)
                if stop_on_429:
                    raise RateLimitedError(url, retry_after)
            resp.raise_for_status()
            resp.encoding = "utf-8"
            return resp.text
        except Exception as exc:
            if isinstance(exc, RateLimitedError):
                raise
            if attempt <= retry:
                backoff = random.uniform(3.0, 6.0) * attempt
                print("  ⚠ 第 {} 次重试（{:.1f}s 后）: {}".format(attempt, backoff, exc))
                time.sleep(backoff)
                last_referer = random.choice(REFERER_POOL)
            else:
                print("  ✗ 请求失败（已重试 {} 次）: {}".format(retry, exc), file=sys.stderr)
    return None


def parse_retry_after(value: Optional[str]) -> Optional[int]:
    if not value:
        return None
    value = value.strip()
    if value.isdigit():
        return max(0, int(value))
    return None


def human_delay(base: float, count: int) -> None:
    if base <= 0:
        return
    time.sleep(base * random.uniform(0.6, 1.4))
    if random.random() < 0.15:
        pause = random.uniform(2.0, 6.0)
        print("  … 模拟阅读停顿 {:.1f}s".format(pause))
        time.sleep(pause)
    if count > 0 and count % random.randint(10, 15) == 0:
        rest = random.uniform(10.0, 25.0)
        print("  ☕ 批次休息 {:.0f}s（已处理 {} 篇）…".format(rest, count))
        time.sleep(rest)


def load_progress() -> Dict:
    if os.path.exists(PROGRESS_FILE):
        try:
            with open(PROGRESS_FILE, "r", encoding="utf-8") as file:
                data = json.load(file)
                print("📂 已加载进度：{} 条已完成".format(len(data.get("done", {}))))
                return data
        except Exception as exc:
            print("⚠ 进度文件读取失败，将从空进度开始：{}".format(exc))
    return {"done": {}, "failed": [], "catalogs": {}}


def save_progress(progress: Dict) -> None:
    with open(PROGRESS_FILE, "w", encoding="utf-8") as file:
        json.dump(progress, file, ensure_ascii=False, indent=2)


def clean_text(text: str) -> str:
    text = html.unescape(text or "")
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text)
    text = text.strip()
    return re.sub(r"^#+\s*", "", text).strip()


def normalize_path(href: str) -> Optional[str]:
    href = html.unescape((href or "").strip())
    if not href or href.startswith("#") or href.startswith("mailto:") or href.startswith("javascript:"):
        return None
    absolute = urljoin(BASE_URL + "/", href)
    parsed = urlsplit(absolute)
    if parsed.netloc and parsed.netloc != "pdai.tech":
        return None
    path = unquote(parsed.path)
    if not path.startswith("/md/") or not path.endswith(".html"):
        return None
    return path


def belongs_to_category(path: str, spec: CategorySpec) -> bool:
    return any(path.startswith(prefix) for prefix in spec.prefixes)


def article_url(path: str) -> str:
    return BASE_URL + path


def find_direct_sidebar_heading(li_tag: Tag) -> str:
    for child in li_tag.children:
        if isinstance(child, Tag):
            classes = child.get("class") or []
            if "sidebar-heading" in classes:
                return clean_text(child.get_text(" ", strip=True))
    return ""


def add_catalog_item(items: List[Dict], seen: Set[str], spec: CategorySpec,
                     path: str, title: str, group: str) -> None:
    if path in seen or not belongs_to_category(path, spec):
        return
    seen.add(path)
    items.append({
        "title": clean_text(title) or os.path.basename(path),
        "path": path,
        "url": article_url(path),
        "group": clean_text(group),
    })


def parse_sidebar_catalog(html_text: str, spec: CategorySpec) -> List[Dict]:
    soup = BeautifulSoup(html_text, "html.parser")
    sidebar = soup.select_one("ul.sidebar-items")
    items: List[Dict] = []
    seen: Set[str] = set()

    if sidebar:
        for li_tag in sidebar.find_all("li", recursive=False):
            group = find_direct_sidebar_heading(li_tag)
            for anchor in li_tag.select("a.sidebar-item[href]"):
                path = normalize_path(anchor.get("href", ""))
                if not path:
                    continue
                title = anchor.get("aria-label") or anchor.get_text(" ", strip=True)
                add_catalog_item(items, seen, spec, path, title, group)

    if items:
        return items

    # 兜底：如果未来 VuePress 结构变化导致 sidebar-items 缺失，至少从整页同前缀链接恢复目录。
    for anchor in soup.select("a[href]"):
        path = normalize_path(anchor.get("href", ""))
        if not path:
            continue
        title = anchor.get("aria-label") or anchor.get_text(" ", strip=True)
        add_catalog_item(items, seen, spec, path, title, "")
    return items


def discover_catalog(session: requests.Session, spec: CategorySpec,
                     timeout: int, retry: int, stop_on_429: bool,
                     cooldown_429: int) -> List[Dict]:
    seed_url = article_url(spec.seed_path)
    print("\n🔎 解析分类目录：{}  {}".format(spec.name, seed_url))
    html_text = fetch_url(
        session,
        seed_url,
        timeout=timeout,
        retry=retry,
        stop_on_429=stop_on_429,
        cooldown_429=cooldown_429,
    )
    if html_text is None:
        raise RuntimeError("分类入口页抓取失败：{}".format(seed_url))
    items = parse_sidebar_catalog(html_text, spec)
    if not items:
        raise RuntimeError("未能从侧栏解析到文章：{}".format(seed_url))
    print("  ✓ 发现 {} 篇文章".format(len(items)))
    for idx, item in enumerate(items[:8], start=1):
        print("    {:>2}. [{}] {}".format(idx, item.get("group") or "-", item["title"]))
    if len(items) > 8:
        print("    ... 其余 {} 篇略".format(len(items) - 8))
    return items


def safe_image_filename(abs_url: str) -> str:
    parsed = urlsplit(abs_url)
    base = os.path.basename(unquote(parsed.path)) or "image"
    base = re.sub(r"[^A-Za-z0-9._-]+", "_", base)
    stem, ext = os.path.splitext(base)
    if ext.lower() not in (".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"):
        ext = ".png"
    stem = (stem or "image")[:70].strip("._-") or "image"
    digest = hashlib.sha1(abs_url.encode("utf-8")).hexdigest()[:10]
    return "{}-{}{}".format(stem, digest, ext)


def download_image(session: requests.Session, src: str, page_url: str,
                   timeout: int, enabled: bool) -> str:
    src = (src or "").strip()
    if not src or src.startswith("data:"):
        return src

    abs_url = urljoin(page_url, html.unescape(src))
    if not enabled:
        return abs_url
    if abs_url in _img_cache:
        return _img_cache[abs_url]

    try:
        os.makedirs(IMG_DOWNLOAD_DIR, exist_ok=True)
        filename = safe_image_filename(abs_url)
        local_path = os.path.join(IMG_DOWNLOAD_DIR, filename)
        if not os.path.exists(local_path):
            resp = session.get(abs_url, headers=random_headers(page_url, image=True), timeout=timeout)
            resp.raise_for_status()
            with open(local_path, "wb") as file:
                file.write(resp.content)
        local_url = "{}/{}".format(IMG_URL_PREFIX, filename)
        _img_cache[abs_url] = local_url
        return local_url
    except Exception as exc:
        print("  ⚠ 图片下载失败 {}：{}".format(src[:80], exc), file=sys.stderr)
        _img_cache[abs_url] = abs_url
        return abs_url


def remove_noise(content_el: Tag) -> None:
    for selector in (
        "script",
        "style",
        "nav",
        "header",
        "footer",
        "button",
        "svg",
        ".header-anchor",
        ".table-of-contents",
        ".toc",
        ".edit-link",
        ".last-updated",
        "[class*='sidebar']",
        "[class*='nav']",
        "[class*='footer']",
        "[class*='page-meta']",
    ):
        for tag in content_el.select(selector):
            tag.decompose()


def extract_content(html_text: str, page_url: str, session: requests.Session,
                    image_timeout: int, download_images: bool) -> Tuple[str, str]:
    soup = BeautifulSoup(html_text, "html.parser")
    content_el: Optional[Tag] = None
    for selector in CONTENT_SELECTORS:
        content_el = soup.select_one(selector)
        if content_el:
            break
    if content_el is None:
        content_el = soup.body or soup

    remove_noise(content_el)

    title = ""
    h1 = content_el.find("h1")
    if h1:
        title = clean_text(h1.get_text(" ", strip=True))
    if not title:
        og = soup.find("meta", {"property": "og:title"})
        if og:
            title = clean_text(og.get("content", ""))
    if not title and soup.title:
        title = clean_text(soup.title.get_text(" ", strip=True).split("|")[0])

    for img_tag in content_el.find_all("img"):
        src = img_tag.get("src", "")
        img_tag["src"] = download_image(session, src, page_url, image_timeout, download_images)

    md = el_to_md(content_el, page_url)
    md = "\n".join(line for line in md.splitlines() if line.strip() != "[]")
    md = re.sub(r"\n{3,}", "\n\n", md).strip()
    if len(md) < 200:
        md = "## {}\n\n> 内容获取失败，请访问原文：{}\n".format(title, page_url)
    return title, md


def el_to_md(el: Tag, page_url: str) -> str:
    lines: List[str] = []
    walk_block(el, lines, page_url)
    return "\n".join(lines)


def append_line(lines: List[str], value: str) -> None:
    if lines:
        lines[-1] += value
    else:
        lines.append(value)


def inline_md(node, page_url: str) -> str:
    if isinstance(node, NavigableString):
        return str(node)
    if not isinstance(node, Tag):
        return ""

    name = (node.name or "").lower()
    if name == "code":
        return "`{}`".format(node.get_text("", strip=True))
    if name in ("strong", "b"):
        return "**{}**".format(clean_text(children_inline_md(node, page_url)))
    if name in ("em", "i"):
        return "*{}*".format(clean_text(children_inline_md(node, page_url)))
    if name == "br":
        return "\n"
    if name == "img":
        src = node.get("src", "").strip()
        alt = clean_text(node.get("alt", "")) or "图片"
        return "![{}]({})".format(alt, src) if src else ""
    if name == "a":
        text = clean_text(children_inline_md(node, page_url) or node.get_text(" ", strip=True))
        href = node.get("href", "").strip()
        if not text:
            return ""
        if href and not href.startswith("#"):
            return "[{}]({})".format(text, urljoin(page_url, html.unescape(href)))
        return text

    return children_inline_md(node, page_url)


def children_inline_md(node: Tag, page_url: str) -> str:
    raw = "".join(inline_md(child, page_url) for child in node.children)
    raw = raw.replace("\xa0", " ")
    raw = re.sub(r"[ \t\r\f\v]+", " ", raw)
    raw = re.sub(r" *\n *", "\n", raw)
    return raw.strip()


def walk_block(node, lines: List[str], page_url: str) -> None:
    if isinstance(node, NavigableString):
        text = clean_text(str(node))
        if text:
            append_line(lines, text)
        return
    if not isinstance(node, Tag):
        return

    name = (node.name or "").lower()
    if name in ("h1", "h2", "h3", "h4", "h5", "h6"):
        text = clean_text(node.get_text(" ", strip=True))
        if text:
            lines.extend(["", "{} {}".format("#" * int(name[1]), text), ""])
        return

    if name == "pre":
        code = node.find("code")
        raw = (code or node).get_text()
        lang = ""
        if code:
            for klass in code.get("class") or []:
                if klass.startswith("language-"):
                    lang = klass[9:]
                    break
        lines.extend(["", "```{}".format(lang)] + raw.rstrip("\n").splitlines() + ["```", ""])
        return

    if name == "table":
        rows = node.find_all("tr")
        if rows:
            lines.append("")
            for row_index, row in enumerate(rows):
                cells = [clean_text(cell.get_text(" ", strip=True)) for cell in row.find_all(["th", "td"])]
                if not cells:
                    continue
                lines.append("| " + " | ".join(cells) + " |")
                if row_index == 0:
                    lines.append("| " + " | ".join(["---"] * len(cells)) + " |")
            lines.append("")
        return

    if name in ("ul", "ol"):
        lines.append("")
        for index, li_tag in enumerate(node.find_all("li", recursive=False), start=1):
            prefix = "{}. ".format(index) if name == "ol" else "- "
            text = clean_text(children_inline_md(li_tag, page_url))
            if text:
                lines.append(prefix + text)
        lines.append("")
        return

    if name == "blockquote":
        inner_lines: List[str] = []
        for child in node.children:
            walk_block(child, inner_lines, page_url)
        text = "\n".join(inner_lines).strip()
        if text:
            lines.append("")
            for line in text.splitlines():
                lines.append("> " + line)
            lines.append("")
        return

    if name == "p":
        text = children_inline_md(node, page_url)
        if text:
            lines.extend(["", text, ""])
        return

    if name == "hr":
        lines.extend(["", "---", ""])
        return

    if name == "img":
        src = node.get("src", "").strip()
        alt = clean_text(node.get("alt", "")) or "图片"
        if src:
            lines.extend(["", "![{}]({})".format(alt, src), ""])
        return

    if name in ("code", "strong", "b", "em", "i", "a"):
        text = inline_md(node, page_url)
        if text:
            append_line(lines, text)
        return

    for child in node.children:
        walk_block(child, lines, page_url)


def esc_sql(value: str) -> str:
    value = value.replace("\\", "\\\\").replace("'", "\\'")
    value = value.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n")
    return value.replace("\t", "\\t")


def slug_from_path(path: str) -> str:
    path = re.sub(r"^/md/", "", path)
    path = re.sub(r"\.html$", "", path)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", path).strip("-").lower()
    return ("pdai-" + slug)[:240]


def summary_from_content(content: str, max_len: int = 250) -> str:
    parts: List[str] = []
    in_code = False
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("```"):
            in_code = not in_code
            continue
        if in_code:
            continue
        if not stripped or stripped.startswith(("#", "|", ">", "-", "!", "[")):
            continue
        text = re.sub(r"[`*_#\[\]\(\)]", "", stripped)
        if len(text) > 10:
            parts.append(text)
        if len(parts) >= 4:
            break
    summary = " ".join(parts).strip() or "暂无摘要"
    return (summary[:max_len] + "...") if len(summary) > max_len else summary


def cover_for(category_name: str) -> str:
    return random.choice(COVER_URLS.get(category_name, DEFAULT_COVERS))


def category_sort_order(spec: CategorySpec) -> int:
    for index, item in enumerate(CATEGORY_SPECS, start=1):
        if item.name == spec.name:
            return index * 10
    return 999


def spec_position(spec: CategorySpec) -> int:
    for index, item in enumerate(CATEGORY_SPECS, start=1):
        if item.name == spec.name:
            return index
    return 99


def pdai_column_id(spec: CategorySpec) -> int:
    return PDAI_COLUMN_ID_BASE + spec_position(spec)


def build_topic_id_map(spec: CategorySpec, categories: Sequence[str]) -> Dict[str, int]:
    base = PDAI_TOPIC_ID_BASE + spec_position(spec) * PDAI_TOPIC_ID_STRIDE
    return {category: base + index for index, category in enumerate(categories, start=1)}


def normalize_tag_name(value: str) -> str:
    value = clean_text(value)
    value = value.replace("♥", "").replace("【", "").replace("】", "")
    value = re.sub(r"\s+", " ", value).strip(" -_/|")
    return value[:50]


def article_tags(article: Dict, spec: CategorySpec) -> List[str]:
    tags: List[str] = []

    def add(value: str) -> None:
        value = normalize_tag_name(value)
        if value and value not in tags:
            tags.append(value)

    add(spec.name)
    add(article.get("group", ""))

    haystack = " ".join([
        str(article.get("title", "")),
        str(article.get("group", "")),
        str(article.get("slug", "")),
        str(article.get("url", "")),
    ]).lower()
    haystack = haystack.replace("-", " ").replace("_", " ")
    for tag, patterns in TAG_RULES:
        if any(pattern.lower() in haystack for pattern in patterns):
            add(tag)

    return tags[:6]


def article_category(article: Dict, spec: CategorySpec) -> str:
    category = normalize_tag_name(article.get("group", ""))
    return category or spec.name


def build_article_tag_map(spec: CategorySpec, articles: Sequence[Dict]) -> Dict[int, List[str]]:
    result: Dict[int, List[str]] = {}
    for article in articles:
        result[int(article["id"])] = article_tags(article, spec)
    return result


def count_tags(article_tag_map: Dict[int, List[str]]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for tags in article_tag_map.values():
        for tag in tags:
            counts[tag] = counts.get(tag, 0) + 1
    return counts


def collect_article_categories(spec: CategorySpec, articles: Sequence[Dict]) -> List[str]:
    categories: List[str] = []
    for article in articles:
        category = article_category(article, spec)
        if category and category not in categories:
            categories.append(category)
    return categories


def sql_string_list(values: Sequence[str]) -> str:
    return ", ".join("'{}'".format(esc_sql(value)) for value in values)


def append_category_sql(lines: List[str], spec: CategorySpec, categories: Sequence[str], now: str) -> None:
    for offset, category in enumerate(categories):
        lines.append(
            "INSERT INTO `blog_category` "
            "(`name`,`description`,`sort_order`,`enabled`,`status`,"
            "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "('{name}','{description}',{sort_order},1,'NORMAL','{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE "
            "`description`=VALUES(`description`),`sort_order`=VALUES(`sort_order`),"
            "`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
                name=esc_sql(category),
                description=esc_sql("pdai.tech {} / {} 分类文章".format(spec.name, category)),
                sort_order=category_sort_order(spec) + offset,
                now=now,
            )
        )
    lines.append("")


def append_tag_sql(lines: List[str], tag_counts: Dict[str, int], now: str) -> None:
    if not tag_counts:
        return
    for tag in sorted(tag_counts):
        lines.append(
            "INSERT INTO `blog_tag` "
            "(`name`,`description`,`use_count`,`enabled`,`status`,"
            "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "('{name}','{description}',{use_count},1,'NORMAL','{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE "
            "`description`=VALUES(`description`),`enabled`=1,`status`='NORMAL',"
            "`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
                name=esc_sql(tag),
                description=esc_sql("pdai.tech 自动导入标签：{}".format(tag)),
                use_count=tag_counts[tag],
                now=now,
            )
        )
    lines.append("")


def append_article_tag_sql(lines: List[str], article_tag_map: Dict[int, List[str]], now: str) -> None:
    article_ids = sorted(article_tag_map)
    if not article_ids:
        return

    for start in range(0, len(article_ids), 80):
        chunk = article_ids[start:start + 80]
        lines.append(
            "UPDATE `blog_article_tag` SET `deleted_at`='{now}',`updated_at`='{now}' "
            "WHERE `article_id` IN ({ids});".format(
                now=now,
                ids=", ".join(str(item) for item in chunk),
            )
        )
    lines.append("")

    for article_id in article_ids:
        for tag in article_tag_map[article_id]:
            lines.append(
                "INSERT INTO `blog_article_tag` "
                "(`article_id`,`tag_name`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
                "({article_id},'{tag}','{now}','{now}',NULL,0) "
                "ON DUPLICATE KEY UPDATE `deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                    article_id=article_id,
                    tag=esc_sql(tag),
                    now=now,
                )
            )
    lines.append("")


def append_tag_count_refresh_sql(lines: List[str], tag_names: Sequence[str]) -> None:
    if not tag_names:
        return
    lines.append(
        "UPDATE `blog_tag` t SET `use_count` = ("
        "SELECT COUNT(*) FROM `blog_article_tag` atg "
        "WHERE atg.`tag_name` = t.`name` AND atg.`deleted_at` IS NULL"
        ") WHERE t.`name` IN ({});".format(sql_string_list(sorted(tag_names)))
    )
    lines.append("")


def append_column_sql(lines: List[str], spec: CategorySpec, articles: Sequence[Dict], now: str) -> int:
    column_id = pdai_column_id(spec)
    title = "{} 知识体系".format(spec.name)
    summary = "pdai.tech {} 体系化文章专栏，按原站侧栏顺序整理，共 {} 篇。".format(spec.name, len(articles))
    sort_order = category_sort_order(spec)
    lines.append(
        "INSERT INTO `blog_column` "
        "(`id`,`author_id`,`title`,`summary`,`cover_url`,`status`,`sort_order`,"
        "`subscriber_count`,`article_count`,`recommended`,`recommend_weight`,"
        "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
        "({column_id},1,'{title}','{summary}','{cover_url}','PUBLISHED',{sort_order},"
        "0,{article_count},1,{recommend_weight},'{now}','{now}',NULL,0) "
        "ON DUPLICATE KEY UPDATE "
        "`author_id`=VALUES(`author_id`),`title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`cover_url`=VALUES(`cover_url`),`status`=VALUES(`status`),`sort_order`=VALUES(`sort_order`),"
        "`recommended`=VALUES(`recommended`),`recommend_weight`=VALUES(`recommend_weight`),"
        "`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
            column_id=column_id,
            title=esc_sql(title),
            summary=esc_sql(summary),
            cover_url=esc_sql(cover_for(spec.name)),
            sort_order=sort_order,
            article_count=len(articles),
            recommend_weight=1000 - sort_order,
            now=now,
        )
    )
    lines.append("")
    return column_id


def append_topic_sql(lines: List[str], spec: CategorySpec, categories: Sequence[str],
                     articles: Sequence[Dict], now: str) -> Dict[str, int]:
    topic_ids = build_topic_id_map(spec, categories)
    topic_counts: Dict[str, int] = {}
    for article in articles:
        category = article_category(article, spec)
        topic_counts[category] = topic_counts.get(category, 0) + 1

    for offset, category in enumerate(categories):
        topic_id = topic_ids[category]
        summary = "pdai.tech {} / {} 专题，按原站侧栏顺序整理，共 {} 篇。".format(
            spec.name,
            category,
            topic_counts.get(category, 0),
        )
        lines.append(
            "INSERT INTO `blog_topic` "
            "(`id`,`title`,`summary`,`cover_url`,`status`,`sort_order`,`article_count`,"
            "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "({topic_id},'{title}','{summary}','{cover_url}','PUBLISHED',{sort_order},{article_count},"
            "'{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE "
            "`title`=VALUES(`title`),`summary`=VALUES(`summary`),`cover_url`=VALUES(`cover_url`),"
            "`status`=VALUES(`status`),`sort_order`=VALUES(`sort_order`),"
            "`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
                topic_id=topic_id,
                title=esc_sql(category),
                summary=esc_sql(summary),
                cover_url=esc_sql(cover_for(spec.name)),
                sort_order=category_sort_order(spec) * 100 + offset,
                article_count=topic_counts.get(category, 0),
                now=now,
            )
        )
    lines.append("")
    return topic_ids


def append_column_article_sql(lines: List[str], column_id: int,
                              articles: Sequence[Dict], now: str) -> None:
    if not articles:
        return
    lines.append(
        "UPDATE `blog_column_article` SET `deleted_at`='{now}',`updated_at`='{now}' "
        "WHERE `column_id`={column_id};".format(now=now, column_id=column_id)
    )
    lines.append("")

    for index, article in enumerate(articles, start=1):
        lines.append(
            "INSERT INTO `blog_column_article` "
            "(`column_id`,`article_id`,`sort_order`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "({column_id},{article_id},{sort_order},'{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE `sort_order`=VALUES(`sort_order`),"
            "`deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                column_id=column_id,
                article_id=int(article["id"]),
                sort_order=index * 10,
                now=now,
            )
        )
    lines.append("")


def append_topic_article_sql(lines: List[str], spec: CategorySpec, topic_ids: Dict[str, int],
                             articles: Sequence[Dict], now: str) -> None:
    if not articles or not topic_ids:
        return
    lines.append(
        "UPDATE `blog_topic_article` SET `deleted_at`='{now}',`updated_at`='{now}' "
        "WHERE `topic_id` IN ({topic_ids});".format(
            now=now,
            topic_ids=", ".join(str(topic_id) for topic_id in sorted(topic_ids.values())),
        )
    )
    lines.append("")

    topic_orders: Dict[int, int] = {}
    for article in articles:
        topic_id = topic_ids[article_category(article, spec)]
        topic_orders[topic_id] = topic_orders.get(topic_id, 0) + 1
        lines.append(
            "INSERT INTO `blog_topic_article` "
            "(`topic_id`,`article_id`,`sort_order`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "({topic_id},{article_id},{sort_order},'{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE `sort_order`=VALUES(`sort_order`),"
            "`deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                topic_id=topic_id,
                article_id=int(article["id"]),
                sort_order=topic_orders[topic_id] * 10,
                now=now,
            )
        )
    lines.append("")


def append_column_count_refresh_sql(lines: List[str], column_ids: Sequence[int]) -> None:
    if not column_ids:
        return
    lines.append(
        "UPDATE `blog_column` c SET `article_count` = ("
        "SELECT COUNT(*) FROM `blog_column_article` ca "
        "INNER JOIN `blog_article` a ON a.`id` = ca.`article_id` "
        "WHERE ca.`column_id` = c.`id` AND ca.`deleted_at` IS NULL "
        "AND a.`deleted_at` IS NULL AND a.`status` = 'PUBLISHED'"
        ") WHERE c.`id` IN ({});".format(", ".join(str(item) for item in sorted(column_ids)))
    )
    lines.append("")


def append_topic_count_refresh_sql(lines: List[str], topic_ids: Sequence[int]) -> None:
    if not topic_ids:
        return
    lines.append(
        "UPDATE `blog_topic` t SET `article_count` = ("
        "SELECT COUNT(*) FROM `blog_topic_article` ta "
        "INNER JOIN `blog_article` a ON a.`id` = ta.`article_id` "
        "WHERE ta.`topic_id` = t.`id` AND ta.`deleted_at` IS NULL "
        "AND a.`deleted_at` IS NULL AND a.`status` = 'PUBLISHED'"
        ") WHERE t.`id` IN ({});".format(", ".join(str(item) for item in sorted(topic_ids)))
    )
    lines.append("")


def generate_category_sql(spec: CategorySpec, articles: Sequence[Dict],
                          output_path: str, dry_run: bool) -> None:
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    published_base = datetime(2025, 6, 1)
    article_tag_map = build_article_tag_map(spec, articles)
    article_categories = collect_article_categories(spec, articles)
    tag_counts = count_tags(article_tag_map)
    col_names = (
        "`id`,`author_id`,`title`,`summary`,`content`,`cover_url`,"
        "`category`,`offline_reason`,`featured`,`featured_at`,`slug`,"
        "`seo_title`,`seo_description`,`warn_flag`,`status`,"
        "`view_count`,`like_count`,`favorite_count`,`comment_count`,"
        "`published_at`,`created_at`,`updated_at`,`deleted_at`,`version`"
    )

    insert_prefix = "INSERT INTO `blog_article` ({}) VALUES".format(col_names)
    lines = [
        "-- ============================================================",
        "-- pdai 侧栏全量分类：{}  共 {} 篇  生成时间：{}".format(spec.name, len(articles), now),
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "START TRANSACTION;",
        "",
    ]

    append_category_sql(lines, spec, article_categories, now)
    append_tag_sql(lines, tag_counts, now)
    column_id = append_column_sql(lines, spec, articles, now)
    topic_ids = append_topic_sql(lines, spec, article_categories, articles, now)

    article_upsert_suffix = (
        " ON DUPLICATE KEY UPDATE "
        "`author_id`=VALUES(`author_id`),`title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`content`=VALUES(`content`),`cover_url`=VALUES(`cover_url`),`category`=VALUES(`category`),"
        "`offline_reason`=VALUES(`offline_reason`),`featured`=VALUES(`featured`),"
        "`featured_at`=VALUES(`featured_at`),`seo_title`=VALUES(`seo_title`),"
        "`seo_description`=VALUES(`seo_description`),`warn_flag`=VALUES(`warn_flag`),"
        "`status`=VALUES(`status`),`view_count`=VALUES(`view_count`),`like_count`=VALUES(`like_count`),"
        "`favorite_count`=VALUES(`favorite_count`),`comment_count`=VALUES(`comment_count`),"
        "`published_at`=VALUES(`published_at`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL"
    )

    for index, art in enumerate(articles):
        title = esc_sql(art["title"])
        content = art["content"]
        if art.get("source_url") and art["source_url"] not in content:
            content = "{}\n\n---\n\n原文链接：{}".format(content.rstrip(), art["source_url"])

        summary = esc_sql(summary_from_content(content))
        content_sql = esc_sql(content)
        seo_desc = esc_sql(summary_from_content(content, 120))
        published_at = datetime.fromtimestamp(
            published_base.timestamp() + index * random.randint(1, 3) * 86400
        ).strftime("%Y-%m-%d %H:%M:%S")
        view_count = random.randint(200, 8000)
        like_count = random.randint(10, max(10, int(view_count * 0.15)))
        favorite_count = random.randint(5, max(5, int(view_count * 0.08)))
        comment_count = random.randint(0, max(0, int(view_count * 0.02)))
        author_id = 1 if index % 2 == 0 else 2

        row = (
            "({aid},{author_id},'{title}','{summary}','{content}','{cover_url}',"
            "'{category}',NULL,0,NULL,'{slug}','{seo_title}','{seo_desc}',"
            "0,'PUBLISHED',{view},{like},{favorite},{comment},"
            "'{published_at}','{created_at}','{updated_at}',NULL,0)".format(
                aid=art["id"],
                author_id=author_id,
                title=title,
                summary=summary,
                content=content_sql,
                cover_url=esc_sql(cover_for(spec.name)),
                category=esc_sql(article_category(art, spec)),
                slug=esc_sql(art["slug"]),
                seo_title=esc_sql(art["title"] + " - 技术博客"),
                seo_desc=seo_desc,
                view=view_count,
                like=like_count,
                favorite=favorite_count,
                comment=comment_count,
                published_at=published_at,
                created_at=published_at,
                updated_at=now,
            )
        )
        lines.append(insert_prefix)
        lines.append(row + article_upsert_suffix + ";")
        lines.append("")

    append_article_tag_sql(lines, article_tag_map, now)
    append_column_article_sql(lines, column_id, articles, now)
    append_topic_article_sql(lines, spec, topic_ids, articles, now)
    append_tag_count_refresh_sql(lines, list(tag_counts.keys()))
    append_column_count_refresh_sql(lines, [column_id])
    append_topic_count_refresh_sql(lines, list(topic_ids.values()))

    lines.append("COMMIT;")
    sql = "\n".join(lines) + "\n"

    if dry_run:
        print(sql[:2000] + "\n... (dry-run，仅显示前 2000 字符)")
        return

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as file:
        file.write(sql)
    print("  💾 SQL 已写入：{} ({} 条 INSERT)".format(os.path.basename(output_path), len(articles)))


def generate_sql_from_progress(specs: Sequence[CategorySpec], progress: Dict, dry_run: bool) -> None:
    done_items = []
    for item in progress.get("done", {}).values():
        if isinstance(item, dict):
            done_items.append(item)

    for spec in specs:
        articles = [
            dict(item)
            for item in done_items
            if item.get("category") == spec.name and item.get("content")
        ]
        articles.sort(key=lambda item: int(item.get("id", 0)))
        if not articles:
            print("  ⚠ 进度中没有分类 {} 的已抓取文章".format(spec.name))
            continue
        output_file = spec.file.replace("articles_pdai_full_", "articles_pdai_cached_")
        output_path = os.path.join(SQL_OUTPUT_DIR, output_file)
        print("\n  ✍ 从进度生成 SQL：{}  {} 篇".format(spec.name, len(articles)))
        generate_category_sql(spec, articles, output_path, dry_run=dry_run)


def parse_categories_filter(raw: str) -> List[CategorySpec]:
    if not raw:
        return list(CATEGORY_SPECS)
    names = {name.strip() for name in raw.split(",") if name.strip()}
    matched = [spec for spec in CATEGORY_SPECS if spec.name in names]
    missing = names - {spec.name for spec in matched}
    if missing:
        print("⚠ 未找到分类：{}；可选：{}".format(
            ", ".join(sorted(missing)),
            ", ".join(spec.name for spec in CATEGORY_SPECS),
        ))
        sys.exit(1)
    return matched


def remove_failed(progress: Dict, url: str) -> None:
    failed = progress.get("failed", [])
    progress["failed"] = [item for item in failed if item.get("url") != url]


def crawl_category(session: requests.Session, spec: CategorySpec, catalog: List[Dict],
                   progress: Dict, args, fetched_count: int) -> Tuple[List[Dict], int]:
    done_urls: Dict[str, Dict] = progress.setdefault("done", {})
    results: List[Dict] = []

    for index, entry in enumerate(catalog):
        art_id = spec.id_start + index
        url = entry["url"]
        path = entry["path"]

        if url in done_urls and not args.force:
            cached = dict(done_urls[url])
            cached.update({
                "id": art_id,
                "category": spec.name,
                "group": entry.get("group", ""),
                "slug": slug_from_path(path),
                "source_url": url,
            })
            print("[cached] ID={} [{}] {}".format(art_id, entry.get("group") or "-", cached["title"]))
            results.append(cached)
            continue

        if args.limit > 0 and fetched_count >= args.limit:
            print("\n[LIMIT] 已达到 --limit={}，停止新增抓取".format(args.limit))
            break

        fetched_count += 1
        print("[{}] ID={} [{}] {}".format(
            fetched_count,
            art_id,
            entry.get("group") or "-",
            url,
        ))

        try:
            html_text = fetch_url(
                session,
                url,
                timeout=args.timeout,
                retry=args.retry,
                stop_on_429=not args.ignore_429,
                cooldown_429=args.cooldown_429,
            )
        except RateLimitedError as exc:
            failed_entry = {
                "url": url,
                "id": art_id,
                "title": entry["title"],
                "category": spec.name,
                "group": entry.get("group", ""),
                "reason": "429 Too Many Requests",
            }
            progress.setdefault("failed", [])
            if url not in [item.get("url") for item in progress["failed"]]:
                progress["failed"].append(failed_entry)
            save_progress(progress)
            wait_hint = "，Retry-After={}s".format(exc.retry_after) if exc.retry_after is not None else ""
            print("  ✋ 命中站点限流{}，已保存进度并停止本轮抓取".format(wait_hint))
            break
        if html_text is None:
            failed_entry = {
                "url": url,
                "id": art_id,
                "title": entry["title"],
                "category": spec.name,
                "group": entry.get("group", ""),
            }
            progress.setdefault("failed", [])
            if url not in [item.get("url") for item in progress["failed"]]:
                progress["failed"].append(failed_entry)
            save_progress(progress)
            human_delay(args.delay, fetched_count)
            continue

        title, content = extract_content(
            html_text,
            url,
            session,
            image_timeout=args.image_timeout,
            download_images=not args.no_images,
        )
        final_title = title or entry["title"]
        article = {
            "id": art_id,
            "title": final_title,
            "category": spec.name,
            "group": entry.get("group", ""),
            "content": content,
            "url": url,
            "source_url": url,
            "slug": slug_from_path(path),
        }
        print("  ✓ {}（{} 字符）".format(final_title, len(content)))
        results.append(article)
        done_urls[url] = article
        remove_failed(progress, url)
        progress["done"] = done_urls
        save_progress(progress)
        human_delay(args.delay, fetched_count)

    return results, fetched_count


def write_catalog_file(catalogs: Dict[str, List[Dict]]) -> None:
    with open(CATALOG_FILE, "w", encoding="utf-8") as file:
        json.dump(catalogs, file, ensure_ascii=False, indent=2)
    print("\n📘 目录清单已写入：{}".format(os.path.normpath(CATALOG_FILE)))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="从 pdai.tech 侧栏目录全量爬取文章并生成 SQL")
    parser.add_argument("--category", default="", help="只处理指定分类，多个用逗号分隔，例如：Java,Spring")
    parser.add_argument("--catalog-only", action="store_true", help="只解析侧栏目录，不抓正文、不生成 SQL")
    parser.add_argument("--progress-sql", action="store_true", help="只用本地进度文件生成 SQL，不访问 pdai")
    parser.add_argument("--dry-run", action="store_true", help="抓取并打印 SQL 片段，但不写 SQL 文件")
    parser.add_argument("--reset", action="store_true", help="清空新脚本的进度文件后重爬")
    parser.add_argument("--force", action="store_true", help="忽略进度缓存，重新抓取已完成文章")
    parser.add_argument("--no-images", action="store_true", help="不下载正文图片，保留 pdai 原始图片 URL")
    parser.add_argument("--delay", type=float, default=2.0, help="每篇文章基础间隔秒数，默认 2.0")
    parser.add_argument("--cooldown-429", type=int, default=0, help="遇到 429 后先等待 N 秒再停止本轮抓取")
    parser.add_argument("--ignore-429", action="store_true", help="保持旧行为：遇到 429 仍按 retry 重试")
    parser.add_argument("--timeout", type=int, default=20, help="页面请求超时秒数，默认 20")
    parser.add_argument("--image-timeout", type=int, default=15, help="图片请求超时秒数，默认 15")
    parser.add_argument("--retry", type=int, default=2, help="失败重试次数，默认 2")
    parser.add_argument("--limit", type=int, default=0, help="最多新增抓取 N 篇，0 表示不限制")
    return parser


def main() -> None:
    args = build_parser().parse_args()
    selected_specs = parse_categories_filter(args.category)

    if args.reset and os.path.exists(PROGRESS_FILE):
        os.remove(PROGRESS_FILE)
        print("🗑 已清空进度文件：{}".format(os.path.normpath(PROGRESS_FILE)))

    progress = load_progress()

    if args.progress_sql:
        generate_sql_from_progress(selected_specs, progress, dry_run=args.dry_run)
        return

    session = requests.Session()

    catalogs: Dict[str, List[Dict]] = {}
    for spec in selected_specs:
        try:
            catalog = discover_catalog(
                session,
                spec,
                timeout=args.timeout,
                retry=args.retry,
                stop_on_429=not args.ignore_429,
                cooldown_429=args.cooldown_429,
            )
        except RateLimitedError:
            print("✋ 解析目录时命中站点限流，已停止。稍后可断点续跑。")
            return
        catalogs[spec.name] = catalog
        progress.setdefault("catalogs", {})[spec.name] = catalog
        save_progress(progress)
    write_catalog_file(catalogs)

    if args.catalog_only:
        total = sum(len(items) for items in catalogs.values())
        print("\n✅ 目录解析完成：{} 个分类，{} 篇文章".format(len(catalogs), total))
        return

    fetched_count = 0
    for spec in selected_specs:
        catalog = catalogs[spec.name]
        print("\n" + "=" * 60)
        print("📂 分类：{}  侧栏文章：{} 篇".format(spec.name, len(catalog)))
        print("=" * 60)
        articles, fetched_count = crawl_category(session, spec, catalog, progress, args, fetched_count)
        if not articles:
            print("  ⚠ 分类 {} 无可用数据，跳过 SQL 生成".format(spec.name))
            continue
        output_path = os.path.join(SQL_OUTPUT_DIR, spec.file)
        print("\n  ✍ 生成分类 SQL...")
        generate_category_sql(spec, articles, output_path, dry_run=args.dry_run)

    failed_count = len(progress.get("failed", []))
    print("\n" + "=" * 60)
    print("✅ 累计完成：{}  ✗ 失败：{}".format(len(progress.get("done", {})), failed_count))
    if failed_count:
        print("\n失败列表：")
        for item in progress.get("failed", []):
            print("  ID={} [{}] {}  {}".format(
                item.get("id"),
                item.get("category"),
                item.get("title"),
                item.get("url"),
            ))
    elif not args.dry_run:
        print("SQL 文件位于：{}".format(os.path.normpath(SQL_OUTPUT_DIR)))


if __name__ == "__main__":
    main()
