"""
Crawl Waylandz AI Agent book pages, download images, and generate import SQL.

The crawler is intentionally single-threaded with randomized delays. It is
meant for respectful personal learning imports, not high-throughput scraping.
"""
import argparse
import hashlib
import html
from html.parser import HTMLParser
import json
import os
import random
import re
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote, unquote, urljoin, urlparse
from urllib.request import Request, urlopen


BASE_DIR = Path(__file__).resolve().parents[1]
SQL_DIR = BASE_DIR / "src" / "main" / "resources" / "db" / "articles"
UPLOADS_DIR = BASE_DIR / "uploads"
CACHE_DIR = BASE_DIR / "scripts" / "download_cache" / "waylandz_ai_agent_book"
ARTICLE_CACHE_DIR = CACHE_DIR / "articles"
IMAGE_DIR = UPLOADS_DIR / "waylandz" / "ai-agent-book"
PROGRESS_PATH = CACHE_DIR / "progress.json"
DEFAULT_OUTPUT = SQL_DIR / "articles_ai_agent.sql"

BOOK_URL = "https://www.waylandz.com/ai-agent-book/README/"
SOURCE_NOTE = "来源：waylandz.com/ai-agent-book/"
DEFAULT_COVER = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800"

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.5 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
]

SECTION_TAGS = {
    "前言": ["AI Agent", "技术写作"],
    "Part 1 - Agent 基础": ["AI Agent", "ReAct"],
    "Part 2 - 工具与扩展": ["AI Agent", "工具调用", "MCP"],
    "Part 3 - 上下文与记忆": ["AI Agent", "上下文工程", "记忆系统"],
    "Part 4 - 单 Agent 模式": ["AI Agent", "Planning", "Reflection"],
    "Part 5 - 多 Agent 编排": ["AI Agent", "多智能体", "DAG"],
    "Part 6 - 高级推理": ["AI Agent", "推理模式"],
    "Part 7 - 生产架构": ["AI Agent", "生产架构", "可观测性"],
    "Part 8 - 企业级特性": ["AI Agent", "企业级架构", "安全"],
    "Part 9 - 前沿实践": ["AI Agent", "Agentic Coding", "Computer Use"],
    "附录": ["AI Agent", "学习指南"],
}


def ensure_dirs():
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    ARTICLE_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    SQL_DIR.mkdir(parents=True, exist_ok=True)


def now_text():
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def load_progress():
    if not PROGRESS_PATH.exists():
        return {"source": BOOK_URL, "items": {}, "warnings": []}
    with PROGRESS_PATH.open("r", encoding="utf-8") as f:
        return json.load(f)


def save_progress(progress):
    progress["updated_at"] = now_text()
    with PROGRESS_PATH.open("w", encoding="utf-8") as f:
        json.dump(progress, f, ensure_ascii=False, indent=2)


def human_delay(args):
    if args.delay_max <= 0:
        return
    delay = random.uniform(args.delay_min, args.delay_max)
    time.sleep(delay)


def fetch_url(url, args, binary=False, referer=None):
    request_url = iri_to_uri(url)
    headers = {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        "Cache-Control": "no-cache",
    }
    headers["Accept"] = "*/*" if binary else "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    if referer:
        headers["Referer"] = iri_to_uri(referer)

    last_error = None
    for attempt in range(1, args.retries + 1):
        try:
            req = Request(request_url, headers=headers)
            with urlopen(req, timeout=args.timeout) as resp:
                data = resp.read()
                if binary:
                    return data, resp.headers
                charset = resp.headers.get_content_charset() or "utf-8"
                return data.decode(charset, errors="replace"), resp.headers
        except (HTTPError, URLError, TimeoutError) as exc:
            last_error = exc
            if attempt < args.retries:
                time.sleep(args.retry_delay * attempt + random.uniform(0.5, 1.5))
    raise RuntimeError(f"fetch failed: {url}: {last_error}")


def iri_to_uri(url):
    parsed = urlparse(url)
    path = quote(unquote(parsed.path), safe="/%")
    query = quote(unquote(parsed.query), safe="=&?/%:+,")
    fragment = quote(unquote(parsed.fragment), safe="")
    rebuilt = parsed._replace(path=path, query=query, fragment=fragment)
    return rebuilt.geturl()


