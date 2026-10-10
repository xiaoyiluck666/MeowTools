# MeowTools 2.2.0 release verification

Date: 2026-10-10. Release artifact: `MeowTools-2.2.0.jar` (70,112 bytes).

SHA-256: `f620f2096f324f4d5f65f4f6a795cb7c6105004121848b34ceff67ab87f28633`.

## Release checks

- `gradlew.bat --no-daemon --console=plain build checkCompatibility liveProbeJar`: passed. Each of six API environments ran 32 tests, with no failures, errors or skips (192 executions).
- `python scripts/check_paper_releases.py`: passed. All four 26.x Minecraft releases available through Paper are in the matrix: 26.1.1, 26.1.2, 26.2, 26.3. Paper has no 26.1 build.
- The plugin uses Java 21 class files (major 65) and `api-version: 1.21.10`. No Bukkit, JUnit, Mockito or live-test probe classes are packaged in the release JAR.
- A real Paper 26.3-169 server, Java 25, without protocol-translation plugins, enabled the final 2.2.0 artifact successfully. The smoke check passed with no server errors; the test server stopped afterwards. [Raw result](26.3-smoke.json).
- Six redundant test imports were removed. The release rebuild's 24 plugin class files are byte-for-byte identical to the previous tested artifact. The sole changed JAR entry is `plugin.yml`, carrying version 2.2.0. [Machine-readable checks](artifact-checks.json).

The published Modrinth version before this release used `2.1-SNAPSHOT`. The local build used `1.1.0`; this release aligns the package and public version as `2.2.0`. The integer configuration schema version remains independent.

## Real-server baseline evidence

The [full acceptance report](baseline/acceptance.md) retains the earlier **1.1.0** artifact's six-version, real Paper protocol-client tests: 37 checks per version, 222 passing checks total. That artifact's SHA-256 is `654b7434d95f00283dff43afbf4bef359266b7f68f32a0b9ebcbd5f5215a2e7c`. It includes the explosion damage and profile-request fixes carried into 2.2.0 unchanged.

The baseline also includes a short 20-client test on Paper 26.3. With combined load and 19 inventory windows, it averaged 2.29 ms/tick (P95 4.19 ms, P99 7.81 ms), with no ticks above 50 ms in that sample and all clients connected at the end. The other combined-load sample contained one tick above 50 ms. Raw metrics and the complete environment are in the report.

These historical checks were not rerun in full for the documentation, import and version update. Class-file equivalence establishes which plugin behavior was retained; release-specific testing above confirms the final package builds, passes the API matrix and enables on Paper 26.3.

## Limits and reproduction

26.2/26.3 functional clients used ViaVersion/ViaBackwards with the 1.21.11 protocol; this is not native graphical-client acceptance. The short local load test does not establish maximum player capacity, long-term memory behavior or performance gains against a controlled baseline. Production plugin combinations and native client visuals/movement remain to be checked.

Reproduction tools: [live-test guide](../../scripts/live/README.md), [pinned API matrix](../../gradle/paper-versions.properties), and [GitHub build workflow](../../.github/workflows/build.yml). The probe is a separate test-only JAR. Never use production worlds as test fixtures.

## 中文说明

本次发布完成 6 处未使用导入清理、双语介绍重写、赞助/反馈链接补齐和版本号统一；六组 API 各 32 项测试全部通过，最终包在不装协议桥接的 Paper 26.3 上成功启动。

历史实服验收的 JAR 为 1.1.0，222 项功能检查和 20 客户端短时压力测试的原始结果已保留。本次 2.2.0 的 24 个插件 class 文件与其完全一致，JAR 内仅 `plugin.yml` 版本号改变；不能把历史实服记录称为新版本重新完成的全量实服验收。完整环境、桥接方式与适用边界见上述报告。
