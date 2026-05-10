#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
通用菜鸟教程课程爬虫。

供 crawl_runoob_*.py 入口脚本复用：
  - 从 .runoob-col-md2 发现课程目录
  - 抓取正文并转 Markdown
  - 生成 my-blog 兼容 SQL：文章、标签、分类、专栏、专题、学习路径关系
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
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Sequence, Tuple
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

BASE_URL = "https://www.runoob.com"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.normpath(os.path.join(SCRIPT_DIR, ".."))
SQL_OUTPUT_DIR = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "db", "articles")
IMG_OUTPUT_ROOT = os.path.join(PROJECT_ROOT, "backend", "uploads", "runoob")
IMG_URL_ROOT = "/api/uploads/files/runoob"
AUTHOR_ID = 1

CONTENT_SELECTORS: Tuple[str, ...] = (
    "div.article-intro",
    "#content",
    "article",
    "main",
    ".main",
    ".middle-column",
)

UA_POOL: Tuple[str, ...] = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
)

_img_cache: Dict[str, str] = {}


@dataclass(frozen=True)
class CourseConfig:
    slug: str
    seed_path: str
    display_name: str
    category_name: str
    article_id_start: int
    column_id: int
    topic_id: int
    tags: Tuple[str, ...]
    cover_url: str
    column_title: str
    topic_title: str
    intro: str
    difficulty: str = "BEGINNER"
    sort_order: int = 40
    recommend_weight: int = 940
    section_title: str = "入门教程"

    @property
    def seed_url(self) -> str:
        return BASE_URL + self.seed_path

    @property
    def url_prefix(self) -> str:
        parts = self.seed_path.strip("/").split("/")
        return "/" + (parts[0] if parts else self.slug) + "/"

    @property
    def progress_file(self) -> str:
        return os.path.join(SCRIPT_DIR, "crawl_runoob_{}_progress.json".format(self.slug))

    @property
    def catalog_file(self) -> str:
        return os.path.join(SCRIPT_DIR, "runoob_{}_catalog.json".format(self.slug))

    @property
    def sql_output(self) -> str:
        return os.path.join(SQL_OUTPUT_DIR, "articles_runoob_{}.sql".format(self.slug))

    @property
    def image_dir(self) -> str:
        return os.path.join(IMG_OUTPUT_ROOT, self.slug)

    @property
    def image_url_prefix(self) -> str:
        return "{}/{}".format(IMG_URL_ROOT, self.slug)


@dataclass(frozen=True)
class CatalogEntry:
    title: str
    url: str
    order: int


def random_headers(referer: Optional[str] = None, image: bool = False) -> Dict[str, str]:
    return {
        "User-Agent": random.choice(UA_POOL),
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8" if image
        else "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Accept-Encoding": "gzip, deflate",
        "Connection": "keep-alive",
        "Referer": referer or BASE_URL + "/",
        "Cache-Control": "no-cache",
    }


def fetch_url(session: requests.Session, url: str, timeout: int, retry: int,
              referer: Optional[str] = None) -> Optional[str]:
    last_referer = referer or BASE_URL + "/"
    for attempt in range(1, retry + 2):
        try:
            resp = session.get(url, headers=random_headers(last_referer), timeout=timeout)
            resp.raise_for_status()
            resp.encoding = "utf-8"
            return resp.text
        except Exception as exc:
            if attempt <= retry:
                print("  ⚠ 第 {} 次重试：{}".format(attempt, exc))
                continue
            print("  ✗ 请求失败：{}  {}".format(url, exc), file=sys.stderr)
    return None


def load_json(path: str, default):
    if not os.path.exists(path):
        return default
    try:
        with open(path, "r", encoding="utf-8") as file:
            return json.load(file)
    except Exception as exc:
        print("⚠ 读取失败 {}：{}".format(os.path.basename(path), exc), file=sys.stderr)
        return default


def save_json(path: str, payload) -> None:
    directory = os.path.dirname(path)
    os.makedirs(directory, exist_ok=True)
    tmp_path = None
    try:
        data = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
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
    except Exception as exc:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except OSError:
                pass
        print("⚠ 写入失败 {}：{}".format(os.path.basename(path), exc), file=sys.stderr)


