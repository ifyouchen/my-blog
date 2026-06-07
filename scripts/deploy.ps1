param(
    [string]$HostName = "43.155.132.161",
    [string]$User = "root",
    [string]$Password = "Aa18379574807.",
    [int]$Port = 22,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$UseSshPass = ![string]::IsNullOrEmpty($Password)
if ($UseSshPass -and -not (Get-Command "sshpass" -ErrorAction SilentlyContinue)) {
    throw "请先安装 sshpass：管理员权限 PowerShell 执行：choco install sshpass"
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

# ==================== 路径配置 ====================
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$FrontendDir = Join-Path $RepoRoot "frontend"
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDist = Join-Path $FrontendDir "dist"
$DistZip = Join-Path $FrontendDir "dist.zip"
$JarName = "my-blog-backend-0.1.0.jar"
$BackendJar = Join-Path $BackendDir "target\$JarName"

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
    if ($LASTEXITCODE -ne 0) { throw "$Step 失败" }
}

# ==================== 部署流程 ====================
$initChoice = Read-Host "是否初始化 SQL 数据？(输入 yes 确认，直接回车跳过)"
$InitSql = ($initChoice.Trim() -eq "yes")

Invoke-NativeCommand "构建前端项目" {
    Push-Location $FrontendDir
    npm run build
    Pop-Location
}

Invoke-NativeCommand "打包后端 Jar 包" {
    Push-Location $BackendDir
    if ($SkipTests) { mvn package -DskipTests } else { mvn package }
    Pop-Location
}

if (-not (Test-Path $FrontendDist)) { throw "前端 dist 未生成" }
if (-not (Test-Path $BackendJar)) { throw "后端 Jar 未生成" }

Invoke-NativeCommand "压缩前端 dist" {
    if (Test-Path $DistZip) { Remove-Item $DistZip -Force }
    Compress-Archive -Path "$FrontendDist\*" -DestinationPath $DistZip -Force
}

Invoke-NativeCommand "创建服务器目录" {
    Invoke-SshCommand "mkdir -p $RemoteFrontendParent $RemoteBackendDir"
}

Invoke-NativeCommand "上传前端 dist.zip" {
    Invoke-ScpUpload -Local $DistZip -Remote "${User}@${HostName}:$RemoteZip"
}

Invoke-NativeCommand "上传后端 Jar" {
    Invoke-ScpUpload -Local $BackendJar -Remote "${User}@${HostName}:$RemoteJar"
}

# ==================== 已修复：宝塔 .user.ini 权限问题 ====================
Invoke-NativeCommand "服务器解压前端文件" {
    Invoke-SshCommand "cd $RemoteFrontendParent &&
chattr -i dist/.user.ini 2>/dev/null || true &&
rm -rf dist &&
unzip -o dist.zip -d dist &&
chattr +i dist/.user.ini 2>/dev/null || true &&
rm -f dist.zip"
}

Invoke-NativeCommand "重启服务" {
    $cmd = "cd $RemoteBase && chmod +x ./restart.sh && ./restart.sh"
    if ($InitSql) { $cmd += " init" }
    Invoke-SshCommand $cmd
}

if (Test-Path $DistZip) { Remove-Item $DistZip -Force }

Write-Host "`n🎉 部署成功！`n" -ForegroundColor Green