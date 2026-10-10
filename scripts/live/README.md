# 真实 Paper 测试

本工具启动独立 Paper 实例，通过真实 TCP 客户端测试命令、GUI 点击、快捷键和退出/重连。独立的 `MeowToolsLiveProbe` 负责布置世界、物品、经验球、权限和事件取消条件，读取服务器权威状态，并采集 `ServerTickEndEvent` 耗时。它不进入发布 JAR，也不使用 NMS。

测试目录固定为项目内的 `build/live-servers/<版本>`，端口固定为本机 `127.0.0.1:25585/25586`；端口已占用则退出。离线登录仅用于此隔离环境。脚本结束时停止服务器，保留日志和世界供复核。

需要 JDK 21/25、Node 22+、Python，以及下载 Paper 与测试依赖的网络连接。Windows 默认 JDK 路径见 `run_live.py`。

```powershell
New-Item -ItemType Directory -Path build/live-tools -Force
Copy-Item scripts/live/package*.json build/live-tools/
npm.cmd ci --prefix build/live-tools --no-audit --no-fund
.\gradlew.bat build liveProbeJar
python scripts/live/run_live.py --version 26.1.2 --suite functional --accept-eula
python scripts/live/run_live.py --version 26.3 --bridge --suite functional --accept-eula
python scripts/live/run_live.py --version 26.3 --bridge --suite performance --accept-eula
```

运行前阅读 <https://aka.ms/MinecraftEULA>；`--accept-eula` 表示运行者确认同意。只启动不连接客户端的检查使用 `--suite smoke`。`--suite death`、`--suite explosion`、`--suite xp` 提供局部诊断。完整汇总需要六个版本各执行 functional、26.2/26.3 各执行未安装桥接的 smoke，以及 26.3 的 performance，最后运行 `python scripts/live/summarize.py`；汇总器会检查用例、客户端错误、日志及产物哈希后生成 `build/reports/live/acceptance.md`。

Minecraft-data/mineflayer 当前支持到 26.1；26.1.1、26.1.2 和 1.21.x 使用原生协议客户端。26.2/26.3 的 `--bridge` 安装固定的 ViaVersion/ViaBackwards 5.12.2-SNAPSHOT 测试构建（Jenkins 1474/801），客户端使用 1.21.11 协议。桥接客户端保持静止，确认服务器传送，但不采用库无法正确模拟的新版移动状态；朝向、F 和 GUI 操作仍发送明确协议包。不能把桥接测试称为新版原生图形客户端验收。

运行器从 `build.gradle.kts` 读取当前版本并部署对应 JAR，避免版本更新后误测旧包。汇总器默认要求所有证据与当前 JAR 的哈希一致；复核历史结果可传入 `--artifact <历史JAR路径>`。不能把新版本的一轮 smoke 和旧版本功能/压测混合汇总为新版本完整验收。

功能检查包含四个客户端；世界操作和事件取消由探针布置，背包和命令由客户端发送。区域有固定地面，经验用例与其他客户端隔离，经验球固定初始速度以保证总量可断言。爆炸用例同时观察实际爆炸、伤害取消和死亡事件，避免自动重生导致仅凭血量漏报。压力检查包含 20 个客户端，先预热，再各采样 60 秒：空闲、组合负载、组合负载加 19 个背包查看窗口。组合负载每五秒给 19 名负载客户端各生成 30 个经验球，产生 100 次真实服务端方块破坏操作，每秒向 19 个客户端各发送一次 ping 和 F，目标背包每 tick 改变一格。不同阶段的实际 tick 数与请求数记录在 JSON 中。

采集整服平均/分位 MSPT、实际 tick 速率、超过 50ms 的 tick 数、进程 CPU 时间和采样末堆占用。探针、桥接、机器人与服务端运行在同一台机器；视距/模拟距离均为 3、平坦世界、服务端堆上限 2GiB，JVM ActiveProcessorCount 为 4。数据用于检查这一固定场景的可用性，不能作为真实生存服承载人数或优化前后收益的证明。

结果为 `build/reports/live/<版本>-<套件>.json`，控制台日志位于对应测试服目录。功能用例有意写入无效 YAML 并触发重载失败，因此预期会产生配置错误日志；必须结合用例和调用栈复核，不能把全部 ERROR 简单视为插件运行崩溃，也不能忽略无关错误。修复前的 Paper/Authlib 头像超时和限流记录保留在 `before-profile-fix/`；汇总器要求修复后的功能和压力场景没有头像请求错误。在线正版头像纹理的图形显示不纳入通过结论。桥接更新检查关闭，避免第三方更新请求干扰固定构建的测试。