def load_progress(config: CourseConfig) -> Dict:
    progress = load_json(config.progress_file, {"done": {}, "failed": []})
    print("📂 已加载进度：{} 条已完成".format(len(progress.get("done", {}))))
    return progress


def clean_text(text: str) -> str:
    text = html.unescape(text or "")
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def normalize_url(config: CourseConfig, href: str) -> str:
    href = (href or "").strip()
    if not href or href.startswith("#") or href.startswith("javascript:"):
        return ""
    url = urljoin(BASE_URL + "/", href)
    parsed = urlsplit(url)
    if "runoob.com" not in parsed.netloc:
        return ""
    path = parsed.path
    if not path.startswith(config.url_prefix) or not path.endswith(".html"):
        return ""
    return BASE_URL + path


def choose_article_title(extracted_title: str, fallback_title: str, config: CourseConfig) -> str:
    extracted_title = clean_text(extracted_title)
    fallback_title = clean_text(fallback_title)
    generic_patterns = (
        "菜鸟教程 --",
        "学的不仅是技术",
        "RUNOOB",
        "runoob.com",
    )
    if not extracted_title:
        return fallback_title or config.display_name
    if any(pattern.lower() in extracted_title.lower() for pattern in generic_patterns):
        return fallback_title or extracted_title
    return extracted_title


def discover_catalog(session: requests.Session, config: CourseConfig, args) -> List[CatalogEntry]:
    cached = load_json(config.catalog_file, [])
    if cached and not args.refresh_catalog:
        entries = [
            CatalogEntry(item["title"], item["url"], int(item.get("order", index + 1)))
            for index, item in enumerate(cached)
            if isinstance(item, dict) and item.get("url")
        ]
        if len(entries) >= 5:
            print("📘 已加载目录缓存：{} 篇".format(len(entries)))
            return entries[:args.limit] if args.limit > 0 else entries
        print("⚠ 目录缓存只有 {} 篇，疑似测试截断缓存，重新发现完整目录".format(len(entries)))

    html_text = fetch_url(session, config.seed_url, timeout=args.timeout, retry=args.retry)
    if not html_text:
        if cached:
            print("⚠ 入口页不可用，回退目录缓存")
            return [
                CatalogEntry(item["title"], item["url"], int(item.get("order", index + 1)))
                for index, item in enumerate(cached)
                if isinstance(item, dict) and item.get("url")
            ]
        return [CatalogEntry(config.display_name, config.seed_url, 1)]

    soup = BeautifulSoup(html_text, "html.parser")
    seen = set()
    entries: List[CatalogEntry] = []

    def add(title: str, url: str) -> None:
        if not url or url in seen:
            return
        seen.add(url)
        entries.append(CatalogEntry(clean_text(title) or config.display_name, url, len(entries) + 1))

    containers = soup.select(".runoob-col-md2")
    if not containers:
        containers = soup.select("#leftcolumn, .left-column, .sidebar-box, .design")
    if not containers:
        containers = [soup]
    for container in containers:
        for link in container.select("a[href]"):
            add(link.get_text(" ", strip=True), normalize_url(config, link.get("href", "")))

    if config.seed_url not in seen:
        entries.insert(0, CatalogEntry(config.display_name, config.seed_url, 1))

    normalized = []
    seen.clear()
    for entry in entries:
        if entry.url in seen:
            continue
        seen.add(entry.url)
        normalized.append(CatalogEntry(entry.title, entry.url, len(normalized) + 1))

    save_json(config.catalog_file, [entry.__dict__ for entry in normalized])
    print("📘 发现 {} 文章：{} 篇".format(config.display_name, len(normalized)))
    return normalized[:args.limit] if args.limit > 0 else normalized


def safe_image_filename(abs_url: str) -> str:
    parsed = urlsplit(abs_url)
    base = os.path.basename(unquote(parsed.path)) or "image"
    base = re.sub(r"[^A-Za-z0-9._-]+", "_", base)
    stem, ext = os.path.splitext(base)
    if ext.lower() not in (".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"):
        ext = ".png"
    digest = hashlib.sha1(abs_url.encode("utf-8")).hexdigest()[:10]
    return "{}-{}{}".format((stem or "image")[:70].strip("._-") or "image", digest, ext)


