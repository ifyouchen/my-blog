#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
博客园 (cnblogs.com/pick) 文章爬虫。

从博客园精华区/分类区抓取文章列表和正文，生成与 my-blog 兼容的 SQL。

用法：
  python scripts/crawl_cnblogs.py --catalog-only
  python scripts/crawl_cnblogs.py --limit 50
  python scripts/crawl_cnblogs.py --category 后端开发 --limit 20
  python scripts/crawl_cnblogs.py --category 后端开发,算法 --limit 30
  python scripts/crawl_cnblogs.py --reset --workers 12 --timeout 30 --retry 3

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
import tempfile
import threading
import time
from concurrent.futures import CancelledError, ThreadPoolExecutor, as_completed
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


BASE_URL = "https://www.cnblogs.com"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.normpath(os.path.join(SCRIPT_DIR, ".."))

PROGRESS_FILE = os.path.join(SCRIPT_DIR, "crawl_cnblogs_progress.json")
CATALOG_FILE = os.path.join(SCRIPT_DIR, "cnblogs_catalog.json")
SQL_OUTPUT_DIR = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "db", "articles")
IMG_DOWNLOAD_DIR = os.path.join(PROJECT_ROOT, "backend", "uploads", "cnblogs")
IMG_URL_PREFIX = "/api/uploads/files/cnblogs"
CNBLOG_COLUMN_ID_BASE = 9400000
CNBLOG_TOPIC_ID_BASE = 9500000
CNBLOG_TOPIC_ID_STRIDE = 1000

LIST_API_URL = BASE_URL + "/AggSite/AggSitePostList"


@dataclass(frozen=True)
class CategorySpec:
    name: str
    file: str
    id_start: int
    category_id: int
    category_type: str


@dataclass(frozen=True)
class CrawlTask:
    index: int
    sequence: int
    entry: Dict
    article_id: int


CATEGORY_SPECS: Tuple[CategorySpec, ...] = (
    CategorySpec("精华区", "articles_cnblogs_picked.sql", 10000, -2, "Picked"),
    CategorySpec("后端开发", "articles_cnblogs_backend.sql", 20000, 2, "TopSiteCategory"),
    CategorySpec("企业信息化其他", "articles_cnblogs_enterprise.sql", 30000, 3, "TopSiteCategory"),
    CategorySpec("其他分类", "articles_cnblogs_other.sql", 40000, 4, "TopSiteCategory"),
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
    "https://www.cnblogs.com/",
    "https://www.cnblogs.com/pick/",
    "https://www.cnblogs.com/cate/2",
    "https://www.cnblogs.com/cate/3",
    "https://www.cnblogs.com/cate/4",
)

CONTENT_SELECTORS: Tuple[str, ...] = (
    "div.postBody",
    "div#post_detail",
    "div.post",
    "article",
    "main",
)

COVER_URLS: Dict[str, Tuple[str, ...]] = {
    "精华区": (
        "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
        "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800",
    ),
    "后端开发": (
        "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800",
        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800",
        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
    ),
    "企业信息化其他": (
        "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800",
        "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800",
        "https://images.unsplash.com/photo-1486312338219-ce68d2c6f44d?w=800",
    ),
    "其他分类": (
        "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800",
        "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800",
        "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800",
    ),
}

DEFAULT_COVERS: Tuple[str, ...] = (
    "https://images.unsplash.com/photo-1516116216624-53e697fedbea?w=800",
    "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800",
)

TAG_RULES: Tuple[Tuple[str, Tuple[str, ...]], ...] = (
    ("AI", ("ai", "人工智能", "大模型", "chatgpt", "gpt", "llm", "deepseek", "vibe coding", "agent")),
    ("Java", ("java", "spring", "jvm", "mybatis")),
    ("Python", ("python",)),
    ("前端", ("前端", "vue", "react", "angular", "html", "css", "javascript", "typescript")),
    ("后端", ("后端", "微服务", "架构", "分布式", "高并发", "高可用")),
    ("数据库", ("数据库", "mysql", "redis", "mongodb", "sql", "elasticsearch")),
    ("云原生", ("云原生", "docker", "kubernetes", "k8s", "容器")),
    ("Linux", ("linux", "ubuntu", "centos", "shell")),
    ("开源", ("开源", "github", "git")),
    ("安全", ("安全", "漏洞", "加密", "认证")),
    ("程序员", ("程序员", "编程", "代码", "开发", "重构")),
    ("性能优化", ("性能", "优化", "缓存", "调优")),
)

_img_cache: Dict[str, str] = {}
_img_cache_lock = threading.Lock()
_thread_local = threading.local()
_progress_file_lock = threading.RLock()
_last_progress_save_at = 0.0


class RateLimitedError(Exception):
    def __init__(self, url: str, retry_after: Optional[int] = None):
        super().__init__("429 Too Many Requests: {}".format(url))
        self.url = url
        self.retry_after = retry_after


def thread_session() -> requests.Session:
    session = getattr(_thread_local, "session", None)
    if session is None:
        session = requests.Session()
        _thread_local.session = session
    return session


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
                retry_after = _parse_retry_after(resp.headers.get("Retry-After"))
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


def _parse_retry_after(value: Optional[str]) -> Optional[int]:
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


def load_catalog_cache() -> Dict[str, List[Dict]]:
    if not os.path.exists(CATALOG_FILE):
        return {}
    try:
        with open(CATALOG_FILE, "r", encoding="utf-8") as file:
            data = json.load(file)
    except Exception as exc:
        print("⚠ 目录缓存读取失败，跳过缓存合并：{}".format(exc))
        return {}
    if not isinstance(data, dict):
        print("⚠ 目录缓存格式不是分类映射，跳过缓存合并")
        return {}

    catalogs: Dict[str, List[Dict]] = {}
    total = 0
    for category, items in data.items():
        if not isinstance(items, list):
            continue
        safe_items = [item for item in items if isinstance(item, dict)]
        if not safe_items:
            continue
        catalogs[str(category)] = safe_items
        total += len(safe_items)
    if total > 0:
        print("📂 已加载目录缓存：{} 个分类，{} 篇".format(len(catalogs), total))
    return catalogs


