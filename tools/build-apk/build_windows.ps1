Param(
    # Where the JDK, Android SDK, Gradle caches and the build tool itself live. Nothing is installed outside it.
    [string]$Root = 'D:\aemu-build',
    [string]$SdkRoot,
    [switch]$Mirror,
    # Gradle task to run: assembleRelease is what releases are built with (debug needs a fresh /data each boot)
    [string]$Task = 'assembleRelease',
    [ValidateSet('clean', 'reuse', 'keep')]
    [string]$Sync = 'clean',
    [switch]$SkipToolchain,
    [switch]$InstallStudio
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$sw = [Diagnostics.Stopwatch]::StartNew()
$log = Join-Path $Root 'build.log'
New-Item -ItemType Directory -Force -Path $Root | Out-Null
if ($SdkRoot) { New-Item -ItemType Directory -Force -Path $SdkRoot | Out-Null }
if ($Sync -eq 'clean') { Remove-Item -Recurse -Force (Join-Path $Root 'src') -ErrorAction SilentlyContinue }

function Say([string]$m) {
    $line = '[{0:mm\:ss}] {1}' -f $sw.Elapsed, $m
    Write-Output $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

function Get-File([string]$Url, [string]$Out, [string]$Name = '') {
    if (-not $Name) { $Name = Split-Path $Url -Leaf }
    Say "download $Name"
    $p = Start-Process curl.exe -NoNewWindow -PassThru -Wait -ArgumentList @('-fL', '--retry', '3', '--retry-delay', '3', '--connect-timeout', '30', '-o', $Out, $Url)
    if ($p.ExitCode -ne 0 -or -not (Test-Path $Out) -or (Get-Item $Out).Length -lt 1024) {
        throw "download failed: $Url"
    }
}

try {
    Add-Content -Path $log -Value "=== build started $(Get-Date -Format s) ===" -Encoding UTF8

    # ---------------------------------------------------------------- project
    $src = Join-Path $Root 'src'
    if (-not (Test-Path (Join-Path $src 'app'))) {
        Say "copy project -> $src"
        New-Item -ItemType Directory -Force -Path $src | Out-Null
        robocopy $PSScriptRoot $src /E /MT:16 /NFL /NDL /NJH /NJS /NP /XD .git build .gradle .idea .kotlin release-apks tools | Out-Null
        if ($LASTEXITCODE -ge 8) { throw "robocopy failed ($LASTEXITCODE)" }
    }
    $tools = Join-Path $src 'tools\build-apk'
    $mirrors = $null
    if (Test-Path (Join-Path $tools 'mirrors.json')) { $mirrors = Get-Content (Join-Path $tools 'mirrors.json') -Raw | ConvertFrom-Json }

    # ---------------------------------------------------------------- toolchain
    if (-not $SkipToolchain) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools 'prepare_env.ps1') -Root $Root -SdkRoot $SdkRoot -Mirror:$Mirror -InstallStudio:$InstallStudio
        if ($LASTEXITCODE -ne 0) { throw "toolchain setup failed ($LASTEXITCODE)" }
    }
    $envFile = Join-Path $Root 'env.txt'
    $envPairs = @{}
    if (Test-Path $envFile) {
        foreach ($l in Get-Content $envFile) { if ($l -match '^(.+?)=(.*)$') { $envPairs[$matches[1]] = $matches[2] } }
    }
    $javaHome = $envPairs['JAVA_HOME']
    if (-not $javaHome) {
        $c = Get-ChildItem (Join-Path $Root 'jdk') -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($c) { $javaHome = $c.Directory.Parent.FullName }
    }
    if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) { throw "no JDK found (set -Root or run prepare_env.ps1)" }
    $env:JAVA_HOME = $javaHome
    foreach ($n in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') { if ($envPairs[$n]) { Set-Item -Path "env:$n" -Value $envPairs[$n] } }
    $env:PATH = "$javaHome\bin;" + $env:PATH
    Say "JAVA_HOME=$javaHome  ANDROID_HOME=$env:ANDROID_HOME"

    # the JDK of this machine goes into the project's gradle.properties (the checkout names Android Studio's JBR by default)
    $gp = Join-Path $src 'gradle.properties'
    $gpText = @([IO.File]::ReadAllText($gp) -split "`r?`n" | Where-Object { $_ -notmatch '^\s*org\.gradle\.java\.home' })
    $gpText += "org.gradle.java.home=$($javaHome -replace '\\', '/')"
    $gpText += 'org.gradle.daemon=false'
    [IO.File]::WriteAllLines($gp, [string[]]$gpText, (New-Object Text.UTF8Encoding $false))

    # ---------------------------------------------------------------- gradle
    $gradle = Get-ChildItem (Join-Path $Root 'gradle') -Filter gradle.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $gradle) { throw 'Gradle is missing: run prepare_env.ps1 without -SkipToolchain' }
    $gradleBat = $gradle.FullName
    $gradleHome = Split-Path (Split-Path $gradleBat -Parent) -Parent
    if ($envPairs['GRADLE_HOME']) { $gradleHome = $envPairs['GRADLE_HOME'] }

    # the checkout's own wrapper downloads its distribution with a Java HTTP client; the installed Gradle is used instead
    $gargs = @('--no-daemon', '--console=plain', '--stacktrace')
    $gargs += "-p $src"
    $gargs += "--gradle-user-home=$Root\gradle-home"
    $gargs += "-Porg.gradle.project.dir=$src"
    if ($Mirror) { $gargs += '-Paemu.mirror' }
    $gargs += $Task
    $line = "`"$gradleBat`" " + (($gargs | ForEach-Object { '`"' + $_ + '`"' }) -join ' ')
    Say "gradle $Task (this takes a while: the engine assets are packed from scratch)"
    $plog = Join-Path $Root 'gradle.out'
    $p = Start-Process cmd.exe -NoNewWindow -PassThru -Wait -ArgumentList @('/c', "$line > `"$plog`" 2>&1")
    Get-Content $plog -ErrorAction SilentlyContinue | Select-Object -Last 60 | ForEach-Object { Add-Content -Path $log -Value $_ -Encoding UTF8 }
    if ($p.ExitCode -ne 0) {
        Say "gradle failed ($($p.ExitCode)); last lines:"
        Get-Content $plog | Select-Object -Last 40 | ForEach-Object { Say ('  ' + $_) }
        exit 1
    }

    # ---------------------------------------------------------------- artifacts
    $outDir = Join-Path $Root 'out'
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $apks = Get-ChildItem (Join-Path $src 'app\build\outputs\apk') -Filter *.apk -Recurse -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if (-not $apks) { Say 'gradle succeeded but produced no APK'; exit 1 }
    foreach ($a in $apks) {
        $dest = Join-Path $outDir $a.Name
        Copy-Item $a.FullName $dest -Force
        Say ('APK {0}  {1} MB' -f $dest, [math]::Round($a.Length / 1MB, 1))
    }
    Say "done in $([int]$sw.Elapsed.TotalMinutes) min"
    exit 0
} catch {
    Say "FAILED: $($_.Exception.Message)"
    exit 1
}