def download_image(config: CourseConfig, session: requests.Session, src: str,
                   page_url: str, timeout: int, enabled: bool) -> str:
    src = (src or "").strip()
    if not src or src.startswith("data:"):
        return src
    abs_url = urljoin(page_url, html.unescape(src))
    if not enabled:
        return abs_url
    if abs_url in _img_cache:
        return _img_cache[abs_url]

    try:
        os.makedirs(config.image_dir, exist_ok=True)
        filename = safe_image_filename(abs_url)
        local_path = os.path.join(config.image_dir, filename)
        if not os.path.exists(local_path):
            resp = session.get(abs_url, headers=random_headers(page_url, image=True), timeout=timeout)
            resp.raise_for_status()
            with open(local_path, "wb") as file:
                file.write(resp.content)
        local_url = "{}/{}".format(config.image_url_prefix, filename)
        _img_cache[abs_url] = local_url
        return local_url
    except Exception as exc:
        print("  ⚠ 图片下载失败 {}：{}".format(src[:80], exc), file=sys.stderr)
        _img_cache[abs_url] = abs_url
        return abs_url


def remove_noise(content_el: Tag) -> Tag:
    for selector in (
        "script",
        "style",
        "nav",
        "header",
        "footer",
        "button",
        "iframe",
        ".sidebar",
        ".left-column",
        ".right-column",
        ".previous-next-links",
        ".article-heading-ad",
        ".google-auto-placed",
        ".tryit",
        "[class*='ad']",
        "[id*='ad']",
        "[class*='sidebar']",
        "[class*='nav']",
        "[class*='footer']",
    ):
        for tag in content_el.select(selector):
            tag.decompose()
    return content_el


def extract_content(config: CourseConfig, html_text: str, page_url: str, session: requests.Session,
                    image_timeout: int, download_images: bool) -> Tuple[str, str]:
    soup = BeautifulSoup(html_text, "html.parser")
    title = ""
    h1 = soup.find("h1")
    if h1:
        title = clean_text(h1.get_text(" ", strip=True))
    if not title:
        title_el = soup.find("title")
        if title_el:
            title = clean_text(title_el.get_text(" ", strip=True)).split("|")[0].strip()

    content_el: Optional[Tag] = None
    for selector in CONTENT_SELECTORS:
        content_el = soup.select_one(selector)
        if content_el:
            break
    if content_el is None:
        content_el = soup.body or soup
    content_el = remove_noise(content_el)

    for img_tag in content_el.find_all("img"):
        img_tag["src"] = download_image(
            config,
            session,
            img_tag.get("src", ""),
            page_url,
            timeout=image_timeout,
            enabled=download_images,
        )

    md = element_to_md(content_el)
    md = re.sub(r"\n{3,}", "\n\n", md).strip()
    if len(md) < 120:
        md = "## {}\n\n> 内容获取失败，请访问原文：{}\n".format(title or config.display_name, page_url)
    if page_url not in md:
        md += "\n\n---\n\n原文链接：{}\n".format(page_url)
    return title, md


def element_to_md(node) -> str:
    if isinstance(node, NavigableString):
        return str(node)
    if not isinstance(node, Tag):
        return ""

    name = node.name.lower()
    if name in ("script", "style"):
        return ""
    if name in ("h1", "h2", "h3", "h4", "h5", "h6"):
        level = int(name[1])
        return "\n{} {}\n".format("#" * level, clean_text(node.get_text(" ", strip=True)))
    if name == "p":
        return "\n{}\n".format(children_to_md(node).strip())
    if name == "br":
        return "\n"
    if name == "pre":
        code = node.get_text("\n", strip=False).strip("\n")
        return "\n```text\n{}\n```\n".format(code)
    if name == "code":
        text = node.get_text("", strip=False)
        return "`{}`".format(text.replace("`", "\\`"))
    if name == "a":
        text = clean_text(node.get_text(" ", strip=True))
        href = node.get("href", "").strip()
        if not href:
            return text
        return "[{}]({})".format(text or href, urljoin(BASE_URL + "/", href))
    if name == "img":
        src = node.get("src", "").strip()
        alt = clean_text(node.get("alt", "图片"))
        return "\n![{}]({})\n".format(alt, src)
    if name in ("ul", "ol"):
        lines = []
        ordered = name == "ol"
        for index, li in enumerate(node.find_all("li", recursive=False), start=1):
            text = children_to_md(li).strip()
            prefix = "{}. ".format(index) if ordered else "- "
            lines.append(prefix + re.sub(r"\n+", "\n  ", text))
        return "\n" + "\n".join(lines) + "\n"
    if name == "blockquote":
        text = children_to_md(node).strip()
        return "\n" + "\n".join("> " + line for line in text.splitlines()) + "\n"
    if name == "table":
        return table_to_md(node)
    return children_to_md(node)