def save_progress(progress: Dict) -> None:
    global _last_progress_save_at
    with _progress_file_lock:
        try:
            progress_dir = os.path.dirname(PROGRESS_FILE)
            os.makedirs(progress_dir, exist_ok=True)
        except OSError as exc:
            print("⚠ 进度目录创建失败：{}".format(exc), file=sys.stderr)
            return

        try:
            payload = json.dumps(progress, ensure_ascii=True, indent=2).encode("utf-8")
        except TypeError as exc:
            print("⚠ 进度内容无法序列化：{}".format(exc), file=sys.stderr)
            return

        last_exc: Optional[OSError] = None
        for attempt in range(1, 6):
            tmp_path = None
            try:
                with tempfile.NamedTemporaryFile(
                    mode="wb",
                    dir=progress_dir,
                    prefix="crawl_cnblogs_progress.",
                    suffix=".tmp",
                    delete=False,
                ) as file:
                    tmp_path = file.name
                    file.write(payload)
                    file.flush()
                    os.fsync(file.fileno())
                os.replace(tmp_path, PROGRESS_FILE)
                _last_progress_save_at = time.monotonic()
                return
            except OSError as exc:
                last_exc = exc
                if tmp_path and os.path.exists(tmp_path):
                    try:
                        os.remove(tmp_path)
                    except OSError:
                        pass
                time.sleep(0.08 * attempt)

        print("⚠ 进度文件写入失败（已重试）：{}".format(last_exc), file=sys.stderr)


def save_progress_throttled(progress: Dict, min_interval: float = 2.0) -> None:
    if time.monotonic() - _last_progress_save_at >= min_interval:
        save_progress(progress)


def save_json_file(path: str, payload: object) -> None:
    directory = os.path.dirname(path)
    os.makedirs(directory, exist_ok=True)
    tmp_path = None
    try:
        data = json.dumps(payload, ensure_ascii=True, indent=2).encode("utf-8")
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=directory,
            prefix=os.path.basename(path) + ".",
            suffix=".tmp",
            delete=False,
        ) as file:
            tmp_path = file.name
            file.write(data)
            file.flush()
            os.fsync(file.fileno())
        os.replace(tmp_path, path)
    except OSError:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except OSError:
                pass
        raise


def save_json_file_best_effort(path: str, payload: object, label: str) -> bool:
    try:
        save_json_file(path, payload)
        return True
    except OSError as exc:
        print("⚠ {}写入失败：{}".format(label, exc), file=sys.stderr)
    except TypeError as exc:
        print("⚠ {}内容无法序列化：{}".format(label, exc), file=sys.stderr)
    return False

def clean_text(text: str) -> str:
    text = html.unescape(text or "")
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text)
    text = text.strip()
    return re.sub(r"^#+\s*", "", text).strip()


def esc_sql(value: str) -> str:
    value = value.replace("\\", "\\\\").replace("'", "\\'")
    value = value.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n")
    return value.replace("\t", "\\t")


def slug_from_url(url: str) -> str:
    """从文章 URL 生成唯一 slug."""
    parsed = urlsplit(url)
    path = unquote(parsed.path)
    path = re.sub(r"^/", "", path)
    path = re.sub(r"\.html$", "", path)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", path).strip("-").lower()
    return ("cnblogs-" + slug)[:240]


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


def spec_position(spec: CategorySpec) -> int:
    for index, item in enumerate(CATEGORY_SPECS, start=1):
        if item.name == spec.name:
            return index
    return 99


def cnblogs_column_id(spec: CategorySpec) -> int:
    return CNBLOG_COLUMN_ID_BASE + spec_position(spec)


def build_topic_id_map(spec: CategorySpec, categories: Sequence[str]) -> Dict[str, int]:
    base = CNBLOG_TOPIC_ID_BASE + spec_position(spec) * CNBLOG_TOPIC_ID_STRIDE
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

    haystack = " ".join([
        str(article.get("title", "")),
        str(article.get("category", "")),
        str(article.get("slug", "")),
        str(article.get("url", "")),
    ]).lower()
    haystack = haystack.replace("-", " ").replace("_", " ")
    for tag, patterns in TAG_RULES:
        if any(pattern.lower() in haystack for pattern in patterns):
            add(tag)

    return tags[:6]


def article_category_name(article: Dict, spec: CategorySpec) -> str:
    return spec.name


# ── 目录/文章列表发现 ──────────────────────────────────────


def discover_article_list(session: requests.Session, spec: CategorySpec,
                          args) -> Tuple[List[Dict], int]:
    """通过 POST API 分页获取指定分类的文章列表."""
    articles: List[Dict] = []
    seen_urls: Set[str] = set()

    # 先请求第一页获知总数
    page_size = 50
    page_index = 1
    total_count = 0

    print("\n🔎 获取分类文章列表：{} (categoryId={})".format(spec.name, spec.category_id))

    while True:
        payload = {
            "CategoryType": spec.category_type,
            "ParentCategoryId": 0,
            "CategoryId": spec.category_id,
            "PageIndex": page_index,
            "PageSize": page_size,
            "TotalPostCount": 0,
            "ItemListActionName": "AggSitePostList",
        }
        try:
            html_text = _fetch_list_page(session, payload, args)
        except RateLimitedError as exc:
            print("  ✋ 获取文章列表时命中限流，停止本轮抓取：{}".format(exc))
            raise
        except Exception as exc:
            print("  ✗ 第 {} 页获取失败：{}".format(page_index, exc))
            break

        if not html_text or not html_text.strip():
            break

        item_count = _parse_list_items(html_text, articles, seen_urls, spec, page_index)

        if page_index == 1:
            total_count = _extract_total_count(html_text)
            if total_count > 0:
                total_pages = (total_count + page_size - 1) // page_size
                print("  📊 共 {} 篇，约 {} 页".format(total_count, total_pages))
            else:
                print("  📊 每页 {} 篇".format(page_size))

        if item_count == 0:
            break

        # 检查是否达到用户限制
        if args.limit > 0 and len(articles) >= args.limit:
            articles = articles[:args.limit]
            break

        page_index += 1
        if args.delay > 0:
            time.sleep(random.uniform(0.3, 0.8))

    if articles:
        print("  ✓ 发现 {} 篇文章".format(len(articles)))
    else:
        print("  ⚠ 未找到文章")
    return articles, total_count


