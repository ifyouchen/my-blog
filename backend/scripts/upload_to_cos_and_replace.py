"""
将 uploads/pdai 和 uploads/runoob 下的文件上传到腾讯云 COS，
并将 SQL 文件中的 /api/uploads/files/... 路径替换为 COS URL。

用法:
   python scripts/upload_to_cos_and_replace.py                    # 使用默认 COS 配置
   python scripts/upload_to_cos_and_replace.py --dry-run          # 预览不实际执行
   python scripts/upload_to_cos_and_replace.py --domain https://cdn.example.com  # 指定自定义域名
"""
import os
import re
import sys
import argparse
import shutil
from datetime import datetime

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UPLOADS_DIR = os.path.join(BASE_DIR, "uploads")
SQL_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "db", "articles")
BACKUP_DIR = os.path.join(BASE_DIR, "scripts", "sql_backup", datetime.now().strftime("%Y%m%d_%H%M%S"))

# COS key prefix: myblog/articles/{year}/{month}/{day}/
now = datetime.now()
COS_KEY_PREFIX = f"myblog/articles/{now.year}/{now.month:02d}/{now.day:02d}/"

# Default COS config (from application.yml)
COS_SECRET_ID = os.environ.get("COS_SECRET_ID", "AKID9B0bu27xJU7XjgPtgHdllX5PUbjlcN3X")
COS_SECRET_KEY = os.environ.get("COS_SECRET_KEY", "qTr1wc3OVN1VkuqJadR2SM4XkjMlwsYz")
COS_REGION = os.environ.get("COS_REGION", "ap-shanghai")
COS_BUCKET = os.environ.get("COS_BUCKET", "myblog-1322876242")
COS_DOMAIN = os.environ.get("COS_DOMAIN", "")


def get_cos_url(key):
    """Get the COS public URL for a given key."""
    if COS_DOMAIN:
        domain = COS_DOMAIN.rstrip("/")
        return f"{domain}/{key}"
    return f"https://{COS_BUCKET}.cos.{COS_REGION}.myqcloud.com/{key}"


def init_cos_client():
    """Initialize and return the COS S3 client."""
    try:
        import boto3
        from botocore.client import Config
    except ImportError:
        print("错误: 请先安装 boto3: pip install boto3")
        sys.exit(1)

    client = boto3.session.Session().client(
        "s3",
        region_name=COS_REGION,
        endpoint_url=f"https://cos.{COS_REGION}.myqcloud.com",
        aws_access_key_id=COS_SECRET_ID,
        aws_secret_access_key=COS_SECRET_KEY,
        config=Config(signature_version="s3v4", s3={"addressing_style": "virtual"}),
    )
    # Verify connectivity
    try:
        client.list_buckets()
    except Exception as e:
        print(f"错误: COS 连接失败: {e}")
        sys.exit(1)
    return client


def collect_files():
    """Collect all files under pdai/ and runoob/ directories."""
    files = {}
    for source_dir in ["pdai", "runoob"]:
        src_path = os.path.join(UPLOADS_DIR, source_dir)
        if not os.path.isdir(src_path):
            continue
        for dirpath, _, filenames in os.walk(src_path):
            for fname in filenames:
                rel_path = os.path.relpath(os.path.join(dirpath, fname), UPLOADS_DIR).replace("\\", "/")
                files[rel_path] = os.path.join(dirpath, fname)
    return files


def upload_file(client, key, filepath, dry_run=False):
    """Upload a single file to COS, return the URL."""
    full_key = COS_KEY_PREFIX + key
    if dry_run:
        url = get_cos_url(full_key)
        print(f"  [模拟] 上传: {full_key} -> {url}")
        return url

    content_type = guess_content_type(filepath)
    with open(filepath, "rb") as f:
        client.put_object(Bucket=COS_BUCKET, Key=full_key, Body=f, ContentType=content_type)
    url = get_cos_url(full_key)
    print(f"  [上传] {full_key} -> {url}")
    return url


def guess_content_type(filepath):
    ext = os.path.splitext(filepath)[1].lower()
    mapping = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".svg": "image/svg+xml",
        ".webp": "image/webp",
    }
    return mapping.get(ext, "application/octet-stream")


def replace_in_sql_files(url_map, dry_run=False):
    """Replace /api/uploads/files/... paths with COS URLs in all SQL files."""
    if not dry_run:
        os.makedirs(BACKUP_DIR, exist_ok=True)

    pattern = re.compile(r'/api/uploads/files/([^\s)"\']+)')

    for fname in os.listdir(SQL_DIR):
        if not fname.endswith(".sql"):
            continue
        fpath = os.path.join(SQL_DIR, fname)
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()

        def replace_match(m):
            rel_key = m.group(1)
            cos_url = url_map.get(rel_key)
            if cos_url:
                return cos_url
            return m.group(0)

        new_content = pattern.sub(replace_match, content)

        if new_content != content:
            if not dry_run:
                shutil.copy2(fpath, os.path.join(BACKUP_DIR, fname))
                with open(fpath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"  [替换] {fname} (备份至 {BACKUP_DIR})")
            else:
                # Count replacements
                old_count = len(pattern.findall(content))
                new_count = len(pattern.findall(new_content))
                replaced = old_count - new_count
                print(f"  [模拟] {fname}: 将替换 {replaced} 个路径")
        else:
            print(f"  [跳过] {fname}: 无匹配路径")


def main():
    parser = argparse.ArgumentParser(description="上传文件到 COS 并替换 SQL 中的路径")
    parser.add_argument("--dry-run", action="store_true", help="仅预览，不实际执行上传和替换")
    parser.add_argument("--domain", default="", help="COS 自定义域名（CDN）")
    args = parser.parse_args()

    global COS_DOMAIN
    if args.domain:
        COS_DOMAIN = args.domain

    print("=" * 70)
    print(f"COS 配置: bucket={COS_BUCKET}, region={COS_REGION}")
    if COS_DOMAIN:
        print(f"自定义域名: {COS_DOMAIN}")
    else:
        print(f"默认域名: https://{COS_BUCKET}.cos.{COS_REGION}.myqcloud.com")
    print(f"模式: {'模拟 (dry-run)' if args.dry_run else '实际执行'}")
    print("=" * 70)

    # Collect files
    files = collect_files()
    if not files:
        print("错误: uploads/pdai 和 uploads/runoob 下未找到文件")
        sys.exit(1)
    print(f"\n共发现 {len(files)} 个文件待上传\n")

    # Init COS client (skip for dry-run to avoid unnecessary calls)
    client = None if args.dry_run else init_cos_client()

    # Upload all files
    url_map = {}
    for rel_key in sorted(files.keys()):
        url = upload_file(client, rel_key, files[rel_key], dry_run=args.dry_run)
        url_map[rel_key] = url

    # Replace in SQL files
    print("\n" + "=" * 70)
    print("替换 SQL 文件中的路径")
    print("=" * 70)
    replace_in_sql_files(url_map, dry_run=args.dry_run)

    print("\n" + "=" * 70)
    if args.dry_run:
        print("模拟完成。移除 --dry-run 参数以实际执行。")
    else:
        print(f"完成! SQL 备份目录: {BACKUP_DIR}")
    print("=" * 70)


if __name__ == "__main__":
    main()
