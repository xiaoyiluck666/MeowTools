# Changelog

## 2.2.0

One JAR now supports Paper 1.21.10, 1.21.11, 26.1.1, 26.1.2, 26.2 and 26.3. Use Java 21 for the 1.21 versions and Java 25 for 26.x.

- Fix inventory editing so closing a viewer cannot overwrite newer items; validate equipment, slot transfers and simultaneous viewers.
- Preserve stacked XP totals and vanilla pickup/Mending behavior when merging orbs; respect cancelled events and differing sources.
- Bound and deduplicate leaf-decay work, avoid loading unloaded chunks, and preserve persistent leaves.
- Back up and migrate old configuration while retaining user values and custom keys; preserve malformed files.
- Clean up vanish resources and cache shortcut/command settings with a configurable shortcut cooldown.
- Fix explosion protection for Paper damage events reported as `CUSTOM`, using standard public damage types.
- Stop repeated Mojang profile requests for inventory heads without available skin textures.
- Remove six unused test imports; add bilingual documentation, sponsor links and structured bug/feature feedback.

Validation: 32 tests in each of six API environments; real-server baseline evidence contains 222 functional checks and short 20-client pressure scenarios. The release report identifies the baseline artifact, class-file equivalence and release-specific checks. Only the listed Minecraft releases are supported; Spigot, Folia and pre-releases are outside this release's scope.

### 中文

一个 JAR 支持 Paper 1.21.10、1.21.11、26.1.1、26.1.2、26.2、26.3。1.21 版本服务端使用 Java 21，26.x 使用 Java 25。

- 修复背包界面关闭时的旧快照覆盖问题，支持实时槽位编辑并校验多人查看、物品转移与装备操作。
- 经验球合并保留堆叠总经验、来源和取消事件，由原版处理拾取与经验修补。
- 落叶队列增加去重、容量和每 tick 预算，不加载已卸载区块，保留持久叶。
- 配置迁移保留用户值与自定义键，备份原文件，不覆盖无效 YAML。
- 清理隐身资源，缓存快捷键与命令设置，提供快捷操作冷却。
- 修复 Paper 将部分爆炸伤害标为 `CUSTOM` 时漏拦截的问题。
- 修复无纹理头像在背包刷新时反复查询 Mojang 导致超时和限流的问题。
- 清理六处测试未使用导入，重写双语介绍，补齐赞助和 Bug/建议反馈链接。

验证覆盖六组 API 各 32 项测试；实服基线包含 222 项功能检查和 20 客户端短时压测。发布报告区分历史基线与本次发布检查，并核对 class 文件一致性。兼容范围仅限列出的正式版本。
