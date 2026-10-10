# 🐾 MeowTools

**Vanish, edit inventories, and put your server commands on the F key.**

A server-side toolkit for Paper survival and community servers. Give staff live inventory editing and vanish, let players trigger your server commands with F-key shortcuts, and configure leaf decay, XP and explosions in the same plugin. Players need no client mod.

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/meowtools/versions) [![Documentation](https://img.shields.io/badge/GitHub-Documentation-181717?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) [![Report a bug](https://img.shields.io/badge/GitHub-Report_a_bug-d73a4a?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

**[⬇ Download MeowTools](https://modrinth.com/plugin/meowtools/versions)** — one JAR for the supported Paper releases listed below. See the [feature overview](README.md) for a quick introduction.

## Quick start

1. [Download MeowTools 2.2.0](https://modrinth.com/plugin/meowtools/versions) for a supported Paper version below.
2. Put the JAR in the server's `plugins` folder and restart the server.
3. Run `/mt help`, then edit `plugins/MeowTools/config.yml` and use `/mt reload` as an administrator.

The default language is Chinese; set `language: en_us` for English. Review the enabled F-key shortcuts before opening the server to players: they execute the example commands until customized. PlaceholderAPI is optional, for placeholders in inventory information.

## Compatibility

One plugin JAR supports all of the following tested versions:

| Platform | Minecraft release | Server Java |
| --- | --- | --- |
| Paper | 1.21.10, 1.21.11 | 21 |
| Paper | 26.1.1, 26.1.2, 26.2, 26.3 | 25 |

Spigot, Folia, earlier 1.21 versions, release candidates and pre-releases are outside the supported scope. Paper has no 26.1 server build. Paper's final 26.1.1 build is ALPHA and its current 26.3 build is BETA, despite these being Minecraft release versions.

The [API matrix](gradle/paper-versions.properties) pins upstream builds. Checks compile source against each API and run regressions plus API member linkage checks against the **same baseline Java 21 plugin classes**. This does not replace multiplayer testing on a real server. New releases must pass the matrix before support is claimed; weekly CI detects missing Minecraft releases.

## ✨ What you can do

- **Moderate from one place:** inspect and edit online inventories, armor and offhand; vanish with a BossBar indicator and optional night vision; check player ping.
- **Make common commands easier:** bind F, Shift+F, and looking up/down with either key combination. Commands run with the player's own permissions and a configurable cooldown.
- **Tune survival gameplay:** accelerate detached leaf decay, configure permission-based XP multipliers, and control block damage and entity damage separately for TNT, creepers and other explosions.
- **Keep chat manageable:** customize join/quit messages, reduce repeated death messages and block selected commands.
- **Configure in your language:** Chinese/English messages, optional PlaceholderAPI inventory information, and configuration migration with a backup.

## Commands and behavior

- `/mt help`, `/mt ping [player]`, `/mt inv [player]`, `/mt vanish [player] [on|off]`, `/mt reload`.
- Live inventory inspection/editing, vanish with BossBar and optional night vision.
- Configurable F-key shortcuts, join/quit messages, death message cooldown, explosion controls.
- Bounded, deduplicated leaf decay without loading unloaded chunks.
- Permission-based experience multipliers and bounded orb merging that preserves stacked XP and leaves pickup/Mending to vanilla. Tagged orbs and differing sources are not merged; merge cancellation is respected.
- Cached command blocking and optional command-level permission diagnostics (disabled by default).
- English/Chinese language files; optional PlaceholderAPI.

Inventory editing uses authoritative live slots: left/right click, number keys and shift-click extraction. Dragging into the upper GUI, shift-click insertion from the viewer inventory, double-click collection and creative cloning are blocked. Closing a GUI never commits an old snapshot. Armor slots require suitable equipment.

Inventory heads reuse skin textures already present on the server. Players without textures use a default head without requesting profile completion from Mojang.

Configuration lives in `plugins/MeowTools/config.yml`. Integer `config-version` is independent of the plugin version. Legacy configuration migrates with an original-file backup and temporary-file replacement, preserving user values and custom keys. Malformed YAML is not overwritten.

Leaf settings are under `server_settings.fast_leaf_decay`; limits: 1–1000 candidates/tick and 1–100000 queued positions. Orb radius: 0–4; merges/pickup: 0–128. Multipliers accept finite values 0–1000; explicitly denied permission nodes are ignored. Numeric `meowtools.exp.<value>` grants take precedence, then configured groups in order. Shortcut cooldown defaults to 250 ms.

Administrative permissions default to OP: `meowtools.admin`, `meowtools.inv`, `meowtools.vanish`, `meowtools.vanish.other`, `meowtools.vanish.see`, `meowtools.bypass.blocked_commands`. `meowtools.use` and `meowtools.ping` default to everyone.

## Build

Install JDK 21 and JDK 25, then run:

```sh
bash ./gradlew build checkCompatibility
python scripts/check_paper_releases.py
```

Windows: `gradlew.bat build checkCompatibility`. If JDK discovery fails, configure Gradle's `org.gradle.java.installations.paths`. Artifact: `build/libs/MeowTools-2.2.0.jar`; test reports: `build/reports/tests/`.

Reproducible real Paper acceptance and 20-client stress scenarios are documented in [the live test guide](scripts/live/README.md). The instrumentation plugin is built separately and excluded from the release JAR; evidence is saved under `build/reports/live/`. Bots on 26.2/26.3 connect through protocol translation; native graphical clients and production plugin combinations still need acceptance.

Before production deployment, validate multiplayer inventory transfers, target logout/death, night vision cleanup, XP/Mending, protection plugins and repeated reloads on an isolated test server.

Published validation evidence and its limits are available in [the release verification report](reports/2.2.0/verification.md).

## Support and links

- [Source code](https://github.com/xiaoyiluck666/MeowTools) · [中文说明](README_CN.md)
- [Report a bug or request a feature](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose). Include the MeowTools version, exact Paper build, Java version, relevant configuration, logs and reproduction steps; remove credentials and private player data.
- [Download on Modrinth](https://modrinth.com/plugin/meowtools)

If MeowTools saves you administration time, support continued maintenance through [PayPal](https://www.paypal.com/paypalme/neworldcn) or [Afdian](https://afdian.com/a/xiaoyiluck).