def strip_tags(fragment):
    text = re.sub(r"<script\b.*?</script>", "", fragment, flags=re.S | re.I)
    text = re.sub(r"<style\b.*?</style>", "", text, flags=re.S | re.I)
    text = re.sub(r"<[^>]+>", "", text)
    text = html.unescape(text)
    return re.sub(r"\s+", " ", text).strip()


def slugify(value):
    value = unquote(value).strip("/").split("/")[-1]
    value = re.sub(r"[\s_]+", "-", value)
    value = re.sub(r"[^\w\u4e00-\u9fff.-]+", "-", value, flags=re.U)
    value = value.strip("-").lower()
    return value or hashlib.sha1(value.encode("utf-8")).hexdigest()[:12]


def sql_escape(value):
    if value is None:
        return "NULL"
    value = str(value)
    value = value.replace("\\", "\\\\").replace("'", "''")
    value = value.replace("\x00", "")
    return "'" + value + "'"


def sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, int):
        return str(value)
    return sql_escape(value)


class MarkdownParser(HTMLParser):
    def __init__(self, image_map):
        super().__init__(convert_charrefs=True)
        self.image_map = image_map
        self.parts = []
        self.skip_depth = 0
        self.pre_depth = 0
        self.list_stack = []
        self.link_stack = []
        self.table_depth = 0
        self.table_rows = []
        self.current_row = None
        self.current_cell = None

    def append(self, text):
        if self.skip_depth:
            return
        if self.current_cell is not None:
            self.current_cell.append(text)
        else:
            self.parts.append(text)

    def newline(self, count=1):
        self.append("\n" * count)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        classes = attrs.get("class", "")
        if tag in {"script", "style", "svg"}:
            self.skip_depth += 1
            return
        if tag == "a" and "anchor" in classes:
            self.skip_depth += 1
            return
        if self.skip_depth:
            return

        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            level = int(tag[1])
            self.newline(2)
            self.append("#" * level + " ")
        elif tag == "p":
            self.newline(2)
        elif tag == "br":
            self.newline()
        elif tag == "hr":
            self.newline(2)
            self.append("---")
            self.newline(2)
        elif tag == "strong" or tag == "b":
            self.append("**")
        elif tag == "em" or tag == "i":
            self.append("*")
        elif tag == "blockquote":
            self.newline(2)
            self.append("> ")
        elif tag == "pre":
            self.pre_depth += 1
            self.newline(2)
            self.append("```")
            self.newline()
        elif tag == "code" and not self.pre_depth:
            self.append("`")
        elif tag in {"ul", "ol"}:
            self.list_stack.append({"tag": tag, "index": 1})
            self.newline()
        elif tag == "li":
            prefix = "- "
            if self.list_stack and self.list_stack[-1]["tag"] == "ol":
                prefix = f"{self.list_stack[-1]['index']}. "
                self.list_stack[-1]["index"] += 1
            self.newline()
            self.append("  " * max(len(self.list_stack) - 1, 0) + prefix)
        elif tag == "img":
            src = attrs.get("src", "")
            alt = attrs.get("alt", "")
            src = self.image_map.get(src, src)
            if src:
                self.newline(2)
                self.append(f"![{alt}]({src})")
                self.newline(2)
        elif tag == "a":
            href = attrs.get("href", "")
            self.link_stack.append(href)
            self.append("[")
        elif tag == "table":
            self.table_depth += 1
            self.table_rows = []
            self.newline(2)
        elif tag == "tr" and self.table_depth:
            self.current_row = []
        elif tag in {"td", "th"} and self.current_row is not None:
            self.current_cell = []

    def handle_endtag(self, tag):
        if self.skip_depth:
            if tag in {"script", "style", "svg", "a"}:
                self.skip_depth -= 1
            return

        if tag in {"h1", "h2", "h3", "h4", "h5", "h6", "p", "blockquote"}:
            self.newline(2)
        elif tag == "strong" or tag == "b":
            self.append("**")
        elif tag == "em" or tag == "i":
            self.append("*")
        elif tag == "pre":
            self.newline()
            self.append("```")
            self.newline(2)
            self.pre_depth = max(0, self.pre_depth - 1)
        elif tag == "code" and not self.pre_depth:
            self.append("`")
        elif tag in {"ul", "ol"} and self.list_stack:
            self.list_stack.pop()
            self.newline()
        elif tag == "a":
            href = self.link_stack.pop() if self.link_stack else ""
            self.append(f"]({href})" if href else "]")
        elif tag in {"td", "th"} and self.current_row is not None and self.current_cell is not None:
            cell = re.sub(r"\s+", " ", "".join(self.current_cell)).strip()
            self.current_row.append(cell.replace("|", "\\|"))
            self.current_cell = None
        elif tag == "tr" and self.current_row is not None:
            self.table_rows.append(self.current_row)
            self.current_row = None
        elif tag == "table" and self.table_depth:
            self.table_depth -= 1
            if self.table_rows:
                for index, row in enumerate(self.table_rows):
                    self.parts.append("| " + " | ".join(row) + " |\n")
                    if index == 0:
                        self.parts.append("| " + " | ".join(["---"] * len(row)) + " |\n")
            self.newline(2)

    def handle_data(self, data):
        if not self.skip_depth:
            self.append(data)

    def markdown(self):
        text = "".join(self.parts)
        text = html.unescape(text)
        text = re.sub(r"[ \t]+\n", "\n", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        text = re.sub(r"```\n\s*\n", "```\n", text)
        return text.strip() + "\n"


def html_to_markdown(article_html, image_map):
    parser = MarkdownParser(image_map)
    parser.feed(article_html)
    return parser.markdown()


def extract_article_html(page_html):
    match = re.search(r'<article class="prose">(.*?)</article>', page_html, flags=re.S | re.I)
    if not match:
        raise RuntimeError("article.prose not found")
    return match.group(1)


def extract_toc(index_html):
    section_pattern = re.compile(
        r'<div class="sidebar-section">\s*<div class="section-title">(.*?)</div>\s*'
        r'<ul class="chapter-list">(.*?)</ul>\s*</div>',
        flags=re.S | re.I,
    )
    link_pattern = re.compile(r'<a class="chapter-link[^"]*" href="([^"]+)">(.*?)</a>', flags=re.S | re.I)
    items = []
    for section_html, links_html in section_pattern.findall(index_html):
        section = strip_tags(section_html)
        for href, title_html in link_pattern.findall(links_html):
            title = strip_tags(title_html)
            url = urljoin(BOOK_URL, href)
            items.append({"section": section, "title": title, "url": url, "href": href})
    if not items:
        raise RuntimeError("book sidebar chapters not found")
    return items


def extract_images(article_html, page_url):
    urls = []
    for src in re.findall(r'<img\b[^>]*\bsrc="([^"]+)"', article_html, flags=re.I):
        if src.startswith("data:"):
            continue
        urls.append(urljoin(page_url, html.unescape(src)))
    return list(dict.fromkeys(urls))


def image_extension(url, headers):
    path = urlparse(url).path
    ext = os.path.splitext(path)[1].lower()
    if ext in {".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp"}:
        return ext
    content_type = (headers.get("Content-Type") or "").split(";")[0].lower()
    mapping = {
        "image/jpeg": ".jpg",
        "image/png": ".png",
        "image/gif": ".gif",
        "image/svg+xml": ".svg",
        "image/webp": ".webp",
    }
    return mapping.get(content_type, ".img")


def local_upload_url(path):
    rel = path.relative_to(UPLOADS_DIR).as_posix()
    return "/api/uploads/files/" + rel


def download_image(url, page_url, args, warnings):
    digest = hashlib.sha1(url.encode("utf-8")).hexdigest()[:16]
    temp_name = IMAGE_DIR / f"{digest}.download"
    existing = list(IMAGE_DIR.glob(f"{digest}.*"))
    existing = [p for p in existing if p.name != temp_name.name]
    if existing and not args.refresh_images:
        return existing[0]

    data, headers = fetch_url(url, args, binary=True, referer=page_url)
    ext = image_extension(url, headers)
    target = IMAGE_DIR / f"{digest}{ext}"
    with temp_name.open("wb") as f:
        f.write(data)
    temp_name.replace(target)
    print(f"  [image] {url} -> {target.relative_to(BASE_DIR)}")
    return target


def guess_content_type(path):
    mapping = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".svg": "image/svg+xml",
        ".webp": "image/webp",
    }
    return mapping.get(path.suffix.lower(), "application/octet-stream")