def children_to_md(node: Tag) -> str:
    parts = [element_to_md(child) for child in node.children]
    text = "".join(parts)
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def table_to_md(table: Tag) -> str:
    rows = []
    for tr in table.find_all("tr"):
        cells = [clean_text(cell.get_text(" ", strip=True)) for cell in tr.find_all(["th", "td"])]
        if cells:
            rows.append(cells)
    if not rows:
        return ""
    width = max(len(row) for row in rows)
    rows = [row + [""] * (width - len(row)) for row in rows]
    lines = [
        "| " + " | ".join(rows[0]) + " |",
        "| " + " | ".join(["---"] * width) + " |",
    ]
    for row in rows[1:]:
        lines.append("| " + " | ".join(row) + " |")
    return "\n" + "\n".join(lines) + "\n"


def slug_from_url(url: str) -> str:
    path = unquote(urlsplit(url).path)
    slug = re.sub(r"\.html$", "", path.strip("/"))
    slug = re.sub(r"[^A-Za-z0-9]+", "-", slug).strip("-").lower()
    return ("runoob-" + slug)[:240]


def summary_from_content(content: str, max_len: int = 250) -> str:
    parts = []
    in_code = False
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith("```"):
            in_code = not in_code
            continue
        if in_code or not stripped or stripped.startswith(("#", "|", ">", "-", "!", "[")):
            continue
        text = re.sub(r"[`*_#\[\]\(\)]", "", stripped)
        if len(text) > 10:
            parts.append(text)
        if len(parts) >= 4:
            break
    summary = " ".join(parts).strip() or "菜鸟教程策展索引"
    return (summary[:max_len] + "...") if len(summary) > max_len else summary


def esc_sql(value: str) -> str:
    value = value or ""
    value = value.replace("\\", "\\\\").replace("'", "\\'")
    value = value.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n")
    return value.replace("\t", "\\t")


def sql_string_list(values: Sequence[str]) -> str:
    return ", ".join("'{}'".format(esc_sql(value)) for value in values)


def crawl_article(config: CourseConfig, entry: CatalogEntry,
                  article_id: int, args) -> Tuple[Optional[Dict], Optional[Dict]]:
    session = requests.Session()
    html_text = fetch_url(session, entry.url, timeout=args.timeout, retry=args.retry, referer=config.seed_url)
    if not html_text:
        return None, {
            "url": entry.url,
            "id": article_id,
            "title": entry.title,
            "category": config.category_name,
        }
    title, content = extract_content(
        config,
        html_text,
        entry.url,
        session,
        image_timeout=args.image_timeout,
        download_images=not args.no_images,
    )
    return {
        "id": article_id,
        "title": choose_article_title(title, entry.title, config),
        "category": config.category_name,
        "content": content,
        "url": entry.url,
        "slug": slug_from_url(entry.url),
        "order": entry.order,
    }, None


