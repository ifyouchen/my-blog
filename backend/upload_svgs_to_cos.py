#!/usr/bin/env python3
"""
Upload SVG/WebP images from uploads/waylandz/ai-agent-book/ to Tencent COS,
then replace local paths in articles_ai_agent.sql with COS URLs,
outputting a new articles_ai_agent2.sql.
"""

import os
import re
import sys
import boto3
from botocore.client import Config

# ── COS config (matches application.yml) ──────────────────────────
COS_SECRET_ID = os.environ.get("COS_SECRET_ID", "AKID9B0bu27xJU7XjgPtgHdllX5PUbjlcN3X")
COS_SECRET_KEY = os.environ.get("COS_SECRET_KEY", "qTr1wc3OVN1VkuqJadR2SM4XkjMlwsYz")
COS_REGION = os.environ.get("COS_REGION", "ap-shanghai")
COS_BUCKET = os.environ.get("COS_BUCKET", "myblog-1322876242")
COS_DOMAIN = os.environ.get("COS_DOMAIN", "")

# ── Paths ─────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOADS_DIR = os.path.join(SCRIPT_DIR, "uploads", "waylandz", "ai-agent-book")
SQL_INPUT = os.path.join(SCRIPT_DIR, "src", "main", "resources", "db", "articles", "articles_ai_agent.sql")
SQL_OUTPUT = os.path.join(SCRIPT_DIR, "src", "main", "resources", "db", "articles", "articles_ai_agent2.sql")

# COS key prefix – matches the existing /api/uploads/files/waylandz/ path structure
COS_KEY_PREFIX = "waylandz/ai-agent-book/"

# Local path pattern to match in SQL content
# Matches: /api/uploads/files/waylandz/ai-agent-book/xxxxx.svg  or  xxxx.webp
LOCAL_PATH_PATTERN = re.compile(r"/api/uploads/files/waylandz/ai-agent-book/([a-f0-9]+\.\w+)")


def build_cos_url(key):
    if COS_DOMAIN:
        domain = COS_DOMAIN.rstrip("/")
        return f"{domain}/{key}"
    return f"https://{COS_BUCKET}.cos.{COS_REGION}.myqcloud.com/{key}"


def create_s3_client():
    endpoint = f"https://cos.{COS_REGION}.myqcloud.com"
    return boto3.client(
        "s3",
        aws_access_key_id=COS_SECRET_ID,
        aws_secret_access_key=COS_SECRET_KEY,
        endpoint_url=endpoint,
        region_name=COS_REGION,
        config=Config(signature_version="s3", s3={"addressing_style": "virtual"}),
    )


def collect_local_files():
    """Return dict: filename -> full local path"""
    files = {}
    if not os.path.isdir(UPLOADS_DIR):
        print(f"ERROR: uploads directory not found: {UPLOADS_DIR}")
        sys.exit(1)
    for fname in os.listdir(UPLOADS_DIR):
        fpath = os.path.join(UPLOADS_DIR, fname)
        if os.path.isfile(fpath) and (fname.endswith(".svg") or fname.endswith(".webp")):
            files[fname] = fpath
    return files


def upload_to_cos(s3_client, filename, local_path):
    """Upload one file to COS, return the COS URL."""
    key = COS_KEY_PREFIX + filename
    ext = os.path.splitext(filename)[1].lower()
    content_type = "image/svg+xml" if ext == ".svg" else "image/webp"
    file_size = os.path.getsize(local_path)

    with open(local_path, "rb") as f:
        s3_client.put_object(
            Bucket=COS_BUCKET,
            Key=key,
            Body=f,
            ContentType=content_type,
            ContentLength=file_size,
        )
    url = build_cos_url(key)
    print(f"  uploaded: {filename} -> {url}")
    return url


def replace_paths_in_sql(sql_text, filename_to_url):
    """Replace all local upload paths in SQL with COS URLs."""
    def replacer(m):
        fname = m.group(1)
        if fname in filename_to_url:
            return filename_to_url[fname]
        print(f"  WARNING: no COS URL for {fname}, keeping original")
        return m.group(0)

    return LOCAL_PATH_PATTERN.sub(replacer, sql_text)


def main():
    print("=" * 60)
    print("Upload SVGs to COS & generate articles_ai_agent2.sql")
    print("=" * 60)

    # 1. Collect local files
    local_files = collect_local_files()
    print(f"\nFound {len(local_files)} image files in {UPLOADS_DIR}")

    # 2. Scan SQL to find which files are actually referenced
    if not os.path.isfile(SQL_INPUT):
        print(f"ERROR: SQL file not found: {SQL_INPUT}")
        sys.exit(1)
    with open(SQL_INPUT, "r", encoding="utf-8") as f:
        sql_text = f.read()

    referenced = set(LOCAL_PATH_PATTERN.findall(sql_text))
    print(f"Found {len(referenced)} referenced images in SQL")

    # Only upload files that are referenced in the SQL
    to_upload = {fname: path for fname, path in local_files.items() if fname in referenced}
    not_found = referenced - set(local_files.keys())
    if not_found:
        print(f"WARNING: {len(not_found)} referenced images not found locally:")
        for f in sorted(not_found):
            print(f"  - {f}")

    print(f"\nWill upload {len(to_upload)} files to COS")

    # 3. Upload to COS
    s3_client = create_s3_client()
    filename_to_url = {}
    for fname in sorted(to_upload.keys()):
        local_path = to_upload[fname]
        try:
            url = upload_to_cos(s3_client, fname, local_path)
            filename_to_url[fname] = url
        except Exception as e:
            print(f"  FAILED: {fname} -> {e}")
            sys.exit(1)

    print(f"\nSuccessfully uploaded {len(filename_to_url)} files")

    # 4. Replace paths in SQL
    new_sql = replace_paths_in_sql(sql_text, filename_to_url)

    # 5. Write new SQL file
    with open(SQL_OUTPUT, "w", encoding="utf-8") as f:
        f.write(new_sql)
    print(f"\nNew SQL written to: {SQL_OUTPUT}")

    # 6. Summary
    replaced_count = len(LOCAL_PATH_PATTERN.findall(sql_text))
    remaining = len(LOCAL_PATH_PATTERN.findall(new_sql))
    print(f"Paths replaced: {replaced_count - remaining} / {replaced_count}")
    if remaining > 0:
        print(f"WARNING: {remaining} paths could not be replaced!")
    else:
        print("All local paths replaced with COS URLs!")


if __name__ == "__main__":
    main()
