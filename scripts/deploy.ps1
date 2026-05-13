param(
    [string]$HostName = "43.128.134.245",

    [string]$User = "root",

    [int]$Port = 22,

    [string]$IdentityFile = (Join-Path $PSScriptRoot "shouer.pem"),

    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$FrontendDir = Join-Path $RepoRoot "frontend"
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDist = Join-Path $FrontendDir "dist"
$JarName = "my-blog-backend-0.1.0.jar"
$BackendJar = Join-Path $BackendDir "target\$JarName"

$RemoteRoot = "/www/wwwroot"
$RemoteFrontendParent = "$RemoteRoot/my-blog/frontend"
$RemoteFrontendDist = "$RemoteFrontendParent/dist"
$RemoteJar = "$RemoteRoot/$JarName"
$Target = "$User@$HostName"

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Step,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Step"
    & $Command

    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE"
    }
}

function Protect-PrivateKeyFile {
    if (-not $IdentityFile) {
        return
    }

    $resolvedKey = Resolve-Path -LiteralPath $IdentityFile -ErrorAction SilentlyContinue

    if (-not $resolvedKey) {
        throw "Identity file was not found: $IdentityFile"
    }

    $script:IdentityFile = $resolvedKey.Path

    if ($env:OS -ne "Windows_NT") {
        return
    }

    $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $principalsToRemove = @(
        "Users",
        "Authenticated Users",
        "Everyone",
        "BUILTIN\Users",
        "CodexSandboxUsers",
        "$env:COMPUTERNAME\CodexSandboxUsers"
    )

    Write-Host ""
    Write-Host "==> Protect private key permissions"

    & icacls $IdentityFile /inheritance:r | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to remove inherited permissions from ${IdentityFile}. Run PowerShell as Administrator and fix the key ACL first."
    }

    & icacls $IdentityFile /grant:r "${currentUser}:R" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to grant private key access to ${currentUser}. Run PowerShell as Administrator and fix the key ACL first."
    }

    foreach ($principal in $principalsToRemove) {
        & icacls $IdentityFile /remove:g $principal 2>$null | Out-Null
        & icacls $IdentityFile /remove:d $principal 2>$null | Out-Null
    }
}

function New-SshArgs {
    param(
        [switch]$BatchMode
    )

    $sshArgs = @()

    if ($IdentityFile) {
        $sshArgs += @("-i", $IdentityFile)
    }

    if ($BatchMode) {
        $sshArgs += @("-o", "BatchMode=yes")
    }

    $sshArgs += @("-p", "$Port", $Target)
    return $sshArgs
}

function New-ScpArgs {
    param(
        [switch]$Quiet
    )

    $scpArgs = @()

    if ($IdentityFile) {
        $scpArgs += @("-i", $IdentityFile)
    }

    if ($Quiet) {
        $scpArgs += "-q"
    }

    $scpArgs += @("-P", "$Port")
    return $scpArgs
}

function ConvertTo-ShellSingleQuoted {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Format-ByteSize {
    param(
        [Parameter(Mandatory = $true)]
        [long]$Bytes
    )

    if ($Bytes -ge 1GB) {
        return "{0:N2} GB" -f ($Bytes / 1GB)
    }

    if ($Bytes -ge 1MB) {
        return "{0:N2} MB" -f ($Bytes / 1MB)
    }

    if ($Bytes -ge 1KB) {
        return "{0:N2} KB" -f ($Bytes / 1KB)
    }

    return "$Bytes B"
}

function Get-LocalByteSize {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $item = Get-Item -LiteralPath $Path

    if (-not $item.PSIsContainer) {
        return [long]$item.Length
    }

    $sum = Get-ChildItem -LiteralPath $Path -Recurse -File |
        Measure-Object -Property Length -Sum |
        Select-Object -ExpandProperty Sum

    if ($null -eq $sum) {
        return 0
    }

    return [long]$sum
}

function Get-RemoteByteSize {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemotePath
    )

    $remotePathLiteral = ConvertTo-ShellSingleQuoted $RemotePath
    $command = "if [ -e $remotePathLiteral ]; then du -sb $remotePathLiteral 2>/dev/null | cut -f1; else printf 0; fi"
    $output = & ssh @(New-SshArgs -BatchMode) $command 2>$null

    if ($LASTEXITCODE -ne 0 -or -not $output) {
        return $null
    }

    $firstLine = @($output)[0]
    $remoteBytes = 0L

    if ([long]::TryParse($firstLine, [ref]$remoteBytes)) {
        return $remoteBytes
    }

    return $null
}

function Test-UploadProgressProbe {
    $output = & ssh @(New-SshArgs -BatchMode) "printf 1" 2>$null
    return $LASTEXITCODE -eq 0 -and @($output)[0] -eq "1"
}

function Start-NativeProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FileName,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $processStartInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $processStartInfo.FileName = $FileName
    $processStartInfo.UseShellExecute = $false

    foreach ($argument in $Arguments) {
        [void]$processStartInfo.ArgumentList.Add($argument)
    }

    return [System.Diagnostics.Process]::Start($processStartInfo)
}

