# Building the APK without Android Studio

These scripts set up a self-contained toolchain (JDK, Android SDK, Gradle) in a folder of
their own and build the APK from this checkout. Nothing is installed system-wide.

```
powershell -ExecutionPolicy Bypass -File tools\build-apk\build_windows.ps1
```

Result: `D:\aemu-build\out\app-release.apk` (release variant of the single app),
logs in `D:\aemu-build\build.log`.

## What it does

1. `prepare_env.ps1` downloads Gradle (the version in `gradle/wrapper/gradle-wrapper.properties`), a JDK
   and the Android SDK parts into `-Root` (default `D:\aemu-build`): `platforms/android-35`,
   `build-tools/35.0.0`, `platform-tools`.
2. `build_windows.ps1` copies this checkout to `<root>\src`, points `gradle.properties` at the JDK it
   found, and runs `gradle assembleRelease`.
3. The APK is copied to `<root>\out`.

## Options

| Option | Meaning |
| --- | --- |
| `-Root D:\aemu-build` | where the toolchain, the build copy and the APK go |
| `-SdkRoot D:\sdk` | Android SDK location if it is not `<Root>\sdk` |
| `-Mirror` | also try the Alibaba mirrors for Gradle (`mirrors.aliyun.com`) and Maven (`maven.aliyun.com`). Gradle gets `-Paemu.mirror`, so `settings.gradle.kts` asks those repositories first |
| `-Task assembleDebug` | other Gradle task (e.g. `assembleDebug`) |
| `-Sync reuse` | build from the existing `<Root>\src` copy instead of a fresh one (`keep` = as is, `clean` = default) |
| `-SkipToolchain` | toolchain already installed, build only |
| `-InstallStudio` | use Android Studio's own JDK and `sdkmanager` (larger download, ~1.2 GB) |

## Networks where Google's hosts are blocked

`curl` and the SDK downloads in the default tables use plain `http://dl.google.com`, which answers in
places where `https://` does not. If a local mirror is faster or the only route that works, put its URLs in
`tools/build-apk/mirrors.json` (Gradle distributions, command-line tools, SDK packages) and run with
`-Mirror` for the Maven side.

## Why not `gradlew`

`./gradlew` asks services.gradle.org for its distribution through a Java HTTP client, which needs HTTPS to
that host. `build_windows.ps1` uses an installed Gradle instead. The Maven repositories the build then uses
need HTTPS too (`plugins.gradle.org`, `repo.maven.apache.org`, `dl.google.com/dl/android/maven2`); the
`-Mirror` switch exists for networks where those fail.

## Requirements for the APK

The APK carries every engine asset (about 470 MB of guest files, packed once as blobs; expect a
`packEngines` step that takes a while on the first build) and the arm64 native libraries. `debug` builds
need a fresh `/data` per boot; releases are built with `assembleRelease`.
