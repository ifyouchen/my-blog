#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
waylandz.com/ai-agent-book/ 全量章节爬虫 + 分类 SQL 生成脚本 (Python 3.8+ 兼容)

功能：
  - 自动从目录页提取所有章节链接（无需手动列举）
  - 按 Part 分类爬取章节内容
  - 进度持久化到 scripts/crawl_waylandz_progress.json，支持断点续爬
  - 生成 INSERT SQL 文件到 backend/src/main/resources/db/articles/articles_ai_agent.sql

用法：
  cd /path/to/my-blog
  python3 scripts/crawl_waylandz.py                          # 全量爬取
  python3 scripts/crawl_waylandz.py --dry-run                # 只打印不写文件
  python3 scripts/crawl_waylandz.py --reset                  # 清空进度，重新开始
  python3 scripts/crawl_waylandz.py --delay 2 --timeout 30  # 调整请求间隔
  python3 scripts/crawl_waylandz.py --limit 5               # 仅爬取前5篇（测试用）

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
from typing import Dict, List, Optional, Tuple

try:
    import requests
    from bs4 import BeautifulSoup, NavigableString, Tag
except ImportError:
    print("缺少依赖，请先安装：pip3 install requests beautifulsoup4")
    sys.exit(1)

# ---------------------------------------------------------------------------
# 站点基本配置
# ---------------------------------------------------------------------------
BASE_URL = "https://www.waylandz.com"
INDEX_PATH = "/ai-agent-book/"
INDEX_URL = BASE_URL + INDEX_PATH

# 文章 ID 起始（避免与 pdai 数据冲突）
ID_START = 200001

# 分类名（博客中显示）
CATEGORY_NAME = "AI Agent"

# ---------------------------------------------------------------------------
# 进度文件 & SQL 输出路径
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

PROGRESS_FILE = os.path.join(SCRIPT_DIR, "crawl_waylandz_progress.json")

SQL_OUTPUT_FILE = os.path.join(
    SCRIPT_DIR, "..", "backend", "src", "main", "resources", "db", "articles",
    "articles_ai_agent.sql"
)

# ---------------------------------------------------------------------------
# 反爬：User-Agent 池
# ---------------------------------------------------------------------------
_UA_POOL: List[str] = [
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_4) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:125.0) "
    "Gecko/20100101 Firefox/125.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
]

_REFERER_POOL: List[str] = [
    "https://www.waylandz.com/",
    "https://www.waylandz.com/ai-agent-book/",
    "https://www.google.com/search?q=AI+Agent+架构",
    "https://www.bing.com/search?q=waylandz+ai+agent",
]


def _random_headers(referer: Optional[str] = None) -> Dict[str, str]:
    """每次请求随机生成 Headers，模拟真实浏览器行为"""
    ua = random.choice(_UA_POOL)
    ref = referer or random.choice(_REFERER_POOL)
    return {
        "User-Agent": ua,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": random.choice([
            "zh-CN,zh;q=0.9,en;q=0.8",
            "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        ]),
        "Accept-Encoding": "gzip, deflate",
        "Connection": "keep-alive",
        "Referer": ref,
        "Upgrade-Insecure-Requests": "1",
        "Cache-Control": random.choice(["max-age=0", "no-cache"]),
    }


# ---------------------------------------------------------------------------
# 反爬：人工节奏延迟
# ---------------------------------------------------------------------------

def _human_delay(base: float, idx: int) -> None:
    """模拟人工阅读节奏，避免被反爬"""
    jitter = base * random.uniform(0.6, 1.4)
    time.sleep(jitter)

    if random.random() < 0.15:
        pause = random.uniform(2.0, 6.0)
        print("  … 模拟阅读停顿 {:.1f}s".format(pause))
        time.sleep(pause)

    batch_size = getattr(_human_delay, "_batch", random.randint(6, 10))
    _human_delay._batch = batch_size  # type: ignore[attr-defined]
    count = getattr(_human_delay, "_count", 0) + 1
    _human_delay._count = count  # type: ignore[attr-defined]
    if count >= batch_size:
        rest = random.uniform(10.0, 20.0)
        print("  ☕ 批次休息 {:.0f}s（已处理 {} 篇）…".format(rest, count))
        time.sleep(rest)
        _human_delay._count = 0  # type: ignore[attr-defined]
        _human_delay._batch = random.randint(6, 10)  # type: ignore[attr-defined]


