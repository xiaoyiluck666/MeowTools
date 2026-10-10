# 🐾 MeowTools

**Everyday staff tools and survival tweaks, together in one configurable Paper plugin.**

Inspect live inventories, vanish while moderating, bind commands to the F key, and tune leaf decay, experience and explosions. Install MeowTools on the server; players need no client mod.

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/meowtools/versions) [![Documentation](https://img.shields.io/badge/GitHub-Documentation-181717?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) [![Report a bug](https://img.shields.io/badge/GitHub-Report_a_bug-d73a4a?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

💖 Support development: [PayPal](https://www.paypal.com/paypalme/neworldcn) · [Afdian / 爱发电](https://afdian.com/a/xiaoyiluck)

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

The [API matrix](https://github.com/xiaoyiluck666/MeowTools/blob/main/gradle/paper-versions.properties) pins upstream builds. Checks compile source against each API and run regressions plus API member linkage checks against the **same baseline Java 21 plugin classes**. This does not replace multiplayer testing on a real server. New releases must pass the matrix before support is claimed; weekly CI detects missing Minecraft releases.

## ✨ What you can do

- **Moderate from one place:** inspect and edit online inventories, armor and offhand; vanish with a BossBar indicator and optional night vision; check player ping.
- **Make common commands easier:** bind F, Shift+F, and looking up/down with either key combination. Commands run with the player's own permissions and a configurable cooldown.
- **Tune survival gameplay:** accelerate detached leaf decay, configure permission-based XP multipliers, and control block damage and entity damage separately for TNT, creepers and other explosions.
- **Keep chat manageable:** customize join/quit messages, reduce repeated death messages and block selected commands.
- **Configure in your language:** Chinese/English messages, optional PlaceholderAPI inventory information, and configuration migration with a backup.

## 🧰 Commands

| Command | Purpose |
| --- | --- |
| `/mt help` | Localized help |
| `/mt ping [player]` | Check latency |
| `/mt inv [player]` | Inspect or edit an online inventory |
| `/mt vanish [player] [on\|off]` | Toggle vanish for yourself or another player |
| `/mt reload` | Reload configuration and language files |

`/meowtools` is the full command. Administrative tools default to OP; see the [English guide](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) for detailed permissions and configuration.

## 🧪 Validation

Six Paper versions passed 37 real-server protocol-client checks each. A short 20-client Paper 26.3 run, including 19 open inventory viewers, recorded an average of 2.29 ms/tick in that scenario. These are fixed local test results, not a player-capacity guarantee; 26.2/26.3 bots used protocol translation. The release report distinguishes the tested 1.1.0 baseline from 2.2.0 release checks and verifies identical plugin class files. [Read the evidence and limits](https://github.com/xiaoyiluck666/MeowTools/blob/main/reports/2.2.0/verification.md).

## Support and links

- [Source code](https://github.com/xiaoyiluck666/MeowTools) · [中文说明](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md)
- [Report a bug or request a feature](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose). Include the MeowTools version, exact Paper build, Java version, relevant configuration, logs and reproduction steps; remove credentials and private player data.
- [Download on Modrinth](https://modrinth.com/plugin/meowtools)

If MeowTools saves you administration time, support continued maintenance through [PayPal](https://www.paypal.com/paypalme/neworldcn) or [Afdian](https://afdian.com/a/xiaoyiluck).

<details>
<summary>简体中文介绍 / Chinese</summary>

# 🐾 MeowTools

**把常用管理工具和生存体验微调，集中到一个可配置的 Paper 插件中。**

实时检查背包、隐身管理、用 F 键执行常用命令，并按需调整落叶、经验和爆炸行为。只需安装在服务端，玩家无需客户端模组。

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/meowtools/versions) [![Documentation](https://img.shields.io/badge/GitHub-Documentation-181717?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md) [![Report a bug](https://img.shields.io/badge/GitHub-Report_a_bug-d73a4a?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

💖 支持项目开发：[PayPal](https://www.paypal.com/paypalme/neworldcn) · [爱发电](https://afdian.com/a/xiaoyiluck)

## 三步安装

1. [下载 MeowTools 2.2.0](https://modrinth.com/plugin/meowtools/versions)，确认 Paper 和 Java 版本符合下表。
2. 把 JAR 放入服务端的 `plugins` 目录，重启服务器。
3. 执行 `/mt help`；按需编辑 `plugins/MeowTools/config.yml`，管理员使用 `/mt reload` 重载。

默认语言为中文；改为 `language: en_us` 可使用英文。默认启用的 F 键快捷操作包含示例命令，开服前请按需修改或关闭。PlaceholderAPI 为可选依赖，用于背包信息中的占位符。

## ✨ 能帮你做什么

- **集中处理日常管理**：实时查看和编辑在线玩家背包、装备与副手，隐身巡查，查询延迟；隐身状态通过 BossBar 提示，可选夜视。
- **常用操作少输命令**：F、Shift+F、抬头或低头组合键均可绑定指令，使用玩家自身权限执行，并提供可配置冷却。
- **按需调整生存体验**：加快脱离树干的树叶衰减，按权限设置经验倍率，分别控制 TNT、苦力怕和其他爆炸的方块破坏与实体伤害。
- **让聊天信息更清楚**：自定义进退服提示，抑制重复死亡消息，屏蔽指定命令。
- **配置与语言统一管理**：中英文语言文件、可选 PlaceholderAPI 背包信息、带备份的配置迁移。

## 平台与兼容范围

同一个插件 JAR 支持以下已验证版本：

| 平台 | Minecraft 正式版 | 服务端 Java |
| --- | --- | --- |
| Paper | 1.21.10、1.21.11 | 21 |
| Paper | 26.1.1、26.1.2、26.2、26.3 | 25 |

- 仅针对 Paper 公共 API；不承诺 Spigot、Folia 或更早的 1.21 版本支持。Paper 没有 26.1 服务端构建。
- RC、预发布版本不在支持范围。Paper 为 26.1.1 提供的最后构建标为 ALPHA，26.3 的当前构建标为 BETA；这与 Minecraft 自身的正式版本状态不同。

具体 API 构建固定在 [版本矩阵](https://github.com/xiaoyiluck666/MeowTools/blob/main/gradle/paper-versions.properties)。每个版本均执行源码编译、故障回归以及插件实际字节码中的 API 成员链接检查；这些检查不能代替真实玩家客户端下的完整实服验收。未来发布的版本须加入矩阵后验证，CI 每周检查是否遗漏新正式版本。

## 🧰 常用命令

`/mt help` 查看帮助；`/mt ping [玩家]` 查询延迟；`/mt inv [玩家]` 实时编辑背包；`/mt vanish [玩家] [on|off]` 控制隐身；`/mt reload` 重载配置与语言文件。管理权限默认授予 OP。

## 🧪 验证记录

六个 Paper 版本各通过 37 项真实服务端协议客户端检查。Paper 26.3 的 20 客户端短时测试中，组合负载加 19 个背包窗口场景平均为 2.29 ms/tick。该结果来自固定本机场景，不能推断生产服人数；26.2/26.3 使用协议桥接机器人。报告明确区分 1.1.0 实服基线与 2.2.0 发布检查，并核验插件 class 文件一致。[查看证据与适用边界](https://github.com/xiaoyiluck666/MeowTools/blob/main/reports/2.2.0/verification.md)。

## 支持与反馈

- [GitHub 源码](https://github.com/xiaoyiluck666/MeowTools) · [English guide](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md)
- [提交 Bug 或功能建议](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)：请提供插件版本、具体 Paper 构建、Java 版本、相关配置、日志和复现步骤；提交前移除凭证与玩家隐私信息。
- [Modrinth 下载](https://modrinth.com/plugin/meowtools)

如果 MeowTools 帮你节省了管理时间，欢迎通过 [PayPal](https://www.paypal.com/paypalme/neworldcn) 或[爱发电](https://afdian.com/a/xiaoyiluck)支持持续维护。

</details>