function Invoke-UploadWithProgress {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Step,

        [Parameter(Mandatory = $true)]
        [string]$LocalPath,

        [Parameter(Mandatory = $true)]
        [string]$RemoteTarget,

        [Parameter(Mandatory = $true)]
        [string]$RemoteProgressPath,

        [Parameter(Mandatory = $true)]
        [int]$ProgressId,

        [Parameter(Mandatory = $true)]
        [bool]$CanProbeProgress,

        [switch]$Recursive
    )

    Write-Host ""
    Write-Host "==> $Step"

    if (-not $CanProbeProgress) {
        Write-Host "Upload progress probe unavailable; falling back to scp progress."

        if ($Recursive) {
            scp @(New-ScpArgs) -r $LocalPath $RemoteTarget
        } else {
            scp @(New-ScpArgs) $LocalPath $RemoteTarget
        }

        if ($LASTEXITCODE -ne 0) {
            throw "$Step failed with exit code $LASTEXITCODE"
        }

        return
    }

    $totalBytes = Get-LocalByteSize $LocalPath
    $scpArgs = New-ScpArgs -Quiet

    if ($Recursive) {
        $scpArgs += @("-r", $LocalPath, $RemoteTarget)
    } else {
        $scpArgs += @($LocalPath, $RemoteTarget)
    }

    $process = Start-NativeProcess "scp" $scpArgs

    try {
        while (-not $process.HasExited) {
            $remoteBytes = Get-RemoteByteSize $RemoteProgressPath

            if ($null -ne $remoteBytes -and $totalBytes -gt 0) {
                $clampedBytes = [Math]::Min([long]$remoteBytes, [long]$totalBytes)
                $percent = [Math]::Min(99, [Math]::Floor(($clampedBytes * 100.0) / $totalBytes))
                $status = "$(Format-ByteSize $clampedBytes) / $(Format-ByteSize $totalBytes)"

                Write-Progress -Id $ProgressId -Activity $Step -Status $status -PercentComplete $percent
            } else {
                Write-Progress -Id $ProgressId -Activity $Step -Status "Uploading..." -PercentComplete 0
            }

            Start-Sleep -Milliseconds 800
        }

        $process.WaitForExit()

        if ($process.ExitCode -ne 0) {
            throw "$Step failed with exit code $($process.ExitCode)"
        }

        Write-Progress -Id $ProgressId -Activity $Step -Status "Completed" -PercentComplete 100
        Start-Sleep -Milliseconds 200
    } finally {
        Write-Progress -Id $ProgressId -Activity $Step -Completed

        if (-not $process.HasExited) {
            $process.Kill()
        }
    }
}

Protect-PrivateKeyFile

# SQL 初始化提示（默认不初始化）
$InitSql = $false
$initChoice = Read-Host "是否初始化 SQL 数据？(输入 yes 确认，直接回车跳过)"
if ($initChoice -and $initChoice.Trim().ToLower() -eq "yes") {
    $InitSql = $true
    Write-Host "SQL 初始化已开启"
}

Invoke-NativeCommand "Build frontend" {
    Push-Location $FrontendDir
    try {
        npm run build
    } finally {
        Pop-Location
    }
}

Invoke-NativeCommand "Package backend" {
    Push-Location $BackendDir
    try {
        if ($SkipTests) {
            mvn package -DskipTests
        } else {
            mvn package
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $FrontendDist)) {
    throw "Frontend dist directory was not generated: $FrontendDist"
}

if (-not (Test-Path $BackendJar)) {
    throw "Backend jar was not generated: $BackendJar"
}

$SshArgs = New-SshArgs
$CanProbeUploadProgress = Test-UploadProgressProbe
$RemotePrepareCommand = "set -e; mkdir -p '$RemoteFrontendParent'; rm -rf '$RemoteFrontendDist'; rm -f '$RemoteJar'"
$RemoteRestartCommand = "set -e; cd '$RemoteRoot'; chmod +x './restart.sh'; ./restart.sh$(if ($InitSql) { ' init' })"

Invoke-NativeCommand "Prepare remote directories" {
    ssh @SshArgs $RemotePrepareCommand
}

Invoke-UploadWithProgress `
    -Step "Upload frontend dist" `
    -LocalPath $FrontendDist `
    -RemoteTarget "${Target}:${RemoteFrontendParent}/" `
    -RemoteProgressPath $RemoteFrontendDist `
    -ProgressId 1 `
    -CanProbeProgress $CanProbeUploadProgress `
    -Recursive

Invoke-UploadWithProgress `
    -Step "Upload backend jar" `
    -LocalPath $BackendJar `
    -RemoteTarget "${Target}:${RemoteJar}" `
    -RemoteProgressPath $RemoteJar `
    -ProgressId 2 `
    -CanProbeProgress $CanProbeUploadProgress

Invoke-NativeCommand "Restart remote services" {
    ssh @SshArgs $RemoteRestartCommand
}

Write-Host ""
Write-Host "Deploy completed."