class CosUploader:
    def __init__(self, args):
        self.enabled = args.upload_cos
        self.args = args
        self.client = None
        self.bucket = args.cos_bucket or os.environ.get("COS_BUCKET")
        self.region = args.cos_region or os.environ.get("COS_REGION", "ap-shanghai")
        self.domain = (args.cos_domain or os.environ.get("COS_DOMAIN", "")).rstrip("/")
        self.prefix = (args.cos_prefix or os.environ.get("COS_KEY_PREFIX", "myblog/articles/waylandz/")).strip("/")

    def init(self):
        if not self.enabled:
            return
        missing = []
        secret_id = self.args.cos_secret_id or os.environ.get("COS_SECRET_ID")
        secret_key = self.args.cos_secret_key or os.environ.get("COS_SECRET_KEY")
        if not secret_id:
            missing.append("COS_SECRET_ID")
        if not secret_key:
            missing.append("COS_SECRET_KEY")
        if not self.bucket:
            missing.append("COS_BUCKET")
        if missing:
            raise RuntimeError("COS 配置缺失：" + ", ".join(missing))
        try:
            import boto3
            from botocore.client import Config
        except ImportError as exc:
            raise RuntimeError("COS 上传需要 boto3，请先安装 boto3") from exc

        self.client = boto3.session.Session().client(
            "s3",
            region_name=self.region,
            endpoint_url=f"https://cos.{self.region}.myqcloud.com",
            aws_access_key_id=secret_id,
            aws_secret_access_key=secret_key,
            config=Config(signature_version="s3v4", s3={"addressing_style": "virtual"}),
        )

    def public_url(self, key):
        if self.domain:
            return f"{self.domain}/{key}"
        return f"https://{self.bucket}.cos.{self.region}.myqcloud.com/{key}"

    def upload(self, path):
        if not self.enabled:
            return local_upload_url(path)
        key = f"{self.prefix}/{path.name}".replace("//", "/")
        with path.open("rb") as f:
            self.client.put_object(
                Bucket=self.bucket,
                Key=key,
                Body=f,
                ContentType=guess_content_type(path),
            )
        return self.public_url(key)