def _fetch_list_page(session: requests.Session, payload: Dict,
                     args) -> Optional[str]:
    """请求文章列表 API 一页."""
    for attempt in range(1, args.retry + 2):
        try:
            resp = session.post(
                LIST_API_URL,
                json=payload,
                headers={
                    **random_headers(),
                    "Content-Type": "application/json; charset=utf-8",
                },
                timeout=args.timeout,
            )
            if resp.status_code == 429:
                retry_after = _parse_retry_after(resp.headers.get("Retry-After"))
                wait_seconds = retry_after if retry_after is not None else args.cooldown_429
                if wait_seconds and wait_seconds > 0:
                    print("  ⚠ 命中 429，等待 {}s".format(wait_seconds))
                    time.sleep(wait_seconds)
                if not args.ignore_429:
                    raise RateLimitedError(LIST_API_URL, retry_after)
            resp.raise_for_status()
            resp.encoding = "utf-8"
            return resp.text
        except RateLimitedError:
            raise
        except Exception as exc:
            if attempt <= args.retry:
                backoff = random.uniform(3.0, 6.0) * attempt
                print("  ⚠ 列表页第 {} 次重试（{:.1f}s 后）: {}".format(attempt, backoff, exc))
                time.sleep(backoff)
            else:
                print("  ✗ 列表页请求失败（已重试 {} 次）: {}".format(args.retry, exc), file=sys.stderr)
    return None


def _extract_total_count(html_text: str) -> int:
    """从第一页响应中提取 TotalPostCount."""
    match = re.search(r'"totalCount":\s*(\d+)', html_text, re.IGNORECASE)
    if match:
        return int(match.group(1))
    # 尝试从页面脚注提取
    for line in html_text.splitlines():
        line = line.strip()
        if "TotalPostCount" in line:
            nums = re.findall(r'\d+', line)
            if nums:
                return int(nums[-1])
    return 0


def _parse_list_items(html_text: str, articles: List[Dict], seen_urls: Set[str],
                      spec: CategorySpec, page_index: int) -> int:
    """从 API 返回的 HTML 中解析文章列表项."""
    soup = BeautifulSoup(html_text, "html.parser")
    items = soup.select("article.post-item")
    count = 0

    for item in items:
        title_el = item.select_one("a.post-item-title")
        if not title_el:
            continue
        href = title_el.get("href", "").strip()
        if not href or href in seen_urls:
            continue
        seen_urls.add(href)

        title = clean_text(title_el.get_text(" ", strip=True))
        summary_el = item.select_one("p.post-item-summary")
        summary = clean_text(summary_el.get_text(" ", strip=True)) if summary_el else ""
        author_el = item.select_one(".post-item-author")
        author = clean_text(author_el.get_text(" ", strip=True)) if author_el else ""

        # 发布时间
        time_el = item.select_one("footer .post-meta-item span")
        pub_time = clean_text(time_el.get_text(" ", strip=True)) if time_el else ""

        # 评论/推荐/阅读数
        comment_el = item.select_one(".post-meta-item.btn svg[href*='comment']")
        digg_el = item.select_one("a[id^='digg_control_']")

        # 提取 postId
        post_id = item.get("data-post-id", "")

        articles.append({
            "title": title or "无标题",
            "url": href,
            "summary": summary,
            "author": author,
            "author_url": author_el.get("href", "") if author_el else "",
            "published_at": pub_time,
            "post_id": post_id,
        })
        count += 1

    return count


# ── 文章正文提取 ──────────────────────────────────────────


def extract_content(html_text: str, page_url: str, session: requests.Session,
                    image_timeout: int, download_images: bool) -> Tuple[str, str]:
    """从文章详情页提取标题和正文（转 Markdown）。"""
    soup = BeautifulSoup(html_text, "html.parser")
    content_el: Optional[Tag] = None
    for selector in CONTENT_SELECTORS:
        content_el = soup.select_one(selector)
        if content_el:
            break
    if content_el is None:
        content_el = soup.body or soup

    content_el = _remove_noise(content_el) or content_el

    # 提取标题
    title = ""
    title_link = soup.select_one("a#cb_post_title_url")
    if title_link:
        title = clean_text(title_link.get_text(" ", strip=True))
    if not title:
        h1 = content_el.find("h1")
        if h1:
            title = clean_text(h1.get_text(" ", strip=True))
    if not title:
        og = soup.find("meta", {"property": "og:title"})
        if og:
            title = clean_text(og.get("content", ""))
    if not title and soup.title:
        title = clean_text(soup.title.get_text(" ", strip=True).split("-")[0])

    # 处理图片
    for img_tag in content_el.find_all("img"):
        src = img_tag.get("src", "")
        img_tag["src"] = _download_image(session, src, page_url, image_timeout, download_images)

    # 分类/标签信息
    cat_el = soup.select_one("div#BlogPostCategory")
    category_info = clean_text(cat_el.get_text(" ", strip=True)) if cat_el else ""

    # 转 Markdown
    md = _el_to_md(content_el, page_url)
    md = "\n".join(line for line in md.splitlines() if line.strip() != "[]")
    md = re.sub(r"\n{3,}", "\n\n", md).strip()

    if category_info:
        md = "> 分类：{}\n\n{}".format(category_info, md)

    if len(md) < 100:
        md = "## {}\n\n> 内容获取失败，请访问原文：{}\n".format(title, page_url)

    return title, md


def _remove_noise(content_el: Tag) -> Optional[Tag]:
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
        "div#post_info",
        "div#post_desc",
        "div#blog_post_info_block",
        "div#ad_cnblog",
        "div#ad_under_kk",
        "div#ad_under_post_avatar",
        "div#ad_under_post_title",
        "div#ad_under_content",
        "div.ad_text",
        "div.ads",
        "a[href*='.cnblogs.com']:not(.post-item-title)",
    ):
        for tag in content_el.select(selector):
            tag.decompose()
    # 提取正文核心容器，去掉外围包装
    inner = content_el.select_one("div#cnblogs_post_body")
    if inner:
        return inner
    return content_el