def collect_articles(config: CourseConfig, entries: Sequence[CatalogEntry],
                     progress: Dict, args) -> List[Dict]:
    done_urls: Dict[str, Dict] = progress.setdefault("done", {})
    results: List[Dict] = []
    tasks: List[Tuple[CatalogEntry, int]] = []

    for index, entry in enumerate(entries):
        article_id = config.article_id_start + index
        if entry.url in done_urls and not args.force:
            cached = dict(done_urls[entry.url])
            cached["id"] = article_id
            cached["order"] = entry.order
            cached["title"] = choose_article_title(cached.get("title", ""), entry.title, config)
            results.append(cached)
            print("  [cached] ID={} {}".format(article_id, cached.get("title", entry.title)))
        else:
            tasks.append((entry, article_id))

    if not tasks:
        return sorted(results, key=lambda item: int(item.get("order", item.get("id", 0))))

    print("\n🚀 开始抓取正文：{} 篇，workers={}".format(len(tasks), args.workers))
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as executor:
        future_map = {
            executor.submit(crawl_article, config, entry, article_id, args): (entry, article_id)
            for entry, article_id in tasks
        }
        for future in as_completed(future_map):
            entry, article_id = future_map[future]
            try:
                article, failed = future.result()
            except Exception as exc:
                article, failed = None, {
                    "url": entry.url,
                    "id": article_id,
                    "title": entry.title,
                    "category": config.category_name,
                    "reason": str(exc),
                }
            if failed:
                progress.setdefault("failed", [])
                if failed["url"] not in [item.get("url") for item in progress["failed"]]:
                    progress["failed"].append(failed)
                save_json(config.progress_file, progress)
                print("  ✗ ID={} {}".format(article_id, entry.title))
                continue
            if article:
                results.append(article)
                done_urls[article["url"]] = article
                progress["done"] = done_urls
                progress["failed"] = [
                    item for item in progress.get("failed", [])
                    if item.get("url") != article["url"]
                ]
                save_json(config.progress_file, progress)
                print("  ✓ ID={} {}（{} 字符）".format(
                    article_id,
                    article["title"],
                    len(article["content"]),
                ))
    return sorted(results, key=lambda item: int(item.get("order", item.get("id", 0))))


