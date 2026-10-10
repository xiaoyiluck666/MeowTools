# 🐾 MeowTools

**Vanish, edit inventories, and put your server commands on the F key.**

A server-side toolkit for **Paper survival and community servers**. Give your staff everyday moderation tools and your players convenient shortcuts, with configurable leaf decay, experience and explosion settings in the same plugin. **Players need no client mod.**

**[⬇ Download MeowTools](https://modrinth.com/plugin/meowtools/versions)** · [English guide](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) · [中文文档](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md) · [Report a bug](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

**One JAR:** Paper **1.21.10 / 1.21.11** with Java 21, and **26.1.1 / 26.1.2 / 26.2 / 26.3** with Java 25.

## ✨ Spend less time on everyday server tasks

| You want to… | MeowTools helps you… |
| --- | --- |
| Inspect a player's items | Open a live inventory with `/mt inv Alex`, including armor and offhand. Edit slots directly, even with multiple staff viewing. |
| Moderate while hidden | Use `/mt vanish on`, with permission-based visibility, a BossBar indicator and optional night vision. |
| Stop typing the same commands | Bind **F**, **Shift+F**, and looking up/down with either combination to commands such as your server's spawn, home or menu commands. |
| Tune survival gameplay | Speed up detached leaf decay, set XP multipliers by permission, and merge XP orbs while preserving total experience and vanilla Mending. |
| Control explosion damage | Configure block destruction and entity damage separately for **TNT, creepers and other explosions**. |
| Keep chat manageable | Customize join/quit messages, suppress repeated death messages, block selected commands and check player ping. |

Shortcuts run as the player, using their permissions and a configurable cooldown. Spawn, home and menu commands come from your server or other plugins. Chinese and English messages are included; PlaceholderAPI is optional for inventory information.

## 🚀 Start in three steps

1. **[Download the JAR](https://modrinth.com/plugin/meowtools/versions)** for a Paper version listed above.
2. Put it in your server's **`plugins`** folder and restart the server.
3. Run **`/mt help`**, then customize `plugins/MeowTools/config.yml`. Administrators can apply changes with `/mt reload`.

Set `language: en_us` for English; the default is Chinese. Before opening the server to players, customize or disable the enabled F-key example commands. Administrative permissions default to OP.

## 🧰 Commands at a glance

| Command | What it does |
| --- | --- |
| `/mt help` | Show localized help |
| `/mt ping [player]` | Check your own or another player's latency |
| `/mt inv [player]` | View and edit an online player's inventory |
| `/mt vanish [player] [on\|off]` | Control vanish for yourself or another player |
| `/mt reload` | Reload configuration and language files |

The full command is `/meowtools`. See the [English guide](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) for permissions, inventory controls and configuration examples.

## ✅ Compatibility and validation

| Server | Minecraft release | Java |
| --- | --- | --- |
| Paper | 1.21.10, 1.21.11 | 21 |
| Paper | 26.1.1, 26.1.2, 26.2, 26.3 | 25 |

The release passed **192 API test executions across six environments**. Real-server baseline evidence includes **222 functional checks** and short **20-client load scenarios**. [Read the validation report](https://github.com/xiaoyiluck666/MeowTools/blob/main/reports/2.2.0/verification.md) for tested artifacts, environments and limits; 26.2/26.3 functional bots used protocol translation. These results do not establish a maximum player count.

Support covers the listed Minecraft releases. Spigot, Folia, earlier 1.21 versions and pre-releases are outside the supported scope. Paper has no 26.1 server build; Paper's ALPHA/BETA build channels are separate from Minecraft release status. Future versions require verification.

## 💬 Get help and support development

- 📖 [English documentation](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_EN.md) · [中文文档](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md)
- 🐛 [Report a bug or suggest a feature](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)
- 💻 [Source code](https://github.com/xiaoyiluck666/MeowTools)

If MeowTools makes your server easier to run, support continued maintenance through **[PayPal](https://www.paypal.com/paypalme/neworldcn)** or **[Afdian / 爱发电](https://afdian.com/a/xiaoyiluck)**.

<details>
<summary>简体中文介绍 / Chinese</summary>

# 🐾 MeowTools

**隐身巡查、实时改背包，把常用指令绑到 F 键。**

面向 **Paper 生存服与社区服**的服务端工具箱：为管理员提供日常管理工具，为玩家提供快捷操作，同时按需调整落叶、经验和爆炸行为。**玩家无需安装客户端模组。**

**[⬇ 下载 MeowTools](https://modrinth.com/plugin/meowtools/versions)** · [中文文档](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md) · [提交 Bug](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

**一个 JAR：**Paper **1.21.10 / 1.21.11** 使用 Java 21，**26.1.1 / 26.1.2 / 26.2 / 26.3** 使用 Java 25。

## ✨ 日常开服，少一些重复操作

| 你想做什么 | MeowTools 能帮你做什么 |
| --- | --- |
| 检查和整理玩家物品 | `/mt inv Alex` 实时打开在线玩家背包，包含装备与副手；直接编辑槽位，支持多名管理员同时查看。 |
| 隐身巡查服务器 | `/mt vanish on` 开启隐身，按权限控制可见性，并提供 BossBar 状态提示与可选夜视。 |
| 少输常用指令 | 把服务器已有的出生点、家或菜单指令绑定到 **F、Shift+F、抬头/低头组合键**。 |
| 调整生存体验 | 加快脱离树干的树叶衰减，按权限设置经验倍率；经验球合并保留总经验和原版经验修补行为。 |
| 分别控制爆炸影响 | 为 **TNT、苦力怕及其他爆炸**分别配置方块破坏与实体伤害。 |
| 让聊天更清楚 | 自定义进退服消息、抑制重复死亡消息、屏蔽指定命令，并查询玩家延迟。 |

快捷指令以玩家身份和权限执行，支持冷却；出生点、家和菜单指令由服务器或其他插件提供。内置中英文语言文件，PlaceholderAPI 可选，用于背包信息展示。

## 🚀 三步开始使用

1. **[下载 JAR](https://modrinth.com/plugin/meowtools/versions)**，确认 Paper 和 Java 版本符合上表。
2. 放入服务端 **`plugins`** 目录，重启服务器。
3. 执行 **`/mt help`**，按需编辑 `plugins/MeowTools/config.yml`；管理员使用 `/mt reload` 应用更改。

默认使用中文。开服前请修改或关闭默认启用的 F 键示例命令。管理权限默认授予 OP；完整命令、权限和配置见[中文文档](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md)。

## ✅ 有验证记录，兼容范围明确

本次发布通过六组环境共 **192 次 API 测试**；实服基线保留了 **222 项功能检查**与 **20 客户端短时负载测试**。[查看验证报告](https://github.com/xiaoyiluck666/MeowTools/blob/main/reports/2.2.0/verification.md)，了解对应产物与测试边界；26.2/26.3 功能机器人使用协议桥接，测试结果不代表生产服承载人数。

仅支持上方列出的 Minecraft 正式版本。Spigot、Folia、更早的 1.21 与预发布版本不在支持范围；Paper 没有 26.1 服务端构建。Paper 的 ALPHA/BETA 构建渠道与 Minecraft 正式版状态不同，未来版本须验证后再加入。

## 💬 反馈与支持

- 📖 [中文文档](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md)
- 🐛 [提交 Bug 或功能建议](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)
- 💻 [GitHub 源码](https://github.com/xiaoyiluck666/MeowTools)

如果 MeowTools 让你开服更省心，欢迎通过 **[PayPal](https://www.paypal.com/paypalme/neworldcn)** 或**[爱发电](https://afdian.com/a/xiaoyiluck)**支持持续维护。

</details>
