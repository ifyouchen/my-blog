param(
    [string]$HostName = "43.155.132.161",
    [string]$User = "root",
    [string]$Password = "Aa18379574807.",
    [int]$Port = 22,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# 自动处理 sshpass
$UseSshPass = ![string]::IsNullOrEmpty($Password)
if ($UseSshPass -and -not (Get-Command "sshpass" -ErrorAction SilentlyContinue)) {
    throw "请先安装 sshpass：choco install sshpass"
}

function Invoke-SshCommand {
    param([string]$Command)
    $sshArgs = @(
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-p", $Port,
        "$User@$HostName",
        $Command
    )
    if ($UseSshPass) {
        & sshpass -p $Password ssh $sshArgs
    } else {
        & ssh $sshArgs
    }
}

function Invoke-ScpUpload {
    param([string]$Local, [string]$Remote)
    $scpArgs = @(
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-P", $Port,
        $Local,
        $Remote
    )
    if ($UseSshPass) {
        & sshpass -p $Password scp $scpArgs
    } else {
        & scp $scpArgs
    }
}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$FrontendDir = Join-Path $RepoRoot "frontend"
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDist = Join-Path $FrontendDir "dist"
$DistZip = Join-Path $FrontendDir "dist.zip"
$JarName = "my-blog-backend-0.1.0.jar"
$BackendJar = Join-Path $BackendDir "target\$JarName"

# 你的服务器路径
$RemoteRoot = "/www/wwwroot"
$RemoteBase = "$RemoteRoot/myblog"
$RemoteFrontendParent = "$RemoteBase/frontend"
$RemoteBackendDir = "$RemoteBase/backend"
$RemoteJar = "$RemoteBackendDir/$JarName"
$RemoteZip = "$RemoteFrontendParent/dist.zip"

function Invoke-NativeCommand {
    param([string]$Step, [scriptblock]$Command)
    Write-Host "`n==> $Step" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) { throw "$Step 失败，错误码 $LASTEXITCODE" }
}

# 询问是否初始化 SQL
$initChoice = Read-Host "是否初始化 SQL 数据？(输入 yes 确认，直接回车跳过)"
$InitSql = $initChoice -and $initChoice.Trim().ToLower() -eq "yes"

# 构建前端
Invoke-NativeCommand "构建前端项目" {
    Push-Location $FrontendDir
    npm run build
    Pop-Location
}

# 打包后端
Invoke-NativeCommand "打包后端 Jar 包" {
    Push-Location $BackendDir
    if ($SkipTests) {
        Write-Host "⚠️  已跳过 Java 单元测试"
        mvn package -DskipTests
    } else {
        mvn package
    }
    Pop-Location
}

# 检查构建结果
if (-not (Test-Path $FrontendDist)) { throw "错误：前端 dist 目录未生成！" }
if (-not (Test-Path $BackendJar)) { throw "错误：后端 Jar 包未生成！" }

# ==============================================
# ✅ 关键优化：压缩前端 dist
# ==============================================
Invoke-NativeCommand "压缩前端 dist 为 zip" {
    if (Test-Path $DistZip) { Remove-Item $DistZip -Force }
    Compress-Archive -Path "$FrontendDist\*" -DestinationPath $DistZip -Force
}

# 服务器创建目录
Invoke-NativeCommand "准备远程服务器目录" {
    Invoke-SshCommand "mkdir -p '$RemoteFrontendParent' '$RemoteBackendDir'"
}

# 上传 zip（只传一个文件！）
Invoke-NativeCommand "上传前端 dist.zip" {
    Invoke-ScpUpload -Local $DistZip -Remote "${User}@${HostName}:$RemoteZip"
}

# 上传 jar
Invoke-NativeCommand "上传后端 Jar" {
    Invoke-ScpUpload -Local $BackendJar -Remote "${User}@${HostName}:$RemoteJar"
}

# ==============================================
# ✅ 服务器解压 + 清理压缩包
# ==============================================
Invoke-NativeCommand "服务器解压前端文件" {
    Invoke-SshCommand @"
cd '$RemoteFrontendParent'
rm -rf dist
unzip -o dist.zip -d dist
rm -f dist.zip
"@
}

# 启动服务
Invoke-NativeCommand "重启前后端服务" {
    Invoke-SshCommand "cd '$RemoteBase' && chmod +x ./restart.sh && ./restart.sh$(if ($InitSql) {' init'})"
}

# 清理本地 zip
if (Test-Path $DistZip) { Remove-Item $DistZip -Force }

Write-Host "`n🎉 部署全部完成！速度起飞！`n" -ForegroundColor Green