def parse_article(page_html, page_url, item, image_map):
    article_html = extract_article_html(page_html)
    title_match = re.search(r"<h1\b[^>]*>(.*?)</h1>", article_html, flags=re.S | re.I)
    title = strip_tags(title_match.group(1)) if title_match else item["title"]
    markdown = html_to_markdown(article_html, image_map)
    plain = re.sub(r"!\[[^\]]*]\([^)]+\)", "", markdown)
    plain = re.sub(r"`{3}.*?`{3}", " ", plain, flags=re.S)
    plain = re.sub(r"[#>*`\-|]+", " ", plain)
    plain = re.sub(r"\s+", " ", plain).strip()
    summary = plain[:497] + "..." if len(plain) > 500 else plain
    return {
        "title": title,
        "summary": summary,
        "content": markdown,
        "source_url": page_url,
        "source_title": item["title"],
        "section": item["section"],
    }


def cache_path_for(item):
    return ARTICLE_CACHE_DIR / f"{slugify(item['href'])}.json"


def crawl_articles(items, args, uploader):
    progress = load_progress()
    progress["warnings"] = []
    warnings = progress["warnings"]
    articles = []
    if args.limit:
        items = items[: args.limit]

    uploader.init()

    for index, item in enumerate(items, start=1):
        article_id = args.article_start_id + index - 1
        cpath = cache_path_for(item)
        done = progress["items"].get(item["url"], {}).get("status") == "done"
        if args.resume and done and cpath.exists() and not args.refresh:
            with cpath.open("r", encoding="utf-8") as f:
                cached = json.load(f)
            cached["id"] = article_id
            cached["order"] = index
            articles.append(cached)
            print(f"[skip] {index}/{len(items)} {item['title']}")
            continue

        print(f"[fetch] {index}/{len(items)} {item['url']}")
        try:
            human_delay(args)
            page_html, _ = fetch_url(item["url"], args, referer=BOOK_URL)
            article_html = extract_article_html(page_html)
            image_map = {}

            if not args.no_images:
                for image_url in extract_images(article_html, item["url"]):
                    try:
                        human_delay(args)
                        local_path = download_image(image_url, item["url"], args, warnings)
                        try:
                            uploaded_url = uploader.upload(local_path)
                            image_map[image_url] = uploaded_url
                            image_map[urlparse(image_url).path] = uploaded_url
                        except Exception as exc:
                            warning = f"COS 上传失败：{local_path} ({exc})"
                            warnings.append(warning)
                            print(f"  [warn] {warning}")
                            fallback_url = local_upload_url(local_path)
                            image_map[image_url] = fallback_url
                            image_map[urlparse(image_url).path] = fallback_url
                    except Exception as exc:
                        warning = f"图片下载失败：{image_url} ({exc})"
                        warnings.append(warning)
                        print(f"  [warn] {warning}")

            article = parse_article(page_html, item["url"], item, image_map)
            article.update(
                {
                    "id": article_id,
                    "order": index,
                    "slug": f"{args.slug_prefix}-{slugify(item['href'])}",
                    "cover_url": next(iter(image_map.values()), DEFAULT_COVER),
                    "tags": list(dict.fromkeys(SECTION_TAGS.get(item["section"], ["AI Agent"]))),
                }
            )
            with cpath.open("w", encoding="utf-8") as f:
                json.dump(article, f, ensure_ascii=False, indent=2)
            articles.append(article)
            progress["items"][item["url"]] = {
                "status": "done",
                "title": article["title"],
                "article_id": article_id,
                "cache": str(cpath.relative_to(BASE_DIR)).replace("\\", "/"),
                "fetched_at": now_text(),
            }
            save_progress(progress)
        except Exception as exc:
            progress["items"][item["url"]] = {
                "status": "failed",
                "title": item["title"],
                "article_id": article_id,
                "error": str(exc),
                "fetched_at": now_text(),
            }
            save_progress(progress)
            if not args.keep_going:
                raise
            print(f"  [fail] {item['title']}: {exc}")
    return articles, warnings


