Param(
    [string]$Root = 'D:\aemu-build',
    [string]$SdkRoot,
    # Alibaba mirrors for Gradle and Maven (for networks where Google's hosts are blocked)
    [switch]$Mirror,
    # Android Studio itself as the JDK (its JBR) and sdkmanager source: ~1.2 GB
    [switch]$InstallStudio
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if (-not $SdkRoot) { $SdkRoot = Join-Path $Root 'sdk' }

$log = Join-Path $Root 'prepare.log'
New-Item -ItemType Directory -Force -Path $Root, $SdkRoot | Out-Null
$sw = [Diagnostics.Stopwatch]::StartNew()

function Say([string]$m) {
    $line = '[{0:mm\:ss}] {1}' -f $sw.Elapsed, $m
    Write-Output $line
    Add-Content -Path $log -Value $line -Encoding UTF8
}

function Get-File([string]$Url, [string]$Out, [string]$Name = '') {
    if (-not $Name) { $Name = Split-Path $Url -Leaf }
    Say "download $Name"
    $p = Start-Process curl.exe -NoNewWindow -PassThru -Wait -ArgumentList @('-fL', '--retry', '3', '--retry-delay', '3', '--connect-timeout', '30', '-o', $Out, $Url)
    if ($p.ExitCode -ne 0 -or -not (Test-Path $Out) -or (Get-Item $Out).Length -lt 1024) { throw "download failed: $Url" }
}

# ---------------------------------------------------------------- what to fetch
$gradleVersion = '8.14.2'
$repo = Get-Content (Join-Path $Root 'src\gradle\wrapper\gradle-wrapper.properties') -ErrorAction SilentlyContinue
if ($repo -match 'gradle-([0-9.]+)-bin\.zip') { $gradleVersion = $matches[1] }
$mirrors = $null
$mj = Join-Path $PSScriptRoot 'mirrors.json'
if (Test-Path $mj) { $mirrors = Get-Content $mj -Raw | ConvertFrom-Json }

$gradleUrls = @()
if ($mirrors) { foreach ($b in $mirrors.gradle_distributions) { $gradleUrls += "$b/gradle-$gradleVersion-bin.zip" } }
if ($Mirror) {
    $gradleUrls += "https://mirrors.aliyun.com/gradle/distributions/v$gradleVersion/gradle-$gradleVersion-bin.zip"
    $gradleUrls += "https://mirrors.huaweicloud.com/gradle/gradle-$gradleVersion-bin.zip"
}
if (-not $gradleUrls -or -not $Mirror) {
    $gradleUrls += "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    $gradleUrls += "http://downloads.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
}

Say "root=$Root  sdk=$SdkRoot  gradle=$gradleVersion  mirror=$Mirror  studio=$InstallStudio"

# ---------------------------------------------------------------- gradle
$gradleHome = Join-Path $Root "gradle\gradle-$gradleVersion"
if (-not (Test-Path (Join-Path $gradleHome 'bin\gradle.bat'))) {
    $zip = Join-Path $Root "gradle-$gradleVersion-bin.zip"
    $ok = $false
    foreach ($u in $gradleUrls) {
        try { Get-File $u $zip; $ok = $true; break } catch { Say "  failed: $u" }
    }
    if (-not $ok) { throw 'Gradle download failed' }
    Say 'unpack gradle'
    Expand-Archive -Path $zip -DestinationPath (Join-Path $Root 'gradle') -Force
    Remove-Item $zip -Force
}
$gradleBat = Join-Path $gradleHome 'bin\gradle.bat'
Say "gradle: $gradleBat"

# ---------------------------------------------------------------- JDK and Android SDK
$javaHome = $null
$sdk = $null
if ($InstallStudio) {
    $studio = Join-Path $Root 'studio'
    $studioBat = Get-ChildItem $studio -Filter sdkmanager.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $studioBat) {
        $zip = Join-Path $Root 'android-studio.zip'
        Get-File 'http://dl.google.com/dl/android/studio/ide-zips/2024.1.1.12/android-studio-2024.1.1.12-windows.zip' $zip 'android-studio.zip (~1.2 GB)'
        Say 'unpack android studio'
        Expand-Archive -Path $zip -DestinationPath $studio -Force
        Remove-Item $zip -Force
    }
    $jbr = Get-ChildItem $studio -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\jbr\\bin\\java\.exe$' } | Select-Object -First 1
    if ($jbr) { $javaHome = $jbr.Directory.Parent.FullName }
} else {
    # JDK from the Android command-line tools' sdkmanager (OpenJDK) — the smallest download that brings a JDK 17
    $cmdline = Join-Path $Root 'cmdline-tools'
    $sm = Get-ChildItem $cmdline -Filter sdkmanager.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $sm) {
        $zip = Join-Path $Root 'commandlinetools.zip'
        $clUrls = @()
        if ($mirrors) { foreach ($u in $mirrors.commandline_tools) { $clUrls += $u } }
        if ($Mirror) {
            $clUrls += 'https://mirrors.aliyun.com/android.googlesource.com/commandlinetools-win-11076708_latest.zip'
            $clUrls += 'https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-win-11076708_latest.zip'
        }
        $clUrls += 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
        $clUrls += 'http://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
        $ok = $false
        foreach ($u in $clUrls) { try { Get-File $u $zip; $ok = $true; break } catch { Say "  failed: $u" } }
        if (-not $ok) { throw 'Android command-line tools download failed' }
        Say 'unpack command-line tools'
        $tmp = Join-Path $Root 'cmdline-unpack'
        Expand-Archive -Path $zip -DestinationPath $tmp -Force
        New-Item -ItemType Directory -Force -Path (Join-Path $cmdline 'latest') | Out-Null
        $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
        Copy-Item (Join-Path $inner.FullName '*') (Join-Path $cmdline 'latest') -Recurse -Force
        Remove-Item $zip, $tmp -Recurse -Force
        $sm = Get-ChildItem $cmdline -Filter sdkmanager.bat -Recurse | Select-Object -First 1
    }
    Say "sdkmanager: $($sm.FullName)"
    $sdkmanager = $sm.FullName
    $javaHome = Get-ChildItem (Join-Path $Root 'jdk') -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1 | ForEach-Object { $_.Directory.Parent.FullName }
    if (-not $javaHome) {
        $lj = Get-ChildItem (Split-Path (Split-Path $sm.FullName -Parent) -Parent) -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($lj) { $javaHome = $lj.Directory.Parent.FullName }
    }
    if (-not $javaHome) {
        $proxy = ''
        if ($Mirror) { $proxy = '--proxy=http --proxy_host=127.0.0.1 --proxy_port=7897' }
        Say 'install a JDK with sdkmanager'
        $p = Start-Process cmd.exe -NoNewWindow -PassThru -Wait -ArgumentList @('/c', "echo y| `"$sdkmanager`" --sdk_root=`"$SdkRoot`" $proxy jobd 2>&1")
        if ($p.ExitCode -ne 0) { Say 'sdkmanager jdk failed; Android Studio is still an option: run with -InstallStudio' }
        $javaHome = Get-ChildItem (Join-Path $Root 'jdk') -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 | ForEach-Object { $_.Directory.Parent.FullName }
    }
    if (-not $javaHome) { throw 'No JDK: run again with -InstallStudio' }
}
Say "JDK: $javaHome"