# ---------------------------------------------------------------------------
# 进度持久化
# ---------------------------------------------------------------------------

def load_progress() -> Dict:
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
    try:
        with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
            json.dump(progress, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print("⚠ 进度保存失败：{}".format(e), file=sys.stderr)


# ---------------------------------------------------------------------------
# HTTP 请求
# ---------------------------------------------------------------------------

def fetch_url(url: str, timeout: int = 20, retry: int = 2,
              prev_url: Optional[str] = None) -> Optional[str]:
    """抓取 URL 返回 HTML，失败后指数退避重试"""
    from urllib.parse import urlparse, urlunparse, quote
    # 将 URL 中的非 ASCII 路径部分进行百分号编码，避免 latin-1 编码错误
    try:
        parsed = urlparse(url)
        encoded_path = quote(parsed.path, safe="/-_.~!$&'()*+,;=:@")
        url = urlunparse(parsed._replace(path=encoded_path))
    except Exception:
        pass

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
                print("  ⚠ 第 {} 次重试（{:.1f}s 后）: {}".format(attempt, backoff, e),
                      file=sys.stderr)
                time.sleep(backoff)
                referer = random.choice(_REFERER_POOL)
            else:
                print("  ✗ 请求失败（已重试 {} 次）: {}".format(retry, e), file=sys.stderr)
    return None


# ---------------------------------------------------------------------------
# 目录解析：从首页自动提取所有章节链接
# ---------------------------------------------------------------------------

def _normalize_url(url: str) -> str:
    """将 URL 中的 %XX 编码解码为 Unicode，用于去重比较"""
    try:
        from urllib.parse import unquote
        return unquote(url)
    except Exception:
        return url


def parse_toc(html: str) -> List[Tuple[str, str]]:
    """
    解析 ai-agent-book 目录页，返回 [(title, url), ...] 列表。
    URL 格式为绝对地址，如 https://www.waylandz.com/ai-agent-book/前言/
    顺序与页面目录一致。
    同一章节若同时有中文 URL 和 %XX 编码 URL，以中文版为准（先出现的保留）。
    """
    from urllib.parse import unquote
    soup = BeautifulSoup(html, "html.parser")
    entries: List[Tuple[str, str]] = []
    # seen_normalized：已出现的规范化 URL（解码后）→ 对应的实际 URL
    seen_normalized: Dict[str, str] = {}

    for a in soup.find_all("a", href=True):
        href = a["href"].strip()
        # 只保留 /ai-agent-book/某子路径/ 格式
        if not re.match(r"^/ai-agent-book/.+", href):
            continue
        # 排除语言切换链接（/ai-agent-book-en/、/ai-agent-book-ja/）
        if "agent-book-en" in href or "agent-book-ja" in href:
            continue
        abs_url = BASE_URL + href
        normalized = unquote(abs_url)
        if normalized in seen_normalized:
            # 已有同一章节，跳过
            continue
        seen_normalized[normalized] = abs_url
        title = a.get_text(strip=True) or href.split("/")[-2]
        entries.append((title, abs_url))

    return entries


# ---------------------------------------------------------------------------
# 正文提取
# ---------------------------------------------------------------------------

# waylandz.com 的正文容器选择器（优先级从高到低）
CONTENT_SELECTORS = [
    "article.prose",
    "div.prose",
    ".content",
    "main article",
    "article",
    "main",
]

# 需要从正文中移除的干扰元素
_NOISE_SELECTORS = (
    "nav, header, footer, aside, "
    ".sidebar, .toc, script, style, button, "
    "[class*='sidebar'], [class*='nav'], [class*='footer'], "
    "[class*='toc'], [class*='edit'], [class*='cite'], "
    "[class*='breadcrumb'], [class*='pagination']"
)


def extract_content(html_text: str, url: str) -> Tuple[str, str]:
    """从 HTML 中提取标题和正文（Markdown 格式）"""
    soup = BeautifulSoup(html_text, "html.parser")

    # ---- 提取标题 ----
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
            raw = t.get_text(strip=True)
            # 去掉站点名后缀，如 "第 1 章：... | AI Agent 开发实战"
            title = raw.split("|")[0].strip()

    # ---- 找正文容器 ----
    content_el = None
    for sel in CONTENT_SELECTORS:
        content_el = soup.select_one(sel)
        if content_el:
            break
    if not content_el:
        content_el = soup.body or soup

    # ---- 移除干扰元素 ----
    for tag in content_el.select(_NOISE_SELECTORS):
        tag.decompose()

    # ---- 移除引用/版权区块（waylandz 页尾的 "引用本文 / Cite" 区域）----
    # 匹配包含 "引用本文" 或 "APA格式" 或 "BibTeX" 文字的段落/div
    for tag in content_el.find_all(string=re.compile(r"引用本文|APA格式|BibTeX|@incollection")):
        parent = tag.parent
        if parent:
            # 向上查找最近的 block 级容器，一并删除
            for _ in range(3):
                if parent and parent.name in ("div", "section", "aside", "p", "pre"):
                    parent.decompose()
                    break
                if parent:
                    parent = parent.parent

    # ---- 转换为 Markdown ----
    md = _el_to_md(content_el)
    md = re.sub(r"\n{3,}", "\n\n", md).strip()

    # 内容过短时标记为失败
    if len(md) < 100:
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


def _walk(el, lines: List[str]):  # noqa: C901
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
        text = el.get_text(" ", strip=True)
        if text:
            lines.extend(["", text, ""])
        return

    if n in ("ul", "ol"):
        lines.append("")
        for i, li in enumerate(el.find_all("li", recursive=False)):
            pfx = "{}. ".format(i + 1) if n == "ol" else "- "
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
        text = el.get_text(" ", strip=True)
        if text:
            lines.extend(["", "> " + text, ""])
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
        # waylandz.com 的图片大多是 SVG 示意图，保留 alt 文字即可
        alt = el.get("alt", "").strip() or "图示"
        src = el.get("src", "").strip()
        if src:
            abs_src = (BASE_URL + src) if src.startswith("/") else src
            lines.extend(["", "![{}]({})".format(alt, abs_src), ""])
        return

    if n == "a":
        text = el.get_text(strip=True)
        if text:
            _app(lines, text)
        return

    for child in el.children:
        _walk(child, lines)


# ---------------------------------------------------------------------------
# 辅助：slug / 摘要 / SQL 转义
# ---------------------------------------------------------------------------

def _esc(s: str) -> str:
    """转义 SQL 字符串"""
    s = s.replace("\\", "\\\\").replace("'", "\\'")
    s = s.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n")
    return s.replace("\t", "\\t")


def _slug(url: str) -> str:
    """从 URL 中提取 slug，如 .../ai-agent-book/第01章-Agent的本质/ -> di01zhang-agentde-ben-zhi"""
    # 取最后一个非空段
    parts = [p for p in url.rstrip("/").split("/") if p]
    raw = parts[-1] if parts else "unknown"
    # 只保留 ASCII 字母、数字、连字符，转为 URL-safe slug
    slug = re.sub(r"[^\w\-]", "-", raw, flags=re.UNICODE)
    slug = re.sub(r"-{2,}", "-", slug).strip("-").lower()
    # 前缀加 waylandz- 避免与其它来源 slug 冲突
    return "waylandz-" + slug[:80]


def _summary(content: str, max_len: int = 250) -> str:
    """提取摘要（取前几段有意义的文字）"""
    parts = []
    for line in content.splitlines():
        s = line.strip()
        if (s and len(s) > 15
                and not s.startswith(("#", "|", "```", ">", "-", "!", "*"))):
            parts.append(re.sub(r"[`*]", "", s))
        if len(parts) >= 4:
            break
    text = " ".join(parts)
    return (text[:max_len] + "...") if len(text) > max_len else (text or "暂无摘要")


# AI Agent 主题封面图（Unsplash）
_COVER_URLS = [
    "https://images.unsplash.com/photo-1677442135703-1787eea5ce01?w=800",
    "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800",
    "https://images.unsplash.com/photo-1676573393047-fb5ab3823f9b?w=800",
    "https://images.unsplash.com/photo-1633356122102-3fe601e05bd2?w=800",
    "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800",
    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800",
]


def _cover() -> str:
    return random.choice(_COVER_URLS)


# ---------------------------------------------------------------------------
# SQL 生成
# ---------------------------------------------------------------------------

def generate_sql(articles: List[Dict], output_path: str, dry_run: bool = False) -> None:
    """生成 INSERT SQL 文件"""
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    published_base = datetime(2026, 1, 1)

    col_names = (
        "`id`,`author_id`,`title`,`summary`,`content`,`cover_url`,"
        "`category`,`offline_reason`,`featured`,`featured_at`,`slug`,"
        "`seo_title`,`seo_description`,`warn_flag`,`status`,"
        "`view_count`,`like_count`,`favorite_count`,`comment_count`,"
        "`published_at`,`created_at`,`updated_at`,`deleted_at`,`version`"
    )

    lines = [
        "-- ============================================================",
        "-- 来源：waylandz.com/ai-agent-book/  共 {} 篇  生成时间：{}".format(
            len(articles), now),
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "",
        "INSERT INTO `blog_article` ({}) VALUES".format(col_names),
    ]

    rows = []
    for i, art in enumerate(articles):
        title_esc = _esc(art["title"])
        summary_esc = _esc(_summary(art["content"]))
        content_esc = _esc(art["content"])
        cover_esc = _esc(_cover())
        slug_esc = _esc(art["slug"])
        seo_title_esc = _esc(art["title"] + " - AI Agent 架构")
        seo_desc_esc = _esc(_summary(art["content"], 120))

        # 发布时间：从 2026-01-01 起，每篇间隔 1-2 天
        days_offset = i * random.randint(1, 2)
        pub_ts = published_base.timestamp() + days_offset * 86400
        pub_dt = datetime.fromtimestamp(pub_ts).strftime("%Y-%m-%d %H:%M:%S")

        view_count = random.randint(300, 5000)
        like_count = random.randint(20, int(view_count * 0.12))
        fav_count = random.randint(10, int(view_count * 0.06))
        comment_count = random.randint(0, int(view_count * 0.02))

        author_id = 1 if i % 2 == 0 else 2

        row = (
            "({aid},{author_id},'{title}','{summary}','{content}',"
            "'{cover_url}','AI Agent',NULL,0,NULL,'{slug}',"
            "'{seo_title}','{seo_desc}',0,'PUBLISHED',"
            "{view},{like},{fav},{cmt},"
            "'{pub_at}','{created}','{updated}',NULL,0)"
        ).format(
            aid=art["id"], author_id=author_id,
            title=title_esc, summary=summary_esc, content=content_esc,
            cover_url=cover_esc,
            slug=slug_esc, seo_title=seo_title_esc, seo_desc=seo_desc_esc,
            view=view_count, like=like_count, fav=fav_count, cmt=comment_count,
            pub_at=pub_dt, created=pub_dt, updated=now,
        )
        rows.append(row)

    lines.append(",\n".join(rows) + ";")
    lines.append("")
    sql = "\n".join(lines)

    if dry_run:
        print(sql[:3000] + "\n... (dry-run，仅显示前3000字符)")
    else:
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(sql)
        print("  💾 SQL 已写入：{} ({} 条 INSERT)".format(
            os.path.basename(output_path), len(articles)))


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser(
        description="爬取 waylandz.com/ai-agent-book/ 全量章节，生成 INSERT SQL"
    )
    p.add_argument("--dry-run", action="store_true",
                   help="仅打印 SQL，不写文件")
    p.add_argument("--reset", action="store_true",
                   help="清空进度文件，重新爬取所有章节")
    p.add_argument("--delay", type=float, default=2.0,
                   help="每次请求基础间隔秒数（默认 2.0）")
    p.add_argument("--timeout", type=int, default=20,
                   help="HTTP 超时秒数（默认 20）")
    p.add_argument("--retry", type=int, default=2,
                   help="失败后最大重试次数（默认 2）")
    p.add_argument("--limit", type=int, default=0,
                   help="最多爬取 N 篇（0=不限制，用于测试）")
    args = p.parse_args()

    # 清空进度
    if args.reset and os.path.exists(PROGRESS_FILE):
        os.remove(PROGRESS_FILE)
        print("🗑  已清空进度文件，将重新爬取所有章节")

    progress = load_progress()
    done_urls: Dict[str, Dict] = progress.get("done", {})

    # ---- 第一步：爬取目录页，获取所有章节链接 ----
    print("\n📋 正在获取章节目录：{}".format(INDEX_URL))
    index_html = fetch_url(INDEX_URL, timeout=args.timeout, retry=args.retry)
    if not index_html:
        print("✗ 无法获取目录页，请检查网络连接", file=sys.stderr)
        sys.exit(1)

    toc = parse_toc(index_html)
    if not toc:
        print("✗ 目录解析失败，未找到任何章节链接", file=sys.stderr)
        sys.exit(1)

    print("✅ 共发现 {} 个章节".format(len(toc)))
    for title, url in toc:
        print("  - {} → {}".format(title, url))

    # ---- 第二步：逐章节爬取 ----
    limit = args.limit if args.limit > 0 else 9999999
    pending = [(t, u) for t, u in toc if u not in done_urls]
    total = min(len(pending), limit)

    print("\n待爬取：{} / 总计：{}\n".format(total, len(toc)))

    results: List[Dict] = []
    # 先把已完成的按 toc 顺序放回来
    for _, url in toc:
        if url in done_urls:
            results.append(done_urls[url])

    global_idx = 0
    prev_url: Optional[str] = None

    for default_title, url in toc:
        if url in done_urls:
            continue
        if global_idx >= limit:
            print("\n[LIMIT] 已达到 --limit={} 限制，停止爬取".format(limit))
            break

        global_idx += 1
        art_id = ID_START + len(done_urls) + global_idx - 1
        print("[{}/{}] ID={}  {}".format(global_idx, total, art_id, url))

        html = fetch_url(url, timeout=args.timeout, retry=args.retry, prev_url=prev_url)
        if html is None:
            print("  ✗ 跳过（将加入失败列表）")
            progress.setdefault("failed", [])
            if url not in [f["url"] for f in progress["failed"]]:
                progress["failed"].append({
                    "url": url, "id": art_id, "title": default_title
                })
            save_progress(progress)
            continue

        title, content = extract_content(html, url)
        final_title = title or default_title
        print("  ✓ {}（{} 字符）".format(final_title, len(content)))

        art_data: Dict = {
            "id": art_id,
            "title": final_title,
            "content": content,
            "url": url,
            "slug": _slug(url),
        }
        results.append(art_data)
        done_urls[url] = art_data
        progress["done"] = done_urls
        save_progress(progress)
        prev_url = url

        if global_idx < total:
            _human_delay(args.delay, global_idx)

    # ---- 第三步：生成 SQL ----
    if results:
        print("\n  ✍ 生成 SQL 文件...")
        generate_sql(results, os.path.normpath(SQL_OUTPUT_FILE), dry_run=args.dry_run)
    else:
        print("  ⚠ 无可用数据，跳过 SQL 生成")

    # ---- 汇总 ----
    print("\n" + "=" * 55)
    failed_list = progress.get("failed", [])
    print("✅ 累计完成：{}  ✗ 失败：{}".format(len(done_urls), len(failed_list)))
    if failed_list:
        print("\n失败列表（重新运行脚本会自动重试，或用 --reset 全部重跑）：")
        for f in failed_list:
            print("  ID={}  {}  {}".format(f["id"], f["title"], f["url"]))
    else:
        print("🎉 所有章节爬取成功！")

    if not args.dry_run:
        print("\nSQL 文件位于：{}".format(os.path.normpath(SQL_OUTPUT_FILE)))


if __name__ == "__main__":
    main()