def section_title_for(article):
    return article["section"].replace(" - ", ": ")


def published_at_for(args, order):
    start = datetime.strptime(args.published_start, "%Y-%m-%d")
    return (start + timedelta(days=order - 1)).strftime("%Y-%m-%d 00:00:00")


def article_sql_tuple(article, args, generated_at):
    values = [
        article["id"],
        args.author_id,
        article["title"][:256],
        article["summary"][:500],
        article["content"],
        article["cover_url"],
        args.category,
        None,
        0,
        None,
        0,
        article["slug"][:255],
        f"{article['title']} - AI Agent 架构",
        article["summary"][:500],
        0,
        "PUBLISHED",
        random.randint(400, 4200),
        random.randint(30, 480),
        random.randint(10, 120),
        random.randint(5, 35),
        published_at_for(args, article["order"]),
        published_at_for(args, article["order"]),
        generated_at,
        None,
        0,
    ]
    return "(" + ",".join(sql_value(v) for v in values) + ")"


def write_sql(articles, args, warnings):
    generated_at = now_text()
    out = Path(args.output)
    lines = [
        "-- ============================================================",
        f"-- 来源：waylandz.com/ai-agent-book/  共 {len(articles)} 篇  生成时间：{generated_at}",
        "-- 作者ID已统一设置为 1，可通过 --author-id 覆盖",
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE `my_blog`;",
        "",
    ]

    lines.extend(
        [
            "INSERT INTO `blog_category`",
            "(`name`,`group_name`,`description`,`sort_order`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`)",
            "VALUES",
            f"({sql_escape(args.category)},'人工智能','AI Agent 与大模型应用架构',20,1,'NORMAL',{sql_escape(generated_at)},{sql_escape(generated_at)},NULL,0)",
            "ON DUPLICATE KEY UPDATE",
            "`group_name`=VALUES(`group_name`),`description`=VALUES(`description`),`enabled`=1,",
            "`status`='NORMAL',`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;",
            "",
        ]
    )

    tag_names = []
    for article in articles:
        tag_names.extend(article["tags"])
    tag_names = list(dict.fromkeys(tag_names))
    if tag_names:
        lines.append(
            "INSERT INTO `blog_tag` "
            "(`name`,`description`,`group_name`,`use_count`,`enabled`,`status`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES"
        )
        tag_values = []
        for tag in tag_names:
            use_count = sum(1 for article in articles if tag in article["tags"])
            tag_values.append(
                "("
                + ",".join(
                    [
                        sql_escape(tag),
                        sql_escape(f"{tag} 相关文章"),
                        sql_escape("AI Agent"),
                        str(use_count),
                        "1",
                        sql_escape("NORMAL"),
                        sql_escape(generated_at),
                        sql_escape(generated_at),
                        "NULL",
                        "0",
                    ]
                )
                + ")"
            )
        lines.append(",\n".join(tag_values) + "\nON DUPLICATE KEY UPDATE")
        lines.append(
            "`description`=VALUES(`description`),`group_name`=VALUES(`group_name`),"
            "`use_count`=VALUES(`use_count`),`enabled`=1,`status`='NORMAL',"
            "`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;"
        )
        lines.append("")

    lines.append(
        "INSERT INTO `blog_article` "
        "(`id`,`author_id`,`title`,`summary`,`content`,`cover_url`,`category`,`offline_reason`,"
        "`featured`,`featured_at`,`feature_weight`,`slug`,`seo_title`,`seo_description`,`warn_flag`,"
        "`status`,`view_count`,`like_count`,`favorite_count`,`comment_count`,`published_at`,`created_at`,"
        "`updated_at`,`deleted_at`,`version`) VALUES"
    )
    lines.append(",\n".join(article_sql_tuple(article, args, generated_at) for article in articles) + "\nON DUPLICATE KEY UPDATE")
    lines.append(
        "`author_id`=VALUES(`author_id`),`title`=VALUES(`title`),`summary`=VALUES(`summary`),"
        "`content`=VALUES(`content`),`cover_url`=VALUES(`cover_url`),`category`=VALUES(`category`),"
        "`offline_reason`=VALUES(`offline_reason`),`featured`=VALUES(`featured`),"
        "`featured_at`=VALUES(`featured_at`),`feature_weight`=VALUES(`feature_weight`),"
        "`seo_title`=VALUES(`seo_title`),`seo_description`=VALUES(`seo_description`),"
        "`warn_flag`=VALUES(`warn_flag`),`status`=VALUES(`status`),`view_count`=VALUES(`view_count`),"
        "`like_count`=VALUES(`like_count`),`favorite_count`=VALUES(`favorite_count`),"
        "`comment_count`=VALUES(`comment_count`),`published_at`=VALUES(`published_at`),"
        "`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;"
    )
    lines.append("")

    lines.extend(
        [
            "-- 文章标签",
            "UPDATE `blog_article_tag`",
            f"SET `deleted_at`={sql_escape(generated_at)},`updated_at`={sql_escape(generated_at)}",
            "WHERE `article_id` BETWEEN "
            f"{args.article_start_id} AND {args.article_start_id + len(articles) - 1};",
            "INSERT INTO `blog_article_tag` (`article_id`,`tag_name`,`created_at`,`updated_at`,`deleted_at`,`version`) VALUES",
        ]
    )
    tag_values = []
    for article in articles:
        for tag in article["tags"]:
            tag_values.append(
                "("
                + ",".join([str(article["id"]), sql_escape(tag), sql_escape(generated_at), sql_escape(generated_at), "NULL", "0"])
                + ")"
            )
    lines.append(",\n".join(tag_values) + "\nON DUPLICATE KEY UPDATE")
    lines.append("`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;")
    lines.append("")

    estimated_minutes = max(1, sum(max(1, len(a["content"]) // 450) for a in articles))
    lines.extend(
        [
            "-- ============================================================",
            "-- 书籍聚合：AI Agent 架构实战",
            "-- ============================================================",
            f"SET @ai_agent_book_column_id = {args.column_id};",
            f"SET @ai_agent_book_topic_id = {args.topic_id};",
            f"SET @ai_agent_book_created_at = {sql_escape(generated_at)};",
            "",
            "INSERT INTO `blog_column`",
            "(`id`,`author_id`,`title`,`summary`,`cover_url`,`status`,`sort_order`,`subscriber_count`,`article_count`,`intro`,",
            " `difficulty`,`estimated_minutes`,`source_type`,`source_note`,`recommended`,`recommend_weight`,",
            " `created_at`,`updated_at`,`deleted_at`,`version`)",
            "VALUES",
            "("
            + ",".join(
                [
                    "@ai_agent_book_column_id",
                    str(args.author_id),
                    sql_escape("AI Agent 架构实战"),
                    sql_escape(f"《AI Agent 架构实战》书籍专栏，共 {len(articles)} 篇，系统讲解从 ReAct、工具调用、多 Agent 编排到企业级生产架构。"),
                    sql_escape(DEFAULT_COVER),
                    sql_escape("PUBLISHED"),
                    "12",
                    "0",
                    str(len(articles)),
                    sql_escape("这是一套面向生产级 AI Agent 系统的连续阅读路径，覆盖 Agent 基础、工具与扩展、上下文与记忆、单 Agent 模式、多 Agent 编排、高级推理、生产架构、企业级特性和前沿实践。"),
                    sql_escape("ADVANCED"),
                    str(estimated_minutes),
                    sql_escape("CURATED"),
                    sql_escape(SOURCE_NOTE),
                    "1",
                    "990",
                    "@ai_agent_book_created_at",
                    "@ai_agent_book_created_at",
                    "NULL",
                    "0",
                ]
            )
            + ")",
            "ON DUPLICATE KEY UPDATE",
            "`author_id`=VALUES(`author_id`),`title`=VALUES(`title`),`summary`=VALUES(`summary`),",
            "`cover_url`=VALUES(`cover_url`),`status`=VALUES(`status`),`sort_order`=VALUES(`sort_order`),",
            "`article_count`=VALUES(`article_count`),`intro`=VALUES(`intro`),`difficulty`=VALUES(`difficulty`),",
            "`estimated_minutes`=VALUES(`estimated_minutes`),`source_type`=VALUES(`source_type`),",
            "`source_note`=VALUES(`source_note`),`recommended`=VALUES(`recommended`),",
            "`recommend_weight`=VALUES(`recommend_weight`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;",
            "",
            "-- This import is a book-like, ordered reading path. Keep it as a column only.",
            "-- If an older run created a duplicate topic, soft-delete it and its relations.",
            "UPDATE `blog_topic`",
            "SET `status`='OFFLINE',",
            "    `recommended`=0,",
            "    `deleted_at`=COALESCE(`deleted_at`, @ai_agent_book_created_at),",
            "    `updated_at`=@ai_agent_book_created_at",
            "WHERE `id`=@ai_agent_book_topic_id;",
            "",
            "UPDATE `blog_column_article`",
            "SET `deleted_at`=@ai_agent_book_created_at,`updated_at`=@ai_agent_book_created_at",
            "WHERE `column_id`=@ai_agent_book_column_id;",
            "",
            "UPDATE `blog_topic_article`",
            "SET `deleted_at`=@ai_agent_book_created_at,`updated_at`=@ai_agent_book_created_at",
            "WHERE `topic_id`=@ai_agent_book_topic_id;",
            "",
            "INSERT INTO `blog_column_article`",
            "(`column_id`,`article_id`,`sort_order`,`section_title`,`step_order`,`required`,`editor_note`,",
            " `created_at`,`updated_at`,`deleted_at`,`version`)",
            "VALUES",
        ]
    )
    relation_values = []
    for article in articles:
        relation_values.append(
            "("
            + ",".join(
                [
                    "@ai_agent_book_column_id",
                    str(article["id"]),
                    str(article["order"] * 10),
                    sql_escape(section_title_for(article)),
                    str(article["order"]),
                    "1",
                    sql_escape("AI Agent 架构实战"),
                    "@ai_agent_book_created_at",
                    "@ai_agent_book_created_at",
                    "NULL",
                    "0",
                ]
            )
            + ")"
        )
    lines.append(",\n".join(relation_values) + "\nON DUPLICATE KEY UPDATE")
    lines.append(
        "`sort_order`=VALUES(`sort_order`),`section_title`=VALUES(`section_title`),"
        "`step_order`=VALUES(`step_order`),`required`=VALUES(`required`),"
        "`editor_note`=VALUES(`editor_note`),`updated_at`=VALUES(`updated_at`),`deleted_at`=NULL;"
    )

    if warnings:
        lines.append("")
        lines.append("-- WARNINGS")
        for warning in warnings:
            lines.append("-- " + warning.replace("\n", " "))

    if args.dry_run:
        print(f"[dry-run] SQL will be written to {out}")
        print("\n".join(lines[:40]))
        return
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print(f"[sql] {out}")


def parse_args():
    parser = argparse.ArgumentParser(description="爬取 Waylandz AI Agent 电子书专栏并生成导入 SQL")
    parser.add_argument("--book-url", default=BOOK_URL, help="目录页 URL")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="输出 SQL 文件")
    parser.add_argument("--author-id", type=int, default=1, help="文章和专栏作者 ID，默认 1")
    parser.add_argument("--category", default="AI Agent", help="文章分类名")
    parser.add_argument("--article-start-id", type=int, default=950001, help="文章起始 ID")
    parser.add_argument("--column-id", type=int, default=9600020, help="专栏 ID")
    parser.add_argument("--topic-id", type=int, default=9700020, help="旧专题 ID，脚本会软删除")
    parser.add_argument("--slug-prefix", default="ai-agent-book", help="文章 slug 前缀")
    parser.add_argument("--published-start", default="2026-01-01", help="第一篇发布时间，格式 YYYY-MM-DD")
    parser.add_argument("--limit", type=int, default=0, help="只抓取前 N 篇，调试用")
    parser.add_argument("--resume", action="store_true", default=True, help="复用已完成缓存")
    parser.add_argument("--refresh", action="store_true", help="忽略文章缓存，重新抓取")
    parser.add_argument("--no-images", action="store_true", help="不下载正文图片")
    parser.add_argument("--refresh-images", action="store_true", help="重新下载已存在图片")
    parser.add_argument("--upload-cos", action="store_true", help="上传图片到腾讯云 COS，并在 SQL 中使用 COS URL")
    parser.add_argument("--cos-secret-id", default="", help="COS SecretId，默认读取 COS_SECRET_ID")
    parser.add_argument("--cos-secret-key", default="", help="COS SecretKey，默认读取 COS_SECRET_KEY")
    parser.add_argument("--cos-region", default="", help="COS region，默认读取 COS_REGION 或 ap-shanghai")
    parser.add_argument("--cos-bucket", default="", help="COS bucket，默认读取 COS_BUCKET")
    parser.add_argument("--cos-domain", default="", help="COS/CDN 自定义域名，默认读取 COS_DOMAIN")
    parser.add_argument("--cos-prefix", default="", help="COS key 前缀，默认 myblog/articles/waylandz/")
    parser.add_argument("--delay-min", type=float, default=2.5, help="请求之间最小随机等待秒数")
    parser.add_argument("--delay-max", type=float, default=7.0, help="请求之间最大随机等待秒数")
    parser.add_argument("--timeout", type=int, default=25, help="单次请求超时时间")
    parser.add_argument("--retries", type=int, default=3, help="失败重试次数")
    parser.add_argument("--retry-delay", type=float, default=2.0, help="失败重试基础等待秒数")
    parser.add_argument("--keep-going", action="store_true", help="单篇失败后继续抓取")
    parser.add_argument("--dry-run", action="store_true", help="不写 SQL，只打印预览")
    return parser.parse_args()


def main():
    args = parse_args()
    global BOOK_URL
    BOOK_URL = args.book_url
    if args.delay_min > args.delay_max:
        raise SystemExit("--delay-min 不能大于 --delay-max")

    ensure_dirs()
    print(f"[toc] {BOOK_URL}")
    index_html, _ = fetch_url(BOOK_URL, args)
    items = extract_toc(index_html)
    print(f"[toc] found {len(items)} chapters")

    uploader = CosUploader(args)
    try:
        articles, warnings = crawl_articles(items, args, uploader)
        if not articles:
            raise RuntimeError("no articles crawled")
        write_sql(articles, args, warnings)
    except Exception as exc:
        print(f"[error] {exc}", file=sys.stderr)
        return 1

    if warnings:
        print("\n[warn] 本次存在需要处理的警告：")
        for warning in warnings:
            print(f"  - {warning}")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