def _safe_image_filename(abs_url: str) -> str:
    parsed = urlsplit(abs_url)
    base = os.path.basename(unquote(parsed.path)) or "image"
    base = re.sub(r"[^A-Za-z0-9._-]+", "_", base)
    stem, ext = os.path.splitext(base)
    if ext.lower() not in (".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"):
        ext = ".png"
    stem = (stem or "image")[:70].strip("._-") or "image"
    digest = hashlib.sha1(abs_url.encode("utf-8")).hexdigest()[:10]
    return "{}-{}{}".format(stem, digest, ext)


def _download_image(session: requests.Session, src: str, page_url: str,
                    timeout: int, enabled: bool) -> str:
    src = (src or "").strip()
    if not src or src.startswith("data:"):
        return src

    abs_url = urljoin(page_url, html.unescape(src))
    if not enabled:
        return abs_url
    with _img_cache_lock:
        cached_url = _img_cache.get(abs_url)
    if cached_url:
        return cached_url

    try:
        os.makedirs(IMG_DOWNLOAD_DIR, exist_ok=True)
        filename = _safe_image_filename(abs_url)
        local_path = os.path.join(IMG_DOWNLOAD_DIR, filename)
        if not os.path.exists(local_path):
            resp = session.get(abs_url, headers=random_headers(page_url, image=True), timeout=timeout)
            resp.raise_for_status()
            with open(local_path, "wb") as file:
                file.write(resp.content)
        local_url = "{}/{}".format(IMG_URL_PREFIX, filename)
        with _img_cache_lock:
            _img_cache[abs_url] = local_url
        return local_url
    except Exception as exc:
        print("  ⚠ 图片下载失败 {}：{}".format(src[:80], exc), file=sys.stderr)
        with _img_cache_lock:
            _img_cache[abs_url] = abs_url
        return abs_url


# ── HTML → Markdown 转换 ─────────────────────────────────


def _el_to_md(el: Tag, page_url: str) -> str:
    lines: List[str] = []
    _walk_block(el, lines, page_url)
    return "\n".join(lines)


def _inline_md(node, page_url: str) -> str:
    if isinstance(node, NavigableString):
        return str(node)
    if not isinstance(node, Tag):
        return ""

    name = (node.name or "").lower()
    if name == "code":
        return "`{}`".format(node.get_text("", strip=True))
    if name in ("strong", "b"):
        return "**{}**".format(clean_text(_children_inline_md(node, page_url)))
    if name in ("em", "i"):
        return "*{}*".format(clean_text(_children_inline_md(node, page_url)))
    if name == "br":
        return "\n"
    if name == "img":
        src = node.get("src", "").strip()
        alt = clean_text(node.get("alt", "")) or "图片"
        return "![{}]({})".format(alt, src) if src else ""
    if name == "a":
        text = clean_text(_children_inline_md(node, page_url) or node.get_text(" ", strip=True))
        href = node.get("href", "").strip()
        if not text:
            return ""
        if href and not href.startswith("#"):
            return "[{}]({})".format(text, urljoin(page_url, html.unescape(href)))
        return text

    return _children_inline_md(node, page_url)


def _children_inline_md(node: Tag, page_url: str) -> str:
    raw = "".join(_inline_md(child, page_url) for child in node.children)
    raw = raw.replace("\xa0", " ")
    raw = re.sub(r"[ \t\r\f\v]+", " ", raw)
    raw = re.sub(r" *\n *", "\n", raw)
    return raw.strip()


def _walk_block(node, lines: List[str], page_url: str) -> None:
    if isinstance(node, NavigableString):
        text = clean_text(str(node))
        if text:
            _append_line(lines, text)
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
            text = clean_text(_children_inline_md(li_tag, page_url))
            if text:
                lines.append(prefix + text)
        lines.append("")
        return

    if name == "blockquote":
        inner_lines: List[str] = []
        for child in node.children:
            _walk_block(child, inner_lines, page_url)
        text = "\n".join(inner_lines).strip()
        if text:
            lines.append("")
            for line in text.splitlines():
                lines.append("> " + line)
            lines.append("")
        return

    if name == "p":
        text = _children_inline_md(node, page_url)
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
        text = _inline_md(node, page_url)
        if text:
            _append_line(lines, text)
        return

    for child in node.children:
        _walk_block(child, lines, page_url)


def _append_line(lines: List[str], value: str) -> None:
    if lines:
        lines[-1] += value
    else:
        lines.append(value)


# ── SQL 生成 ──────────────────────────────────────────────


