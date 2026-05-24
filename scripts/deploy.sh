#!/usr/bin/env bash
set -euo pipefail

HOST_NAME="43.155.132.161"
USER_NAME="root"
PORT="22"
IDENTITY_FILE=""
SKIP_TESTS="false"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
IDENTITY_FILE="$SCRIPT_DIR/shouer.pem"
FRONTEND_DIR="$REPO_ROOT/frontend"
BACKEND_DIR="$REPO_ROOT/backend"
FRONTEND_DIST="$FRONTEND_DIR/dist"
JAR_NAME="my-blog-backend-0.1.0.jar"
BACKEND_JAR="$BACKEND_DIR/target/$JAR_NAME"

# 本地压缩包
LOCAL_DIST_ZIP="$FRONTEND_DIR/dist.tar.gz"

REMOTE_ROOT="/www/wwwroot"
REMOTE_BASE="$REMOTE_ROOT/my-blog"
REMOTE_FRONTEND_PARENT="$REMOTE_BASE/frontend"
REMOTE_FRONTEND_DIST="$REMOTE_FRONTEND_PARENT/dist"
REMOTE_JAR="$REMOTE_ROOT/$JAR_NAME"
REMOTE_ZIP="$REMOTE_FRONTEND_PARENT/dist.tar.gz"

usage() {
    cat <<EOF
Usage:
  bash scripts/deploy.sh --host <server-ip> [options]

Options:
  --host <server-ip>       Cloud server IP or hostname. Default: $HOST_NAME.
  --user <user>            SSH user. Default: root.
  --port <port>            SSH port. Default: 22.
  --identity-file <path>   SSH private key path. Default: $IDENTITY_FILE.
  --skip-tests             Run mvn package -DskipTests.
  -h, --help               Show help.

Examples:
  bash scripts/deploy.sh --host 1.2.3.4 --user root
  bash scripts/deploy.sh --host 1.2.3.4 --user root --port 2222
  bash scripts/deploy.sh --host 1.2.3.4 --user root --identity-file ~/.ssh/id_rsa
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --host)
            HOST_NAME="${2:-}"
            shift 2
            ;;
        --user)
            USER_NAME="${2:-}"
            shift 2
            ;;
        --port)
            PORT="${2:-}"
            shift 2
            ;;
        --identity-file)
            IDENTITY_FILE="${2:-}"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS="true"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if [[ -z "$HOST_NAME" ]]; then
    echo "--host is required." >&2
    usage
    exit 1
fi

TARGET="$USER_NAME@$HOST_NAME"
SSH_ARGS=()
SSH_BATCH_ARGS=()
SCP_ARGS=()
SCP_QUIET_ARGS=()

build_ssh_args() {
    SSH_ARGS=()

    if [[ -n "$IDENTITY_FILE" ]]; then
        SSH_ARGS+=("-i" "$IDENTITY_FILE")
    fi

    SSH_ARGS+=("-p" "$PORT" "$TARGET")
}

build_ssh_batch_args() {
    SSH_BATCH_ARGS=()

    if [[ -n "$IDENTITY_FILE" ]]; then
        SSH_BATCH_ARGS+=("-i" "$IDENTITY_FILE")
    fi

    SSH_BATCH_ARGS+=("-o" "BatchMode=yes" "-p" "$PORT" "$TARGET")
}

build_scp_args() {
    SCP_ARGS=()

    if [[ -n "$IDENTITY_FILE" ]]; then
        SCP_ARGS+=("-i" "$IDENTITY_FILE")
    fi

    SCP_ARGS+=("-P" "$PORT")
}

build_scp_quiet_args() {
    SCP_QUIET_ARGS=()

    if [[ -n "$IDENTITY_FILE" ]]; then
        SCP_QUIET_ARGS+=("-i" "$IDENTITY_FILE")
    fi

    SCP_QUIET_ARGS+=("-q" "-P" "$PORT")
}

