# 🐾 MeowTools

**隐身巡查、实时改背包，把常用指令绑到 F 键。**

面向 Paper 生存服与社区服的服务端工具箱：为管理员提供实时背包编辑与隐身，为玩家提供常用指令快捷操作，并按需调整落叶、经验和爆炸行为。玩家无需安装客户端模组。

[![Modrinth](https://img.shields.io/badge/Modrinth-Download-1bd96a?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/meowtools/versions) [![Documentation](https://img.shields.io/badge/GitHub-Documentation-181717?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/blob/main/README_CN.md) [![Report a bug](https://img.shields.io/badge/GitHub-Report_a_bug-d73a4a?style=flat-square&logo=github)](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)

**[⬇ 下载 MeowTools](https://modrinth.com/plugin/meowtools/versions)** — 一个 JAR 支持下方列出的 Paper 正式版本。想快速了解功能，可先看[项目介绍](README.md)。

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

具体 API 构建固定在 [版本矩阵](gradle/paper-versions.properties)。每个版本均执行源码编译、故障回归以及插件实际字节码中的 API 成员链接检查；这些检查不能代替真实玩家客户端下的完整实服验收。未来发布的版本须加入矩阵后验证，CI 每周检查是否遗漏新正式版本。

## 功能与命令

- `/mt help`：本地化帮助。
- `/mt ping [玩家]`：查询延迟。
- `/mt inv [玩家]`：实时查看和编辑在线玩家的背包、装备、副手。
- `/mt vanish [玩家] [on|off]`：隐身、BossBar 提示、可选夜视。
- `/mt reload`：安全迁移配置并重载功能设置。
- 可配置快捷键：F、Shift+F、抬头/低头与 F 的组合，以玩家身份执行指令。
- 进退服信息、死亡消息防刷屏、按来源控制爆炸破坏和伤害。
- 快速落叶：忽略持久树叶，延迟等待树叶距离更新，队列去重、容量和每 tick 预算限制，不加载已卸载区块。
- 经验倍率：动态数值权限优先；否则按配置顺序选择权限组倍率，最后使用默认倍率。
- 经验球合并：保留堆叠总经验，仅合并无自定义持久数据、来源一致的球，尊重合并取消事件；拾取、经验修补和经验事件继续由服务器处理。
- 命令屏蔽；可选的命令级权限诊断（默认关闭，不检测其他插件内部任意子命令权限）。

背包编辑直接操作真实槽位，支持左/右键、数字键，以及从目标背包 Shift 点击取出物品。上方界面拖拽、从自身背包 Shift 点击推入、双击收集和创造模式复制被拦截；普通点击自身背包不受影响。关闭界面不会把旧副本覆盖回目标。装备槽只接受对应装备。

背包头像复用服务器已有的皮肤纹理；没有纹理时显示默认头像，不向 Mojang 主动补全玩家资料。

## 配置与权限

配置位置：`plugins/MeowTools/config.yml`。语言文件位于 `plugins/MeowTools/lang/`，支持 `zh_cn`、`en_us`；未知语言回退为英文。

`config-version` 是整数结构版本，与插件发布版本分开。旧 `version: 1.1.0` 配置自动迁移，保留用户值和自定义键；迁移前原文件保存到唯一的 `config.backup.*.yml`，新文件通过临时文件替换。无效 YAML 不会被覆盖。

落叶配置路径是 `server_settings.fast_leaf_decay`：`max_decay_per_tick` 限制为 1–1000，`max_queue_size` 为 1–100000。经验球半径限制为 0–4，每次最多合并 0–128 个球；经验倍率接受有限的 0–1000 倍，0 倍有效，显式拒绝的权限节点不会生效。快捷键通过 `shortcuts.cooldown_millis` 控制冷却（默认 250 毫秒）。

| 权限 | 默认 | 用途 |
| --- | --- | --- |
| `meowtools.use` | 所有人 | 基础命令与快捷键 |
| `meowtools.ping` | 所有人 | 延迟查询 |
| `meowtools.admin` | OP | 重载配置 |
| `meowtools.inv` | OP | 查看与编辑背包 |
| `meowtools.vanish` | OP | 自身隐身 |
| `meowtools.vanish.other` | OP | 操作他人隐身 |
| `meowtools.vanish.see` | OP | 查看隐身玩家 |
| `meowtools.bypass.blocked_commands` | OP | 绕过命令屏蔽 |

`meowtools.exp.<数值>` 取已授权动态数值中的最大值；`meowtools.exp.<配置组名>` 使用配置顺序。PlaceholderAPI 为可选依赖。

## 构建与验证

安装 JDK 21；完整兼容检查还需要 JDK 25。Gradle Wrapper 自动检测本机 JDK；若无法发现，可通过 Gradle 的 `org.gradle.java.installations.paths` 指定安装位置。

```powershell
.\gradlew.bat build
.\gradlew.bat checkCompatibility
python scripts/check_paper_releases.py
```

Linux/macOS 对应 `bash ./gradlew build checkCompatibility`。产物：`build/libs/MeowTools-2.2.0.jar`；测试报告：`build/reports/tests/`。本地开发可用 `gradlew runServer` 启动 Paper 1.21.10 测试环境（运行前自行阅读服务器许可）。

可复现的真实 Paper 多客户端验收和 20 客户端压力测试见 [实服测试说明](scripts/live/README.md)。测试探针单独构建，不进入发布 JAR；原始结果保存在 `build/reports/live/`。26.2/26.3 的机器人通过协议桥接连接，尚需原生图形客户端及生产插件组合的验收。

上线前在独立测试服检查：两名管理员同时查看同一背包、玩家拾取/死亡/退出、各种装备操作；开关夜视后重连；经验农场中的修补和倍率、其他插件取消事件；保护区落叶和爆炸，以及连续重载。不使用生产世界做兼容测试。

公开验证证据及其适用边界见 [发布验证报告](reports/2.2.0/verification.md)。

## 支持与反馈

- [GitHub 源码](https://github.com/xiaoyiluck666/MeowTools) · [English guide](README_EN.md)
- [提交 Bug 或功能建议](https://github.com/xiaoyiluck666/MeowTools/issues/new/choose)：请提供插件版本、具体 Paper 构建、Java 版本、相关配置、日志和复现步骤；提交前移除凭证与玩家隐私信息。
- [Modrinth 下载](https://modrinth.com/plugin/meowtools)

如果 MeowTools 帮你节省了管理时间，欢迎通过 [PayPal](https://www.paypal.com/paypalme/neworldcn) 或[爱发电](https://afdian.com/a/xiaoyiluck)支持持续维护。