def generate_sql(spec: CategorySpec, articles: Sequence[Dict],
                 output_path: str, dry_run: bool) -> None:
    """生成包含文章、分类、标签、专栏、专题的完整 SQL."""
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    published_base = datetime(2025, 6, 1)
    article_tag_map = _build_article_tag_map(spec, articles)
    article_categories = _collect_article_categories(spec, articles)
    tag_counts = _count_tags(article_tag_map)

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
        "-- 博客园文章：{}  共 {} 篇  生成时间：{}".format(spec.name, len(articles), now),
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "START TRANSACTION;",
        "",
    ]

    _append_category_sql(lines, spec, now)
    _append_tag_sql(lines, tag_counts, now)
    column_id = _append_column_sql(lines, spec, articles, now)
    topic_ids = _append_topic_sql(lines, spec, article_categories, articles, now)

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
        title = esc_sql(art["title"][:120])
        content = art.get("content", "")
        if art.get("url") and art["url"] not in content:
            content = "{}\n\n---\n\n原文链接：{}".format(content.rstrip(), art["url"])

        summary = esc_sql(summary_from_content(content))
        content_sql = esc_sql(content)
        seo_desc = esc_sql(summary_from_content(content, 120))

        # 使用文章原始发布时间，如无则自动生成
        raw_time = art.get("published_at", "")
        if raw_time and re.match(r'\d{4}-\d{2}-\d{2}', raw_time):
            published_at = raw_time
        else:
            published_at = datetime.fromtimestamp(
                published_base.timestamp() + index * random.randint(1, 3) * 86400
            ).strftime("%Y-%m-%d %H:%M:%S")

        view_count = random.randint(200, 8000)
        like_count = random.randint(10, max(10, int(view_count * 0.15)))
        favorite_count = random.randint(5, max(5, int(view_count * 0.08)))
        comment_count_text = art.get("comment_count", "0")
        try:
            comment_count = int(comment_count_text)
        except (ValueError, TypeError):
            comment_count = random.randint(0, int(view_count * 0.02))

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
                category=esc_sql(spec.name),
                slug=esc_sql(art["slug"]),
                seo_title=esc_sql(art["title"] + " - 博客园"),
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

    _append_article_tag_sql(lines, article_tag_map, now)
    _append_column_article_sql(lines, column_id, articles, now)
    _append_topic_article_sql(lines, spec, topic_ids, articles, now)
    _append_tag_count_refresh_sql(lines, list(tag_counts.keys()))
    _append_column_count_refresh_sql(lines, [column_id])
    _append_topic_count_refresh_sql(lines, list(topic_ids.values()))

    lines.append("COMMIT;")
    sql = "\n".join(lines) + "\n"

    if dry_run:
        print(sql[:2000] + "\n... (dry-run，仅显示前 2000 字符)")
        return

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as file:
        file.write(sql)
    print("  💾 SQL 已写入：{} ({} 条 INSERT)".format(os.path.basename(output_path), len(articles)))


def _append_category_sql(lines: List[str], spec: CategorySpec, now: str) -> None:
    sort_order = spec_position(spec) * 10
    lines.append(
        "INSERT INTO `blog_category` "
        "(`name`,`description`,`sort_order`,`enabled`,`status`,"
        "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
        "('{name}','{description}',{sort_order},1,'NORMAL','{now}','{now}',NULL,0) "
        "ON DUPLICATE KEY UPDATE "
        "`description`=VALUES(`description`),`sort_order`=VALUES(`sort_order`),"
        "`enabled`=1,`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
            name=esc_sql(spec.name),
            description=esc_sql("博客园 {} 分类文章".format(spec.name)),
            sort_order=sort_order,
            now=now,
        )
    )
    lines.append("")


def _append_tag_sql(lines: List[str], tag_counts: Dict[str, int], now: str) -> None:
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
                description=esc_sql("博客园自动导入标签：{}".format(tag)),
                use_count=tag_counts[tag],
                now=now,
            )
        )
    lines.append("")


def _append_article_tag_sql(lines: List[str], article_tag_map: Dict[int, List[str]], now: str) -> None:
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


def _append_tag_count_refresh_sql(lines: List[str], tag_names: Sequence[str]) -> None:
    if not tag_names:
        return
    lines.append(
        "UPDATE `blog_tag` t SET `use_count` = ("
        "SELECT COUNT(*) FROM `blog_article_tag` atg "
        "WHERE atg.`tag_name` = t.`name` AND atg.`deleted_at` IS NULL"
        ") WHERE t.`name` IN ({});".format(_sql_string_list(sorted(tag_names)))
    )
    lines.append("")


def _append_column_sql(lines: List[str], spec: CategorySpec, articles: Sequence[Dict], now: str) -> int:
    column_id = cnblogs_column_id(spec)
    title = "博客园 - {}".format(spec.name)
    summary = "博客园 {} 精选文章，共 {} 篇。".format(spec.name, len(articles))
    sort_order = spec_position(spec) * 10
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


def _append_column_article_sql(lines: List[str], column_id: int,
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
                sort_order=(index + 1) * 10,
                now=now,
            )
        )
    lines.append("")


def _append_topic_sql(lines: List[str], spec: CategorySpec, categories: Sequence[str],
                      articles: Sequence[Dict], now: str) -> Dict[str, int]:
    if not categories:
        return {}
    topic_ids = build_topic_id_map(spec, categories)
    topic_counts: Dict[str, int] = {}
    for article in articles:
        category = article_category_name(article, spec)
        topic_counts[category] = topic_counts.get(category, 0) + 1

    for offset, category in enumerate(categories):
        topic_id = topic_ids[category]
        summary = "博客园 {} 专题文章，共 {} 篇。".format(category, topic_counts.get(category, 0))
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
                sort_order=spec_position(spec) * 100 + offset,
                article_count=topic_counts.get(category, 0),
                now=now,
            )
        )
    lines.append("")
    return topic_ids


