<div align="center">

<img src="docs/assets/logo.png" width="128" alt="AEmulator Pro logo"/>

# AEmulator Pro

*A community fork of [AEmulator](https://github.com/uxazu/aemulator) — run classic Android firmware on a modern phone.*

[![License](https://img.shields.io/badge/license-GPL--3.0-006D61?style=for-the-badge)](LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/jntm233-jntm/AEmulator-Pro) [![Version](https://img.shields.io/badge/version-0.0.0.3.1-006D61?style=for-the-badge)](https://github.com/jntm233-jntm/AEmulator-Pro/releases)

</div>

---

AEmulator Pro boots a real Android system image straight from a firmware file you already have: a recovery ZIP, an Odin archive, a Google factory image or a GSI. Old 32-bit firmware (2.3–5.4) runs through a patched QEMU, Android 5.0–17 runs natively on the phone's arm64 cores. The kernel's binder is emulated, graphics use your phone's GPU and sound goes through Android's audio stack — everything runs inside a normal app, no root and no PC.

## ✦ What is different in this fork

Compared to upstream [AEmulator](https://github.com/uxazu/aemulator):

| | |
|---|---|
| **Name and package** | *AEmulator Pro*, package `app.aemu.pro` (installs next to the original) |
| **Branding** | new launcher icon colours and a teal `#006D61` accent, light and dark schemes |
| **Settings** | reworked layout, collapsible categories, predictive-back animation |
| **Notifications** | Android 16 live updates (status-bar chip / “dynamic island”), switchable |
| **Reboot** | fast hand-off restart: the machine window closes and reopens at once |
| **Safety** | per-machine password protection (PBKDF2), separate task per machine |
| **Import** | firmware import in the background with progress in the notification |
| **Legal** | licence and modification-notice pages inside the app |
| **Startup** | per-machine setup wizard skip, “…” button can be hidden or moved |

## ✔ Features

- Import almost any firmware format: CWM/TWRP ZIP, Samsung Odin `.tar.md5`, Google factory `.tgz`, `system.img`, OTA `system.new.dat.br`
- Vendor skins work as shipped: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Hardware graphics through the GL bridge, sound, touch and multitouch, network with a modern TLS proxy
- Shared memory-card folder for APKs, music and photos
- Material 3 Expressive interface in 18 languages
- Free and open source (GPL-3.0)

## 🚀 Quick start

1. Download the APK from [Releases](https://github.com/jntm233-jntm/AEmulator-Pro/releases) and install it.
2. Copy a firmware file to the phone.
3. Open AEmulator Pro → **Add firmware** and pick the file. Import takes a few minutes.
4. Press **Start**. The first boot is slower: the system optimises its apps.
5. Use the ⋮ menu for volume, power button and logs; the ⚙ button opens app settings and language.

## 📋 Requirements

- Android 8.0+ (API 26) on a 64-bit ARM phone (arm64-v8a)
- About 1–3 GB of free space per firmware
- A recent Snapdragon / Dimensity / Tensor chip is recommended

## ⚙️ How it works

On Android 2.3–5.4 each guest process runs under a patched user-mode QEMU; on 5.0–17 the firmware's own arm64 programs run natively, with one engine kernel per Android version catching their system calls. A binder daemon replaces the kernel driver, a GL bridge forwards OpenGL ES calls to the phone's GPU, and small guest libraries (audio HAL, audio policy wrapper, LD_PRELOAD shim) adapt vendor code to the emulator. The importer reads the firmware, finds the init scripts in its boot image and builds a start plan for system services.

## 🛠️ Build from source

You need JDK 21 and Android SDK 36 (the guest engines are prebuilt and shipped in `app/src/main/assets`; the NDK is only needed if you rebuild the native parts with `native/*/build.sh`).

```bash
git clone https://github.com/jntm233-jntm/AEmulator-Pro.git
cd AEmulator-Pro
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # signed if keystore.properties is present
./gradlew :app:copyReleaseApks    # release-apks/<version>/AEmulatorPro-<version>.apk
```

- Package id: `app.aemu.pro`
- There are no product flavors: the APK is `app-debug.apk` / `app-release.apk`, without any package suffix.

## 📄 Licence, credits and changes

AEmulator Pro is free software under the **GNU General Public License v3.0** — see [LICENSE](LICENSE). The full licence text is also bundled in the app (Settings → Project → Licence).

Credits:

- **[jntm233-jntm](https://github.com/jntm233-jntm)** — author of this fork (AEmulator Pro);
- **[uxazu](https://github.com/uxazu)** — author of [AEmulator](https://github.com/uxazu/aemulator), the upstream project this fork is based on;
- **[Istratii-tech](https://t.me/istratii_tech1)** — original author of the SENSES engine AEmulator grew out of.

This is an independent community fork. It is **not affiliated with, sponsored or endorsed by** the original authors; their names and links are kept only for attribution. The changes made in this fork are listed in [NOTICE.md](NOTICE.md) and inside the app (Settings → Project → Modification notice), as required by GPL-3.0 §5(a).

## 中文简介

**AEmulator Pro** 是 [AEmulator](https://github.com/uxazu/aemulator) 的社区修改版（原作者 uxazu，其引擎源自 Istratii-tech 的 SENSES），遵循 **GPL-3.0** 协议发布。

相对原版的主要改动：改名为 AEmulator Pro、包名 `app.aemu.pro`；全新图标配色与 `#006D61` 强调色；设置页重排并加入预测性返回动画；接入 Android 16 实时更新通知（可开关）；重启改为快速接力重启；每个虚拟机独立任务栈与密码保护；后台导入固件；应用内增加许可证与修改声明页面。

本项目与原作者无隶属关系，署名仅为遵守协议与致谢。完整改动见 [NOTICE.md](NOTICE.md)。
