# NOTICE — modifications in AEmulator Pro

AEmulator Pro is a **modified version** of [AEmulator](https://github.com/uxazu/aemulator)
by uxazu and contributors, which is itself based on
[SENSES](https://github.com/Istratii-tech/SENSES) by Istratii-tech
(original author, [t.me/istratii_tech1](https://t.me/istratii_tech1)).

This fork is maintained by [jntm233-jntm](https://github.com/jntm233-jntm).

Both the original project and this fork are distributed under the
**GNU General Public License, version 3** (see [LICENSE](LICENSE)).

In accordance with GPL-3.0 §5(a) (“carry prominent notices stating that you changed the
files and the date of any change”), this file records the changes made in this fork.

## Changes, 2026-11

Application identity and branding:

- application name **AEmulator Pro** (was “AEmulator”), launcher label and in-app title follow it;
- application id **`app.aemu.pro`** (was `app.aemu`), so the fork installs next to the original;
- launcher icon background and accent colour changed to **#006D61**, light and dark Material 3
  colour schemes rebuilt around it, logo bitmaps recoloured to match.

User interface and behaviour:

- settings screen reworked: section order, collapsible “new system defaults”, licence and
  modification-notice pages, links to the original project and its author;
- **predictive back animation** for the settings screen (Android 13+), with a switch;
- Android 16 **live-update notifications** (status-bar chip / “dynamic island”) for the running
  machine and for firmware import, with a switch between live and ordinary notifications;
- **fast reboot**: the machine window hands the restart over to an activity in the main process
  and reopens as soon as the `:vm` process exits, instead of waiting for an alarm;
- per-machine password protection, a separate task stack per machine, background firmware import
  with a progress notification, per-machine “skip setup wizard”, movable/hideable “…” button;
- new and updated translations in 18 locales.

Build:

- product flavors `standard`/`clone`/`v0003` removed; a single package `app.aemu.pro` is built and
  the APK is `app-debug.apk` / `app-release.apk` without a `.clone` suffix;
- `copyReleaseApks` now produces `release-apks/<version>/AEmulatorPro-<version>.apk`.

## Distribution notes

- The complete corresponding source of this fork is this repository; the original source is
  [uxazu/aemulator](https://github.com/uxazu/aemulator).
- No warranty is provided, to the extent permitted by law.
- Names and links of the original authors are kept for attribution only. This fork is not
  affiliated with, sponsored or endorsed by them, and “AEmulator” remains the name of the
  original project.