def _append_topic_article_sql(lines: List[str], spec: CategorySpec, topic_ids: Dict[str, int],
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
        topic_id = topic_ids[article_category_name(article, spec)]
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


def _append_column_count_refresh_sql(lines: List[str], column_ids: Sequence[int]) -> None:
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


def _append_topic_count_refresh_sql(lines: List[str], topic_ids: Sequence[int]) -> None:
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


def _sql_string_list(values: Sequence[str]) -> str:
    return ", ".join("'{}'".format(esc_sql(value)) for value in values)


def _build_article_tag_map(spec: CategorySpec, articles: Sequence[Dict]) -> Dict[int, List[str]]:
    result: Dict[int, List[str]] = {}
    for article in articles:
        result[int(article["id"])] = article_tags(article, spec)
    return result


def _count_tags(article_tag_map: Dict[int, List[str]]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for tags in article_tag_map.values():
        for tag in tags:
            counts[tag] = counts.get(tag, 0) + 1
    return counts


def _collect_article_categories(spec: CategorySpec, articles: Sequence[Dict]) -> List[str]:
    categories: List[str] = []
    for article in articles:
        category = article_category_name(article, spec)
        if category and category not in categories:
            categories.append(category)
    return categories


# ── 主流程 ────────────────────────────────────────────────


def _parse_categories_filter(raw: str) -> List[CategorySpec]:
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


def _remove_failed(progress: Dict, url: str) -> None:
    failed = progress.get("failed", [])
    progress["failed"] = [item for item in failed if item.get("url") != url]


def _used_article_ids(done_urls: Dict[str, Dict], spec: CategorySpec) -> Set[int]:
    used: Set[int] = set()
    for item in done_urls.values():
        if not isinstance(item, dict) or item.get("category") != spec.name:
            continue
        try:
            used.add(int(item.get("id")))
        except (TypeError, ValueError):
            continue
    return used


def _next_article_id(spec: CategorySpec, used_ids: Set[int]) -> int:
    article_id = spec.id_start
    while article_id in used_ids:
        article_id += 1
    used_ids.add(article_id)
    return article_id


def _article_id_for_entry(spec: CategorySpec, index: int, url: str,
                          done_urls: Dict[str, Dict], used_ids: Set[int]) -> int:
    cached = done_urls.get(url)
    if isinstance(cached, dict):
        try:
            article_id = int(cached.get("id"))
            used_ids.add(article_id)
            return article_id
        except (TypeError, ValueError):
            pass

    article_id = spec.id_start + index
    if article_id in used_ids:
        return _next_article_id(spec, used_ids)
    used_ids.add(article_id)
    return article_id


def _build_failed_entry(task: CrawlTask, spec: CategorySpec, reason: Optional[str] = None) -> Dict:
    failed_entry = {
        "url": task.entry.get("url", ""),
        "id": task.article_id,
        "title": task.entry.get("title", ""),
        "category": spec.name,
    }
    if reason:
        failed_entry["reason"] = reason
    return failed_entry


def _record_failed(progress: Dict, failed_entry: Dict) -> None:
    url = failed_entry.get("url")
    progress.setdefault("failed", [])
    if url not in [item.get("url") for item in progress["failed"]]:
        progress["failed"].append(failed_entry)


def _record_article(progress: Dict, article: Dict) -> None:
    done_urls: Dict[str, Dict] = progress.setdefault("done", {})
    done_urls[article["url"]] = article
    _remove_failed(progress, article["url"])
    progress["done"] = done_urls


def _crawl_article_task(task: CrawlTask, spec: CategorySpec, args,
                        stop_event: threading.Event) -> Tuple[Optional[Dict], Optional[Dict], Optional[RateLimitedError]]:
    if stop_event.is_set():
        return None, None, None

    url = task.entry["url"]
    print("  [{}] ID={} {}".format(task.sequence, task.article_id, task.entry.get("title", "")))
    session = thread_session()
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
        return None, _build_failed_entry(task, spec, "429 Too Many Requests"), exc

    if html_text is None:
        return None, _build_failed_entry(task, spec), None

    title, content = extract_content(
        html_text,
        url,
        session,
        image_timeout=args.image_timeout,
        download_images=not args.no_images,
    )
    final_title = title or task.entry.get("title", "无标题")
    article = {
        "id": task.article_id,
        "title": final_title,
        "category": spec.name,
        "content": content,
        "url": url,
        "slug": slug_from_url(url),
        "author": task.entry.get("author", ""),
        "published_at": task.entry.get("published_at", ""),
    }
    print("    ✓ ID={} {}（{} 字符）".format(task.article_id, final_title, len(content)))
    return article, None, None


def crawl_category(session: requests.Session, spec: CategorySpec,
                   catalog: List[Dict], progress: Dict, args,
                   fetched_count: int) -> Tuple[List[Dict], int]:
    """抓取指定分类的所有文章正文."""
    done_urls: Dict[str, Dict] = progress.setdefault("done", {})
    used_ids = _used_article_ids(done_urls, spec)
    results: List[Dict] = []
    tasks: List[CrawlTask] = []

    for index, entry in enumerate(catalog):
        url = entry["url"]
        art_id = _article_id_for_entry(spec, index, url, done_urls, used_ids)

        if url in done_urls and not args.force:
            cached = dict(done_urls[url])
            cached.update({
                "id": art_id,
                "category": spec.name,
                "slug": slug_from_url(url),
                "author": entry.get("author", ""),
            })
            print("  [cached] ID={} {}".format(art_id, cached["title"]))
            results.append(cached)
            continue

        if args.limit > 0 and index >= args.limit:
            print("\n  [LIMIT] 分类 {} 已达到 --limit={}，停止新增抓取".format(spec.name, args.limit))
            break

        tasks.append(CrawlTask(
            index=index,
            sequence=fetched_count + len(tasks) + 1,
            entry=dict(entry),
            article_id=art_id,
        ))

    if not tasks:
        return results, fetched_count

    workers = max(1, int(getattr(args, "workers", 1)))
    stop_event = threading.Event()
    progress_lock = threading.Lock()
    rate_limited: Optional[RateLimitedError] = None
    print("\n  🚀 并发抓取：{} 篇，workers={}，delay={}s".format(len(tasks), workers, args.delay))

    if workers == 1:
        for task in tasks:
            article, failed_entry, exc = _crawl_article_task(task, spec, args, stop_event)
            if exc is not None:
                with progress_lock:
                    if failed_entry:
                        _record_failed(progress, failed_entry)
                        save_progress_throttled(progress, args.progress_save_interval)
                rate_limited = exc
                break
            if failed_entry:
                with progress_lock:
                    _record_failed(progress, failed_entry)
                    save_progress_throttled(progress, args.progress_save_interval)
                human_delay(args.delay, task.sequence)
                continue
            if article:
                results.append(article)
                with progress_lock:
                    _record_article(progress, article)
                    save_progress_throttled(progress, args.progress_save_interval)
                human_delay(args.delay, task.sequence)
    else:
        with ThreadPoolExecutor(max_workers=workers) as executor:
            future_map = {
                executor.submit(_crawl_article_task, task, spec, args, stop_event): task
                for task in tasks
            }
            for future in as_completed(future_map):
                task = future_map[future]
                try:
                    article, failed_entry, exc = future.result()
                except CancelledError:
                    continue
                except Exception as exc:
                    print("  ✗ ID={} 线程抓取异常：{}".format(task.article_id, exc), file=sys.stderr)
                    failed_entry = _build_failed_entry(task, spec, str(exc))
                    with progress_lock:
                        _record_failed(progress, failed_entry)
                        save_progress_throttled(progress, args.progress_save_interval)
                    continue

                if exc is not None:
                    stop_event.set()
                    rate_limited = exc
                    with progress_lock:
                        if failed_entry:
                            _record_failed(progress, failed_entry)
                        save_progress_throttled(progress, args.progress_save_interval)
                    for pending in future_map:
                        if not pending.done():
                            pending.cancel()
                    continue
                if failed_entry:
                    with progress_lock:
                        _record_failed(progress, failed_entry)
                        save_progress_throttled(progress, args.progress_save_interval)
                    continue
                if article:
                    results.append(article)
                    with progress_lock:
                        _record_article(progress, article)
                        save_progress_throttled(progress, args.progress_save_interval)

    fetched_count += len(tasks)
    if rate_limited is not None:
        save_progress(progress)
        wait_hint = "，Retry-After={}s".format(rate_limited.retry_after) if rate_limited.retry_after is not None else ""
        print("  ✋ 命中站点限流{}，已保存进度并停止本轮抓取".format(wait_hint))
        raise rate_limited
    results.sort(key=lambda item: int(item.get("id", 0)))
    save_progress(progress)
    return results, fetched_count


def _write_catalog_file(catalogs: Dict[str, List[Dict]]) -> None:
    if save_json_file_best_effort(CATALOG_FILE, catalogs, "目录清单"):
        print("\n📘 目录清单已写入：{}".format(os.path.normpath(CATALOG_FILE)))


def _generate_sql_from_progress(specs: Sequence[CategorySpec], progress: Dict, dry_run: bool,
                                catalog_cache: Optional[Dict[str, List[Dict]]] = None) -> None:
    articles_by_category = _collect_sql_ready_articles(progress, catalog_cache or {})
    for spec in specs:
        articles = articles_by_category.get(spec.name, [])
        if not articles:
            print("  ⚠ 本地缓存中没有分类 {} 的可生成 SQL 文章".format(spec.name))
            continue
        output_path = os.path.join(SQL_OUTPUT_DIR, spec.file)
        print("\n  ✍ 从本地缓存生成 SQL：{}  {} 篇".format(spec.name, len(articles)))
        generate_sql(spec, articles, output_path, dry_run=dry_run)


def _collect_sql_ready_articles(progress: Dict, catalog_cache: Dict[str, List[Dict]]) -> Dict[str, List[Dict]]:
    articles_by_category: Dict[str, Dict[str, Dict]] = {}

    def add(item: Dict, category_hint: Optional[str] = None) -> None:
        if not isinstance(item, dict) or not item.get("content"):
            return
        try:
            int(item.get("id"))
        except (TypeError, ValueError):
            return

        category = str(item.get("category") or category_hint or "").strip()
        if not category:
            return
        article = dict(item)
        article["category"] = category
        key = str(article.get("url") or article.get("slug") or article.get("id"))
        if not key:
            return
        bucket = articles_by_category.setdefault(category, {})
        current = bucket.get(key)
        if current is None or len(str(article.get("content", ""))) > len(str(current.get("content", ""))):
            bucket[key] = article

    for item in progress.get("done", {}).values():
        add(item)

    for category, items in catalog_cache.items():
        for item in items:
            add(item, category)

    result: Dict[str, List[Dict]] = {}
    for category, items_by_key in articles_by_category.items():
        items = list(items_by_key.values())
        items.sort(key=lambda item: int(item.get("id", 0)))
        result[category] = items
    return result


def _progress_articles_for_spec(progress: Dict, spec: CategorySpec,
                                catalog_cache: Optional[Dict[str, List[Dict]]] = None) -> List[Dict]:
    return _collect_sql_ready_articles(progress, catalog_cache or {}).get(spec.name, [])


def _cached_catalog_for_spec(progress: Dict, catalog_cache: Dict[str, List[Dict]],
                             spec: CategorySpec, limit: int) -> List[Dict]:
    catalogs = progress.get("catalogs", {})
    catalog = catalogs.get(spec.name) if isinstance(catalogs, dict) else None
    if not isinstance(catalog, list) or not catalog:
        catalog = catalog_cache.get(spec.name)
    if not isinstance(catalog, list):
        return []
    if limit > 0:
        return catalog[:limit]
    return catalog


def _target_count_before_catalog(progress: Dict, catalog_cache: Dict[str, List[Dict]],
                                 spec: CategorySpec, args) -> Optional[int]:
    if args.limit > 0:
        return args.limit
    cached_catalog = _cached_catalog_for_spec(progress, catalog_cache, spec, args.limit)
    if cached_catalog:
        return len(cached_catalog)
    return None


def _print_crawl_plan_from_progress(specs: Sequence[CategorySpec], progress: Dict,
                                    catalog_cache: Dict[str, List[Dict]], args) -> bool:
    print("\n📌 本次爬取计划（基于本地进度）")
    all_targets_reached = not args.force
    total_remaining = 0

    for spec in specs:
        done_count = len(_progress_articles_for_spec(progress, spec, catalog_cache))
        target_count = _target_count_before_catalog(progress, catalog_cache, spec, args)
        if target_count is None:
            all_targets_reached = False
            print("  - {}：已完成 {} 篇，目标篇数待目录解析后确认".format(spec.name, done_count))
            continue

        remaining = max(target_count - done_count, 0)
        total_remaining += remaining
        if remaining > 0:
            all_targets_reached = False
        print("  - {}：目标 {} 篇，已完成 {} 篇，待抓取 {} 篇".format(
            spec.name,
            target_count,
            done_count,
            remaining,
        ))

    if args.force:
        print("  - 已启用 --force：即使已有进度，也会重新抓取正文")
    else:
        print("  本次预计还需抓取：{} 篇".format(total_remaining))
    return all_targets_reached


def _print_crawl_plan_from_catalogs(specs: Sequence[CategorySpec], catalogs: Dict[str, List[Dict]],
                                    progress: Dict, catalog_cache: Dict[str, List[Dict]], args) -> bool:
    print("\n📌 本次爬取计划（基于最新目录）")
    all_targets_reached = not args.force
    total_remaining = 0

    for spec in specs:
        target_count = len(catalogs.get(spec.name, []))
        done_count = len(_progress_articles_for_spec(progress, spec, catalog_cache))
        remaining = target_count if args.force else max(target_count - done_count, 0)
        total_remaining += remaining
        if remaining > 0:
            all_targets_reached = False
        print("  - {}：目标 {} 篇，已完成 {} 篇，待抓取 {} 篇".format(
            spec.name,
            target_count,
            done_count,
            remaining,
        ))

    if args.force:
        print("  - 已启用 --force：本轮将按目录重新抓取 {} 篇".format(total_remaining))
    else:
        print("  本次还需抓取：{} 篇".format(total_remaining))
    return all_targets_reached


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="博客园文章爬虫")
    parser.add_argument("--category", default="",
                        help="只处理指定分类，多个用逗号分隔，例如：精华区,后端开发")
    parser.add_argument("--catalog-only", action="store_true",
                        help="只获取文章列表目录，不抓正文、不生成 SQL")
    parser.add_argument("--progress-sql", action="store_true",
                        help="只用本地进度文件生成 SQL，不访问博客园")
    parser.add_argument("--dry-run", action="store_true",
                        help="抓取并打印 SQL 片段，但不写 SQL 文件")
    parser.add_argument("--reset", action="store_true",
                        help="清空进度文件后重爬")
    parser.add_argument("--force", action="store_true",
                        help="忽略进度缓存，重新抓取已完成文章")
    parser.add_argument("--no-images", action="store_true",
                        help="不下载正文图片，保留原始图片 URL")
    parser.add_argument("--delay", type=float, default=0.0,
                        help="每篇文章基础间隔秒数，默认 0（不限速）")
    parser.add_argument("--workers", type=int, default=8,
                        help="正文并发抓取线程数，默认 8")
    parser.add_argument("--progress-save-interval", type=float, default=2.0,
                        help="并发抓取时进度文件最小落盘间隔秒数，默认 2.0")
    parser.add_argument("--cooldown-429", type=int, default=0,
                        help="遇到 429 后等待 N 秒再停止，默认 0")
    parser.add_argument("--ignore-429", action="store_true",
                        help="遇到 429 仍按 retry 重试")
    parser.add_argument("--timeout", type=int, default=20,
                        help="页面请求超时秒数，默认 20")
    parser.add_argument("--image-timeout", type=int, default=15,
                        help="图片请求超时秒数，默认 15")
    parser.add_argument("--retry", type=int, default=2,
                        help="失败重试次数，默认 2")
    parser.add_argument("--limit", type=int, default=0,
                        help="每个分类最多抓取 N 篇文章，0 表示不限制")
    return parser


def main() -> None:
    args = build_parser().parse_args()
    selected_specs = _parse_categories_filter(args.category)

    if args.reset and os.path.exists(PROGRESS_FILE):
        os.remove(PROGRESS_FILE)
        print("🗑 已清空进度文件：{}".format(os.path.normpath(PROGRESS_FILE)))

    progress = load_progress()
    catalog_cache = load_catalog_cache()

    if args.progress_sql:
        _generate_sql_from_progress(selected_specs, progress, dry_run=args.dry_run, catalog_cache=catalog_cache)
        return

    if not args.catalog_only:
        args.workers = max(1, args.workers)
        args.delay = max(0.0, args.delay)
        print("⚙ 抓取模式：workers={}，delay={}s".format(args.workers, args.delay))

    if not args.catalog_only and _print_crawl_plan_from_progress(selected_specs, progress, catalog_cache, args):
        print("\n✅ 本地进度已达到本次目标，开始整理 SQL 后退出。")
        _generate_sql_from_progress(selected_specs, progress, dry_run=args.dry_run, catalog_cache=catalog_cache)
        return

    session = requests.Session()

    # 1. 获取所有分类的文章列表
    catalogs: Dict[str, List[Dict]] = {}
    for spec in selected_specs:
        print("\n" + "=" * 60)
        print("📂 分类：{}".format(spec.name))
        print("=" * 60)

        try:
            articles, _ = discover_article_list(session, spec, args)
        except RateLimitedError:
            save_progress(progress)
            print("✋ 解析目录时命中站点限流，已停止。稍后可断点续跑或使用 --progress-sql。")
            return
        if args.limit > 0:
            articles = articles[:args.limit]
        catalogs[spec.name] = articles
        progress.setdefault("catalogs", {})[spec.name] = articles
        save_progress(progress)

    _write_catalog_file(catalogs)

    if args.catalog_only:
        total = sum(len(items) for items in catalogs.values())
        print("\n✅ 目录解析完成：{} 个分类，{} 篇文章".format(len(catalogs), total))
        return

    if _print_crawl_plan_from_catalogs(selected_specs, catalogs, progress, catalog_cache, args):
        print("\n✅ 已抓取文章数达到最新目录目标，开始整理 SQL 后退出。")
        _generate_sql_from_progress(selected_specs, progress, dry_run=args.dry_run, catalog_cache=catalog_cache)
        return

    # 2. 逐篇抓取正文
    fetched_count = 0
    for spec in selected_specs:
        catalog = catalogs[spec.name]
        if not catalog:
            print("  ⚠ 分类 {} 无文章数据".format(spec.name))
            continue
        print("\n" + "=" * 60)
        print("📄 抓取正文：{}  共 {} 篇".format(spec.name, len(catalog)))
        print("=" * 60)
        try:
            articles, fetched_count = crawl_category(session, spec, catalog, progress, args, fetched_count)
        except RateLimitedError:
            print("✋ 抓取正文时命中站点限流，已停止。已完成文章可用 --progress-sql 生成 SQL。")
            return
        sql_articles = _progress_articles_for_spec(progress, spec, catalog_cache)
        if not sql_articles:
            print("  ⚠ 分类 {} 无可用数据，跳过 SQL 生成".format(spec.name))
            continue
        output_path = os.path.join(SQL_OUTPUT_DIR, spec.file)
        print("\n  ✍ 整理本地缓存并生成 SQL...")
        generate_sql(spec, sql_articles, output_path, dry_run=args.dry_run)

    # 3. 汇总统计
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