def generate_sql(config: CourseConfig, articles: Sequence[Dict], dry_run: bool = False) -> None:
    if not articles:
        print("⚠ 没有可生成 SQL 的文章")
        return

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    article_cols = (
        "`id`,`author_id`,`title`,`summary`,`content`,`cover_url`,"
        "`category`,`offline_reason`,`featured`,`featured_at`,`slug`,"
        "`seo_title`,`seo_description`,`warn_flag`,`status`,"
        "`view_count`,`like_count`,`favorite_count`,`comment_count`,"
        "`published_at`,`created_at`,`updated_at`,`deleted_at`,`version`"
    )
    lines = [
        "-- ============================================================",
        "-- 菜鸟教程 {}：共 {} 篇  生成时间：{}".format(config.display_name, len(articles), now),
        "-- 来源：{}".format(config.seed_url),
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "START TRANSACTION;",
        "",
    ]
    lines.append(
        "INSERT INTO `blog_category` "
        "(`name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
        "('{name}','{description}',{sort_order},1,'NORMAL','{now}','{now}',NULL,0) "
        "ON DUPLICATE KEY UPDATE `description`=VALUES(`description`),`enabled`=1,"
        "`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
            name=esc_sql(config.category_name),
            description=esc_sql("菜鸟教程 {} 策展索引".format(config.display_name)),
            sort_order=config.sort_order,
            now=now,
        )
    )
    for tag in config.tags:
        lines.append(
            "INSERT INTO `blog_tag` "
            "(`name`,`description`,`use_count`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "('{tag}','{description}',{count},1,'NORMAL','{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE `description`=VALUES(`description`),`enabled`=1,"
            "`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
                tag=esc_sql(tag),
                description=esc_sql("菜鸟教程 {} 自动导入标签：{}".format(config.display_name, tag)),
                count=len(articles),
                now=now,
            )
        )

    lines.append("")
    article_upsert_suffix = (
        " ON DUPLICATE KEY UPDATE "
        "`author_id`=VALUES(`author_id`),`title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`content`=VALUES(`content`),`cover_url`=VALUES(`cover_url`),`category`=VALUES(`category`),"
        "`offline_reason`=VALUES(`offline_reason`),`featured`=VALUES(`featured`),"
        "`featured_at`=VALUES(`featured_at`),`seo_title`=VALUES(`seo_title`),"
        "`seo_description`=VALUES(`seo_description`),`warn_flag`=VALUES(`warn_flag`),"
        "`status`=VALUES(`status`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL"
    )
    for index, article in enumerate(articles):
        order = int(article.get("order", index + 1))
        pub_at = datetime.fromtimestamp(datetime(2025, 7, 1).timestamp() + (order - 1) * 86400)
        pub_text = pub_at.strftime("%Y-%m-%d %H:%M:%S")
        title = str(article.get("title", config.display_name))[:120]
        content = str(article.get("content", ""))
        lines.append(
            "INSERT INTO `blog_article` ({cols}) VALUES "
            "({id},{author_id},'{title}','{summary}','{content}','{cover_url}',"
            "'{category}',NULL,0,NULL,'{slug}','{seo_title}','{seo_desc}',0,'PUBLISHED',"
            "{view},{like},{favorite},{comment},'{pub_at}','{pub_at}','{now}',NULL,0)"
            "{suffix};".format(
                cols=article_cols,
                id=int(article["id"]),
                author_id=AUTHOR_ID,
                title=esc_sql(title),
                summary=esc_sql(summary_from_content(content, 300)),
                content=esc_sql(content),
                cover_url=esc_sql(config.cover_url),
                category=esc_sql(config.category_name),
                slug=esc_sql(article.get("slug", "")),
                seo_title=esc_sql(title + " - " + config.display_name),
                seo_desc=esc_sql(summary_from_content(content, 120)),
                view=2500 + order * 137,
                like=80 + order * 7,
                favorite=45 + order * 5,
                comment=8 + order,
                pub_at=pub_text,
                now=now,
                suffix=article_upsert_suffix,
            )
        )

    article_ids = [int(article["id"]) for article in articles]
    lines.append("")
    lines.append(
        "UPDATE `blog_article_tag` SET `deleted_at`='{now}',`updated_at`='{now}' "
        "WHERE `article_id` IN ({ids});".format(now=now, ids=", ".join(str(item) for item in article_ids))
    )
    for article in articles:
        for tag in config.tags:
            lines.append(
                "INSERT INTO `blog_article_tag` "
                "(`article_id`,`tag_name`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
                "({article_id},'{tag}','{now}','{now}',NULL,0) "
                "ON DUPLICATE KEY UPDATE `deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                    article_id=int(article["id"]),
                    tag=esc_sql(tag),
                    now=now,
                )
            )

    source_note = "来源：菜鸟教程 {}".format(config.seed_url)
    lines.append("")
    lines.append(
        "INSERT INTO `blog_column` "
        "(`id`,`author_id`,`title`,`summary`,`cover_url`,`status`,`sort_order`,`subscriber_count`,"
        "`article_count`,`intro`,`difficulty`,`estimated_minutes`,`source_type`,`source_note`,"
        "`recommended`,`recommend_weight`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
        "({column_id},{author_id},'{title}','{summary}','{cover_url}','PUBLISHED',{sort_order},0,{count},"
        "'{intro}','{difficulty}',{minutes},'CURATED','{source_note}',1,{weight},'{now}','{now}',NULL,0) "
        "ON DUPLICATE KEY UPDATE `title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`cover_url`=VALUES(`cover_url`),`status`=VALUES(`status`),`article_count`=VALUES(`article_count`),"
        "`intro`=VALUES(`intro`),`difficulty`=VALUES(`difficulty`),"
        "`estimated_minutes`=VALUES(`estimated_minutes`),`source_type`=VALUES(`source_type`),"
        "`source_note`=VALUES(`source_note`),`recommended`=VALUES(`recommended`),"
        "`recommend_weight`=VALUES(`recommend_weight`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
            column_id=config.column_id,
            author_id=AUTHOR_ID,
            title=esc_sql(config.column_title),
            summary=esc_sql("菜鸟教程 {} 系列文章策展索引，共 {} 篇。".format(config.display_name, len(articles))),
            cover_url=esc_sql(config.cover_url),
            sort_order=config.sort_order,
            count=len(articles),
            intro=esc_sql(config.intro),
            difficulty=esc_sql(config.difficulty),
            minutes=max(30, len(articles) * 12),
            source_note=esc_sql(source_note),
            weight=config.recommend_weight,
            now=now,
        )
    )
    lines.append(
        "INSERT INTO `blog_topic` "
        "(`id`,`title`,`summary`,`cover_url`,`status`,`sort_order`,`article_count`,`intro`,"
        "`difficulty`,`estimated_minutes`,`source_type`,`source_note`,`recommended`,`recommend_weight`,"
        "`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
        "({topic_id},'{title}','{summary}','{cover_url}','PUBLISHED',{sort_order},{count},'{intro}',"
        "'{difficulty}',{minutes},'CURATED','{source_note}',1,{weight},'{now}','{now}',NULL,0) "
        "ON DUPLICATE KEY UPDATE `title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`cover_url`=VALUES(`cover_url`),`status`=VALUES(`status`),`article_count`=VALUES(`article_count`),"
        "`intro`=VALUES(`intro`),`difficulty`=VALUES(`difficulty`),"
        "`estimated_minutes`=VALUES(`estimated_minutes`),`source_type`=VALUES(`source_type`),"
        "`source_note`=VALUES(`source_note`),`recommended`=VALUES(`recommended`),"
        "`recommend_weight`=VALUES(`recommend_weight`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;".format(
            topic_id=config.topic_id,
            title=esc_sql(config.topic_title),
            summary=esc_sql("菜鸟教程 {} 学习路径，共 {} 篇。".format(config.display_name, len(articles))),
            cover_url=esc_sql(config.cover_url),
            sort_order=config.sort_order,
            count=len(articles),
            intro=esc_sql(config.intro),
            difficulty=esc_sql(config.difficulty),
            minutes=max(30, len(articles) * 12),
            source_note=esc_sql(source_note),
            weight=config.recommend_weight,
            now=now,
        )
    )

    lines.append("")
    lines.append(
        "UPDATE `blog_column_article` SET `deleted_at`='{now}',`updated_at`='{now}' "
        "WHERE `column_id`={column_id};".format(now=now, column_id=config.column_id)
    )
    lines.append(
        "UPDATE `blog_topic_article` SET `deleted_at`='{now}',`updated_at`='{now}' "
        "WHERE `topic_id`={topic_id};".format(now=now, topic_id=config.topic_id)
    )
    for index, article in enumerate(articles, start=1):
        article_id = int(article["id"])
        title = esc_sql(str(article.get("title", ""))[:80])
        section = esc_sql(config.section_title)
        lines.append(
            "INSERT INTO `blog_column_article` "
            "(`column_id`,`article_id`,`sort_order`,`section_title`,`step_order`,`required`,"
            "`editor_note`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "({column_id},{article_id},{sort_order},'{section}',{step},1,'{note}','{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE `sort_order`=VALUES(`sort_order`),"
            "`section_title`=VALUES(`section_title`),`step_order`=VALUES(`step_order`),"
            "`required`=VALUES(`required`),`editor_note`=VALUES(`editor_note`),"
            "`deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                column_id=config.column_id,
                article_id=article_id,
                sort_order=index,
                section=section,
                step=index,
                note=title,
                now=now,
            )
        )
        lines.append(
            "INSERT INTO `blog_topic_article` "
            "(`topic_id`,`article_id`,`sort_order`,`section_title`,`step_order`,`required`,"
            "`editor_note`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES "
            "({topic_id},{article_id},{sort_order},'{section}',{step},1,'{note}','{now}','{now}',NULL,0) "
            "ON DUPLICATE KEY UPDATE `sort_order`=VALUES(`sort_order`),"
            "`section_title`=VALUES(`section_title`),`step_order`=VALUES(`step_order`),"
            "`required`=VALUES(`required`),`editor_note`=VALUES(`editor_note`),"
            "`deleted_at`=NULL,`updated_at`=VALUES(`updated_at`);".format(
                topic_id=config.topic_id,
                article_id=article_id,
                sort_order=index,
                section=section,
                step=index,
                note=title,
                now=now,
            )
        )

    lines.append("")
    lines.append(
        "UPDATE `blog_tag` t SET `use_count` = ("
        "SELECT COUNT(*) FROM `blog_article_tag` atg "
        "INNER JOIN `blog_article` a ON a.`id` = atg.`article_id` "
        "WHERE atg.`tag_name` = t.`name` AND atg.`deleted_at` IS NULL "
        "AND a.`status` = 'PUBLISHED' AND a.`deleted_at` IS NULL"
        ") WHERE t.`name` IN ({});".format(sql_string_list(config.tags))
    )
    lines.append(
        "UPDATE `blog_column` c SET `article_count` = ("
        "SELECT COUNT(*) FROM `blog_column_article` ca "
        "INNER JOIN `blog_article` a ON a.`id` = ca.`article_id` "
        "WHERE ca.`column_id` = c.`id` AND ca.`deleted_at` IS NULL "
        "AND a.`status` = 'PUBLISHED' AND a.`deleted_at` IS NULL"
        ") WHERE c.`id` = {};".format(config.column_id)
    )
    lines.append(
        "UPDATE `blog_topic` t SET `article_count` = ("
        "SELECT COUNT(*) FROM `blog_topic_article` ta "
        "INNER JOIN `blog_article` a ON a.`id` = ta.`article_id` "
        "WHERE ta.`topic_id` = t.`id` AND ta.`deleted_at` IS NULL "
        "AND a.`status` = 'PUBLISHED' AND a.`deleted_at` IS NULL"
        ") WHERE t.`id` = {};".format(config.topic_id)
    )
    lines.append("COMMIT;")

    sql = "\n".join(lines) + "\n"
    if dry_run:
        print(sql[:3000] + "\n... (dry-run，仅显示前 3000 字符)")
        return
    os.makedirs(os.path.dirname(config.sql_output), exist_ok=True)
    with open(config.sql_output, "w", encoding="utf-8") as file:
        file.write(sql)
    print("💾 SQL 已写入：{} ({} 篇)".format(os.path.normpath(config.sql_output), len(articles)))


