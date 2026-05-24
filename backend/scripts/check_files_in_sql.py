"""
检查 uploads/pdai 和 uploads/runoob 目录下的文件
是否都在 src/main/resources/db/articles/ 的 SQL 文件中被引用。
"""
import os
import re

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UPLOADS_DIR = os.path.join(BASE_DIR, "uploads")
SQL_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "db", "articles")

def scan_files(root_dir):
    files = {}
    for dirpath, _, filenames in os.walk(root_dir):
        for f in filenames:
            rel_path = os.path.relpath(os.path.join(dirpath, f), root_dir).replace("\\", "/")
            files[rel_path] = os.path.join(dirpath, f)
    return files

def extract_sql_references(sql_dir):
    references = set()
    refs_in_file = {}
    for fname in os.listdir(sql_dir):
        if not fname.endswith(".sql"):
            continue
        fpath = os.path.join(sql_dir, fname)
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()
        found = re.findall(r"/api/uploads/files/([^\s)\"'\]]+)", content)
        refs_in_file[fname] = list(found)
        for ref in found:
            # Normalize backslash to forward slash
            references.add(ref)
    return references, refs_in_file

def main():
    pdai_files = scan_files(os.path.join(UPLOADS_DIR, "pdai"))
    runoob_files = scan_files(os.path.join(UPLOADS_DIR, "runoob"))
    sql_refs, refs_in_file = extract_sql_references(SQL_DIR)

    all_disk_files = {}
    for rel_path, abs_path in pdai_files.items():
        key = f"pdai/{rel_path}"
        all_disk_files[key] = abs_path
    for rel_path, abs_path in runoob_files.items():
        key = f"runoob/{rel_path}"
        all_disk_files[key] = abs_path

    print("=" * 70)
    print("检查磁盘文件是否在 SQL 中被引用")
    print("=" * 70)
    unreferenced = []
    for key in sorted(all_disk_files.keys()):
        if key not in sql_refs:
            unreferenced.append(key)
            print(f"  [缺失] SQL 中未引用: {key}")

    if not unreferenced:
        print("  [OK] 所有磁盘文件均在 SQL 中有引用")
    else:
        print(f"\n  共 {len(unreferenced)} 个文件未在 SQL 中引用")

    print("\n" + "=" * 70)
    print("检查 SQL 引用是否有对应的磁盘文件")
    print("=" * 70)
    missing_files = []
    for ref in sorted(sql_refs):
        if ref not in all_disk_files and not ref.startswith("pdai/") and not ref.startswith("runoob/"):
            continue  # skip non-pdai/runoob references
        if ref not in all_disk_files:
            missing_files.append(ref)
            print(f"  [缺失] 磁盘上缺少文件: {ref}")

    if not missing_files:
        print("  [OK] 所有 SQL 中的 pdai/runoob 引用均有对应磁盘文件")
    else:
        print(f"\n  共 {len(missing_files)} 个引用缺少磁盘文件")

    # Print file counts per SQL file
    print("\n" + "=" * 70)
    print("各 SQL 文件中 pdai/runoob 引用统计")
    print("=" * 70)
    for fname, refs in refs_in_file.items():
        relevant = [r for r in refs if r.startswith("pdai/") or r.startswith("runoob/")]
        if relevant:
            print(f"  {fname}: {len(relevant)} 个引用")

    print("\n" + "=" * 70)
    print(f"总览: 磁盘文件 {len(all_disk_files)} 个, SQL 引用(pdai/runoob) {len([r for r in sql_refs if r.startswith('pdai/') or r.startswith('runoob/')])} 个")
    print("=" * 70)

if __name__ == "__main__":
    main()