shell_quote() {
    printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

format_bytes() {
    local bytes="$1"

    if [[ "$bytes" -ge 1073741824 ]]; then
        awk -v size="$bytes" 'BEGIN { printf "%.2f GB", size / 1073741824 }'
    elif [[ "$bytes" -ge 1048576 ]]; then
        awk -v size="$bytes" 'BEGIN { printf "%.2f MB", size / 1048576 }'
    elif [[ "$bytes" -ge 1024 ]]; then
        awk -v size="$bytes" 'BEGIN { printf "%.2f KB", size / 1024 }'
    else
        printf "%s B" "$bytes"
    fi
}

local_size_bytes() {
    local path="$1"
    if [[ -f "$path" ]]; then
        stat -f%z "$path" 2>/dev/null || stat -c%s "$path"
        return
    fi
    find "$path" -type f -print0 | xargs -0 stat -c%s | awk '{s+=$1} END {print s+0}'
}

remote_size_bytes() {
    local remote_path="$1"
    local quoted_path
    local command
    local size

    quoted_path="$(shell_quote "$remote_path")"
    command="if [ -e $quoted_path ]; then du -sb $quoted_path 2>/dev/null | cut -f1; else printf 0; fi"

    if ! size="$(ssh "${SSH_BATCH_ARGS[@]}" "$command" 2>/dev/null)"; then
        return 1
    fi

    [[ "$size" =~ ^[0-9]+$ ]] || return 1
    printf '%s' "$size"
}

can_probe_upload_progress() {
    local output
    output="$(ssh "${SSH_BATCH_ARGS[@]}" "printf 1" 2>/dev/null || true)"
    [[ "$output" == "1" ]]
}

run_step() {
    local name="$1"
    shift

    echo ""
    echo "==> $name"
    "$@"
}

print_progress() {
    local step="$1"
    local uploaded="$2"
    local total="$3"
    local percent=0
    local width=30
    local filled=0
    local empty=0
    local bar=""
    local i

    if [[ "$total" -gt 0 ]]; then
        percent=$(( uploaded * 100 / total ))
    fi

    if [[ "$percent" -gt 99 ]]; then
        percent=99
    fi

    filled=$(( percent * width / 100 ))
    empty=$(( width - filled ))

    for ((i = 0; i < filled; i++)); do
        bar="${bar}#"
    done

    for ((i = 0; i < empty; i++)); do
        bar="${bar}-"
    done

    printf '\r%s [%s] %3d%% %s / %s' \
        "$step" \
        "$bar" \
        "$percent" \
        "$(format_bytes "$uploaded")" \
        "$(format_bytes "$total_bytes")"
}

upload_with_progress() {
    local step="$1"
    local local_path="$2"
    local remote_target="$3"
    local remote_progress_path="$4"

    echo ""
    echo "==> $step"

    if [[ "$CAN_PROBE_UPLOAD_PROGRESS" != "true" ]]; then
        scp "${SCP_ARGS[@]}" "$local_path" "$remote_target"
        return
    fi

    local total_bytes
    total_bytes="$(local_size_bytes "$local_path")"
    scp "${SCP_QUIET_ARGS[@]}" "$local_path" "$remote_target" &
    local scp_pid="$!"

    while kill -0 "$scp_pid" 2>/dev/null; do
        local remote_bytes
        remote_bytes="$(remote_size_bytes "$remote_progress_path" || echo 0)"
        if [[ "$remote_bytes" -gt "$total_bytes" ]]; then
            remote_bytes=$total_bytes
        fi
        print_progress "$step" "$remote_bytes" "$total_bytes"
        sleep 0.8
    done

    wait "$scp_pid"
    print_progress "$step" "$total_bytes" "$total_bytes"
    printf '\n'
}

build_frontend() {
    cd "$FRONTEND_DIR"
    npm run build
}

package_backend() {
    cd "$BACKEND_DIR"
    if [[ "$SKIP_TESTS" == "true" ]]; then
        mvn package -DskipTests
    else
        mvn package
    fi
}

# ====================== ✅ 核心：压缩 dist ======================
compress_frontend() {
    cd "$FRONTEND_DIR"
    rm -f "$LOCAL_DIST_ZIP"
    tar -zcf "$LOCAL_DIST_ZIP" -C dist .
}
# ==============================================================

prepare_remote_directories() {
    ssh "${SSH_ARGS[@]}" "set -e; mkdir -p '$REMOTE_FRONTEND_PARENT'; rm -rf '$REMOTE_FRONTEND_DIST' '$REMOTE_ZIP'"
}

# ====================== ✅ 服务器解压 ======================
extract_remote_zip() {
    echo ""
    echo "==> Extract frontend on server"
    ssh "${SSH_ARGS[@]}" "set -e;
      mkdir -p '$REMOTE_FRONTEND_DIST';
      tar -zxf '$REMOTE_ZIP' -C '$REMOTE_FRONTEND_DIST';
      rm -f '$REMOTE_ZIP';"
}
# ============================================================

restart_remote_services() {
    ssh "${SSH_ARGS[@]}" "set -e; cd '$REMOTE_ROOT'; chmod +x './restart.sh'; ./restart.sh${INIT_SQL:+ $INIT_SQL}"
}

build_ssh_args
build_ssh_batch_args
build_scp_args
build_scp_quiet_args

INIT_SQL=""
read -p "是否初始化 SQL 数据？(输入 yes 确认，直接回车跳过): " init_choice
if [ "${init_choice,,}" = "yes" ]; then
    INIT_SQL="init"
    echo "SQL 初始化已开启"
fi

run_step "Build frontend" build_frontend
run_step "Compress frontend dist" compress_frontend
run_step "Package backend" package_backend

if [[ ! -d "$FRONTEND_DIST" ]]; then
    echo "Frontend dist missing: $FRONTEND_DIST" >&2
    exit 1
fi

if [[ ! -f "$BACKEND_JAR" ]]; then
    echo "Backend jar missing: $BACKEND_JAR" >&2
    exit 1
fi

CAN_PROBE_UPLOAD_PROGRESS="false"
can_probe_upload_progress && CAN_PROBE_UPLOAD_PROGRESS="true"

run_step "Prepare remote directories" prepare_remote_directories

# ====================== ✅ 上传压缩包 ======================
upload_with_progress \
    "Upload frontend dist.tar.gz" \
    "$LOCAL_DIST_ZIP" \
    "$TARGET:$REMOTE_ZIP" \
    "$REMOTE_ZIP"

upload_with_progress \
    "Upload backend jar" \
    "$BACKEND_JAR" \
    "$TARGET:$REMOTE_JAR" \
    "$REMOTE_JAR"
# ==========================================================

run_step "Extract frontend on server" extract_remote_zip
run_step "Restart remote services" restart_remote_services

# 清理本地临时文件
rm -f "$LOCAL_DIST_ZIP"

echo ""
echo "✅ Deploy completed."