def build_parser(config: CourseConfig) -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="爬取菜鸟教程 {} 并生成 SQL".format(config.display_name))
    parser.add_argument("--catalog-only", action="store_true", help="只发现目录，不抓正文、不生成 SQL")
    parser.add_argument("--progress-sql", action="store_true", help="只根据本地进度生成 SQL")
    parser.add_argument("--refresh-catalog", action="store_true", help="忽略目录缓存，重新发现目录")
    parser.add_argument("--reset", action="store_true", help="清空进度后重爬")
    parser.add_argument("--force", action="store_true", help="忽略已完成进度，重新抓取正文")
    parser.add_argument("--dry-run", action="store_true", help="只打印 SQL 片段，不写文件")
    parser.add_argument("--no-images", action="store_true", help="不下载正文图片，保留远程图片地址")
    parser.add_argument("--workers", type=int, default=4, help="正文并发抓取线程数，默认 4")
    parser.add_argument("--timeout", type=int, default=20, help="HTTP 超时秒数，默认 20")
    parser.add_argument("--image-timeout", type=int, default=15, help="图片下载超时秒数，默认 15")
    parser.add_argument("--retry", type=int, default=2, help="失败重试次数，默认 2")
    parser.add_argument("--limit", type=int, default=0, help="最多处理 N 篇，0 表示不限制")
    return parser


def run(config: CourseConfig) -> None:
    args = build_parser(config).parse_args()
    if args.reset and os.path.exists(config.progress_file):
        os.remove(config.progress_file)
        print("🗑 已清空进度文件：{}".format(os.path.normpath(config.progress_file)))

    args.workers = max(1, args.workers)
    progress = load_progress(config)

    if args.progress_sql:
        articles = list(progress.get("done", {}).values())
        articles.sort(key=lambda item: int(item.get("order", item.get("id", 0))))
        generate_sql(config, articles, dry_run=args.dry_run)
        return

    session = requests.Session()
    entries = discover_catalog(session, config, args)
    print("📋 本次目标文章：{} 篇".format(len(entries)))

    if args.catalog_only:
        return

    articles = collect_articles(config, entries, progress, args)
    generate_sql(config, articles, dry_run=args.dry_run)

    failed = progress.get("failed", [])
    print("\n✅ 累计完成：{}  ✗ 失败：{}".format(len(progress.get("done", {})), len(failed)))
    if failed:
        print("失败列表：")
        for item in failed:
            print("  ID={} {} {}".format(item.get("id"), item.get("title"), item.get("url")))