# ---------------------------------------------------------------- Android SDK platforms and build tools
$need = @{
    'android-35'    = @('platforms\android-35\android.jar')
    'build-tools'    = @('build-tools\35.0.0\apksigner.bat')
    'platform-tools' = @('platform-tools\adb.exe')
}
foreach ($k in $need.Keys) {
    $hit = $true
    foreach ($rel in $need[$k]) { if (-not (Test-Path (Join-Path $SdkRoot $rel))) { $hit = $false } }
    if ($hit) { Say "$k already in $SdkRoot"; continue }
    Say "fetch $k"
    $tmp = Join-Path $Root "pkg-$k"
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    switch ($k) {
        'android-35' {
            $zip = Join-Path $Root 'platform-35.zip'
            Get-File (($mirrors.sdk_packages.'platform-35_r02') + '/platform-35_r02.zip') $zip 'platform-35.zip'
            Expand-Archive $zip $tmp -Force; Remove-Item $zip -Force
            $d = Get-ChildItem $tmp -Directory -Filter 'android-*' | Select-Object -First 1
            New-Item -ItemType Directory -Force -Path (Join-Path $SdkRoot 'platforms') | Out-Null
            Copy-Item $d.FullName (Join-Path $SdkRoot 'platforms\android-35') -Recurse -Force
        }
        'build-tools' {
            $zip = Join-Path $Root 'build-tools.zip'
            Get-File (($mirrors.sdk_packages.'build-tools_r35_windows') + '/build-tools_r35_windows.zip') $zip 'build-tools.zip'
            Expand-Archive $zip $tmp -Force; Remove-Item $zip -Force
            $d = Get-ChildItem $tmp -Directory -Filter 'android-*' | Select-Object -First 1
            New-Item -ItemType Directory -Force -Path (Join-Path $SdkRoot 'build-tools') | Out-Null
            Copy-Item $d.FullName (Join-Path $SdkRoot 'build-tools\35.0.0') -Recurse -Force
        }
        'platform-tools' {
            $zip = Join-Path $Root 'platform-tools.zip'
            Get-File 'http://dl.google.com/android/repository/platform-tools-latest-windows.zip' $zip 'platform-tools.zip'
            Expand-Archive $zip $tmp -Force; Remove-Item $zip -Force
            $d = Get-ChildItem $tmp -Directory -Filter 'platform-tools' | Select-Object -First 1
            Copy-Item $d.FullName (Join-Path $SdkRoot 'platform-tools') -Recurse -Force
        }
    }
    Remove-Item $tmp -Recurse -Force
}

# ---------------------------------------------------------------- environment for build_windows.ps1
$envFile = Join-Path $Root 'env.txt'
$lines = @(
    "JAVA_HOME=$javaHome",
    "ANDROID_HOME=$SdkRoot",
    "ANDROID_SDK_ROOT=$SdkRoot",
    "GRADLE_HOME=$(Split-Path (Split-Path $gradleBat -Parent) -Parent)"
)
[IO.File]::WriteAllLines($envFile, $lines, (New-Object Text.UTF8Encoding $false))
Say "environment written to $envFile"
exit 0
