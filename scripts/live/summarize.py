"""Summarize retained real-server evidence; fail on incomplete or failed runs."""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET
import argparse
from run_live import release_jar

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "build/reports/live"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--artifact", type=Path, help="Exact tested JAR; defaults to the current build version")
args = parser.parse_args()
ARTIFACT = (args.artifact or release_jar()).resolve()
ARTIFACT_SHA = hashlib.sha256(ARTIFACT.read_bytes()).hexdigest()
VERSIONS = ["1.21.10", "1.21.11", "26.1.1", "26.1.2", "26.2", "26.3"]
EXPECTED_ERRORS = [
    "[MeowTools] Configuration could not be loaded safely",
    "[MeowTools] Reload failed",
    "org.bukkit.configuration.InvalidConfigurationException: while parsing a flow sequence",
    "org.yaml.snakeyaml.parser.ParserException: while parsing a flow sequence",
    "java.lang.IllegalStateException: Invalid MeowTools configuration",
]


def load(name):
    data = json.loads((REPORTS / name).read_text(encoding="utf-8"))
    assert not data.get("fatal"), (name, data.get("fatal"))
    assert data["cases"] and all(case["passed"] for case in data["cases"]), name
    assert not data["clientErrors"], (name, data["clientErrors"])
    assert data["pluginSHA256"] == ARTIFACT_SHA, (name, "Evidence does not match the selected JAR; rerun this suite")
    invalid_yaml_passed = any(case["passed"] and case["name"].startswith("Invalid YAML") for case in data["cases"])
    expected = []
    environment = []
    unexpected = []
    for index, line in enumerate(data["serverErrors"]):
        profile_timeout = "com.mojang.authlib.exceptions.MinecraftClientException: Failed to read from https://sessionserver.mojang.com/session/minecraft/profile/"
        if profile_timeout in line and "due to Read timed out" in line:
            environment.append(line)
        elif line == "Caused by: java.net.SocketTimeoutException: Read timed out" and index > 0 and profile_timeout in data["serverErrors"][index - 1]:
            environment.append(line)
        elif invalid_yaml_passed and any(pattern in line for pattern in EXPECTED_ERRORS):
            expected.append(line)
        else:
            unexpected.append(line)
    assert not unexpected, (name, unexpected)
    data["expectedErrorLines"] = expected
    data["environmentWarnings"] = environment
    data["unexpectedServerErrors"] = unexpected
    (REPORTS / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return data


rows = []
total = 0
unit_counts = []
for task in ["test", "testPaper1_21_11", "testPaper26_1_1", "testPaper26_1_2", "testPaper26_2", "testPaper26_3"]:
    suites = [ET.parse(p).getroot() for p in (ROOT / "build/test-results" / task).glob("TEST-*.xml")]
    assert suites and all(int(s.get("failures", 0)) == 0 and int(s.get("errors", 0)) == 0 and int(s.get("skipped", 0)) == 0 for s in suites)
    unit_counts.append(sum(int(s.get("tests", 0)) for s in suites))
assert len(set(unit_counts)) == 1
for version in VERSIONS:
    data = load(version + "-functional.json")
    assert not data["environmentWarnings"], (version, "Avatar lookup warning reappeared after profile fix")
    total += len(data["cases"])
    mode = "1.21.11 协议，经 ViaVersion/ViaBackwards 桥接" if data["bridge"] else "原生协议"
    rows.append(f"| {version} | {data['paperBuild']} | {mode} | {len(data['cases'])} / {len(data['cases'])} | [{version}-functional.json]({version}-functional.json) |")
smokes = [load(version + "-smoke.json") for version in ["26.2", "26.3"]]
perf = load("26.3-performance.json")
assert not perf["environmentWarnings"], "Avatar lookup warning reappeared during stress"
assert len(perf["performance"]) == 3
assert all(p["players"] == 20 and p["seconds"] >= 60 and p["ticks"] > 0 for p in perf["performance"])
assert perf["performance"][2]["inventoryViews"] == 19
assert all(p["pickupCallbacks"] > 0 and p["mergeCallbacks"] > 0 for p in perf["performance"][1:])
performance_rows = []
labels = ["20 客户端空闲", "20 客户端组合负载", "组合负载 + 19 个背包查看窗口"]
for label, p in zip(labels, perf["performance"]):
    performance_rows.append(f"| {label} | {p['seconds']:.2f} | {p['observedTPS']:.2f} | {p['meanMSPT']:.2f} | {p['p95MSPT']:.2f} | {p['p99MSPT']:.2f} | {p['maxMSPT']:.2f} | {p['ticksOver50ms']} |")
details = []
for label, p in zip(labels, perf["performance"]):
    latency = f"{p['pingP95ms']}ms" if p["pingP95ms"] is not None else "未采样（无请求）"
    details.append(f"- {label}：{p['ticks']} ticks，{p['requests']} 次 ping 请求 / {p['pingResponses']} 次响应，ping 响应 P95 为 {latency}；拾取回调 {p['pickupCallbacks']}，合并回调 {p['mergeCallbacks']}；末尾经验球 {p['orbsAtEnd']}，堆占用 {p['usedHeapMiB']:.1f}MiB，进程 CPU 时间 {p['processCPUSeconds']:.2f}s。")
sha = ARTIFACT_SHA
text = f"""# {ARTIFACT.stem} 实服验收与压力测试

真实 Paper 服务端、真实 TCP 协议机器人客户端；不使用模拟 Bukkit 服务器。所有版本使用同一份发布 JAR `{ARTIFACT.name}`，SHA-256：`{sha}`。各轮时间见原始 JSON。

## 功能结果

六个版本各 37 项检查，共 {total} 项全部通过，客户端无非预期断开，未发现非预期的插件运行异常。26.2/26.3 还各自通过未安装协议桥接插件时的真实服务端启动检查（测试探针仍在场）。Paper 构建来自官方 Fill API，下载哈希已校验；具体来源及哈希在每个 JSON 中。

| Minecraft 正式版 | Paper 构建 | 客户端方式 | 通过 / 总数 | 原始结果 |
| --- | --- | --- | --- | --- |
{chr(10).join(rows)}

每轮四个客户端，覆盖以下行为：

- 本地化帮助、ping、权限拒绝、命令屏蔽及 OP 绕过；未知语言回退和无效 YAML 重载后的恢复。
- 两名管理员同时编辑同一玩家背包：左右键、堆叠拆分、Shift 取出、数字键、装备和副手；禁止拖拽、双击收集、装饰提取、错误装备和从下方 Shift 推入；关闭不会覆盖玩家新获得的物品，目标退出自动关闭并回收任务。
- 隐身重复开启、可见性、夜视、连续重载、关闭、退出重连和禁用清理。
- 客户端 F 包触发命令、禁用方向快捷键后的回退、八个连续包只执行一次。
- 两个堆叠经验球总经验 31：默认得到 31、2 倍得到 62、0 倍得到 0；取消拾取保留全部经验球，取消合并仍得到 31；20 点原版经验将修补工具损伤从 100 降至 60，剩余经验为 0。
- 随机 tick 关闭时快速落叶，保留持久叶和被取消衰减的叶；真实 TNT、苦力怕及其他爆炸的方块/伤害保护；重复死亡消息只显示第一次。

无效 YAML 是故意注入的失败场景。该场景的配置加载和重载错误及其调用栈属于预期；全部原始错误行保留在 JSON 的 `serverErrors`，另列 `unexpectedServerErrors`，未将未知错误自动忽略。

修复后的离线机器人功能测试和压力测试未再出现 Mojang 头像请求超时或限流警告。在线正版纹理的图形显示不属于本轮通过的功能结论。

## 实服发现并修复的问题

Paper 26.2 的无来源 `World#createExplosion` 会将伤害原因报告为 `CUSTOM`。旧代码只识别 `ENTITY_EXPLOSION` / `BLOCK_EXPLOSION`，因此在关闭“其他爆炸伤害”时仍造成了 43 点伤害和玩家死亡。机器人自动重生会让只检查最终血量的测试漏报；增加真实伤害、爆炸和死亡事件观测后复现，证据保存在 [修复前记录](before-explosion-fix/26.2-explosion.json)。

已补充公共 `DamageSource` 的标准爆炸类型识别，并在普通事件中通过直接来源实体区分 TNT/苦力怕设置；普通自定义伤害及其他命名空间的同名类型不受影响。实服爆炸用例同时检查爆炸确实发生、伤害事件明确取消、无死亡及方块保留。[修复差异](explosion-fix.diff)。实现依据：[Paper DamageType](https://jd.papermc.io/paper/1.21.10/org/bukkit/damage/DamageType.html)。

压力测试还发现：无纹理的离线玩家头像在每次更新 lore 时触发 Paper 自动补全资料，向 Mojang 重复请求，出现 537 次 HTTP 429 限流响应及少量超时。旧实现虽然复用头像对象，仍会在设置 meta 时重新触发资料补全。现在只使用在线玩家已经具备的纹理资料；没有纹理则显示默认头像，不设置待补全的 owner。[修复前压力记录](before-profile-fix/26.3-performance.json)、[头像修复差异](profile-fix.diff)。

为两个问题新增五项故障回归，六组 API 环境各 {unit_counts[0]} 项通过，共 {sum(unit_counts)} 次执行。最终 JAR 已重新执行全部实服功能场景和压力场景。

## 20 客户端压力结果

在 Paper 26.3-169、Java 25.0.2 上采样。主机 Windows 11、Intel Core Ultra 7 265K、约 32GiB RAM；JVM `-Xms1G -Xmx2G -XX:ActiveProcessorCount=4`，平坦世界，视距与模拟距离均为 3。机器人、服务端、测试探针和协议桥接插件运行在同一台日常使用的主机上。

连接 20 个客户端后预热 15 秒，各阶段再预热 5 秒，连续三个阶段各采样约 60 秒。组合负载为每秒 19 个 ping 与 19 个 F 请求、目标背包每 tick 修改一格，每五秒生成 570 个经验球并执行 100 次服务端方块破坏操作；第三阶段保持 19 个管理员背包查看窗口。具体负载实现见 [测试说明](../../../scripts/live/README.md)。

| 场景 | 秒 | 实际 TPS | 平均 MSPT | P95 MSPT | P99 MSPT | 最大 MSPT | >50ms ticks |
| --- | --- | --- | --- | --- | --- | --- | --- |
{chr(10).join(performance_rows)}

{chr(10).join(details)}

压测结束 20 个客户端均保持连接。原始指标见 [26.3-performance.json](26.3-performance.json)，每个场景记录整服 tick 耗时；CPU、堆占用均包含 Paper、桥接和探针，不能解释为 MeowTools 独占成本。

## 结论边界与复现

这组证据确认当前插件在上述真实服务端和固定协议场景下可用。26.2/26.3 使用桥接的 1.21.11 客户端，尚未完成这两版原生图形客户端与真人操作的验收；桥接机器人保持静止，不能验证新版移动协议。未安装 WorldGuard、LuckPerms、PlaceholderAPI 等生产依赖组合，取消事件由测试探针提供。

本次是单机、固定场景、短时压力测试，未测容量极限、长时间内存趋势、真实生存世界探索/农场或跨机器网络延迟，未做多轮、受控的优化前后对照，不能据此承诺生产服人数或 TPS 提升。后续重点是生产插件组合、原生客户端视觉/移动交互和长时间运行。

复现入口见 [scripts/live/README.md](../../../scripts/live/README.md)。服务器日志位于 `build/live-servers/<版本>/console-<套件>.log`，服务器与机器人已在测试结束后停止。协议测试工具的 RCON 分包、26.3 默认白名单/死亡消息规则以及桥接后的移动猜测问题已在测试夹具中处理；测试区域提供固定地面，避免把浮空夹具当作正常游戏环境。
"""
(REPORTS / "acceptance.md").write_text(text, encoding="utf-8")
print(f"Verified {total} functional cases across {len(VERSIONS)} real Paper versions; 20-client performance report written.")
