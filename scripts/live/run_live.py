"""Run an isolated loopback-only Paper server and real protocol client acceptance.

Requires liveProbeJar, Node 22+, dependencies installed in build/live-tools, JDKs 21/25.
Never points at production files. --accept-eula is explicit acknowledgement by the operator.
"""
import argparse
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import socket
import subprocess
import time
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
LIVE = ROOT / "build/live-servers"
HEADERS = {"User-Agent": "MeowTools-live-validation/1.1"}


def release_jar():
    version = re.search(r'^version = "([^"]+)"', (ROOT / "build.gradle.kts").read_text(encoding="utf-8"), re.MULTILINE)
    if not version:
        raise ValueError("Could not determine the plugin release version")
    jar = ROOT / "build/libs" / f"MeowTools-{version.group(1)}.jar"
    if not jar.is_file():
        raise FileNotFoundError(f"Build the plugin first: {jar}")
    return jar


def fetch(url):
    with urlopen(Request(url, headers=HEADERS), timeout=90) as response:
        return response.read()


def prepare(version, bridge, accept):
    if not re.fullmatch(r"(?:26|1\.21)\.\d+(?:\.\d+)?", version):
        raise ValueError("Expected a supported release version, not a filesystem path")
    folder = LIVE / version
    assert folder.resolve().is_relative_to(LIVE.resolve())
    folder.mkdir(parents=True, exist_ok=True)
    metadata = folder / "download.json"
    if metadata.exists():
        build = json.loads(metadata.read_text(encoding="utf-8"))
    else:
        build = json.loads(fetch(f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds"))[0]
        metadata.write_text(json.dumps(build, indent=2), encoding="utf-8")
    download = build["downloads"]["server:default"]
    jar = folder / "paper.jar"
    if not jar.exists():
        jar.write_bytes(fetch(download["url"]))
    assert hashlib.sha256(jar.read_bytes()).hexdigest() == download["checksums"]["sha256"]
    plugins = folder / "plugins"
    plugins.mkdir(exist_ok=True)
    shutil.copy2(release_jar(), plugins / "MeowTools.jar")
    shutil.copy2(ROOT / "build/live-tools/MeowToolsLiveProbe.jar", plugins / "MeowToolsLiveProbe.jar")
    if bridge:
        for job, number, name in [("ViaVersion", 1474, "ViaVersion-5.12.2-SNAPSHOT.jar"), ("ViaBackwards", 801, "ViaBackwards-5.12.2-SNAPSHOT.jar")]:
            cached = ROOT / "build/live-tools" / name
            if not cached.exists():
                cached.write_bytes(fetch(f"https://ci.viaversion.com/job/{job}/{number}/artifact/build/libs/{name}"))
            shutil.copy2(cached, plugins / name)
        via_config = plugins / "ViaVersion/config.yml"
        via_config.parent.mkdir(exist_ok=True)
        via_text = via_config.read_text(encoding="utf-8") if via_config.exists() else "check-for-updates: true\n"
        via_config.write_text(via_text.replace("check-for-updates: true", "check-for-updates: false"), encoding="utf-8")
    else:
        disabled = folder / "disabled-test-plugins"
        disabled.mkdir(exist_ok=True)
        for name in ["ViaVersion-5.12.2-SNAPSHOT.jar", "ViaBackwards-5.12.2-SNAPSHOT.jar"]:
            source = plugins / name
            if source.exists():
                source.replace(disabled / name)
    if not accept:
        raise SystemExit("Pass --accept-eula after reviewing https://aka.ms/MinecraftEULA")
    (folder / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (folder / "server.properties").write_text('''server-ip=127.0.0.1
server-port=25585
online-mode=false
white-list=false
enforce-whitelist=false
enforce-secure-profile=false
level-name=live-test-world
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
view-distance=3
simulation-distance=3
max-players=40
spawn-protection=0
difficulty=peaceful
enable-rcon=true
rcon.port=25586
rcon.password=meowtools-local-test-only
broadcast-rcon-to-ops=false
sync-chunk-writes=false
allow-flight=true
network-compression-threshold=-1
pause-when-empty-seconds=-1
''', encoding="utf-8")
    return folder, build


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--bridge", action="store_true")
    parser.add_argument("--suite", choices=["smoke", "functional", "performance", "death", "explosion", "xp"], default="functional")
    parser.add_argument("--accept-eula", action="store_true")
    args = parser.parse_args()
    for port in [25585, 25586]:
        with socket.socket() as sock:
            if sock.connect_ex(("127.0.0.1", port)) == 0:
                raise SystemExit(f"Test port {port} already in use; refusing to reuse another server")
    folder, build = prepare(args.version, args.bridge, args.accept_eula)
    java = Path("C:/Program Files/Java") / ("jdk-21" if args.version.startswith("1.") else "jdk-25.0.2") / "bin/java.exe"
    console = folder / f"console-{args.suite}.log"
    result = ROOT / "build/reports/live" / f"{args.version}-{args.suite}.json"
    result.parent.mkdir(parents=True, exist_ok=True)
    with console.open("w", encoding="utf-8") as log:
        proc = subprocess.Popen([str(java), "-Xms1G", "-Xmx2G", "-XX:ActiveProcessorCount=4", "-Dfile.encoding=UTF-8", "-jar", "paper.jar", "--nogui"], cwd=folder, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT)
        suite_failed = False
        try:
            deadline = time.monotonic() + 240
            while time.monotonic() < deadline:
                content = console.read_text(encoding="utf-8", errors="replace")
                if 'Done (' in content:
                    break
                if proc.poll() is not None:
                    raise RuntimeError("Server exited before ready:\n" + content[-8000:])
                time.sleep(.5)
            else:
                raise TimeoutError("Paper startup timed out; see " + str(console))
            print(f"READY: Paper {args.version} build {build['id']}; suite={args.suite}; bridge={args.bridge}", flush=True)
            env = os.environ.copy()
            env.update(LIVE_VERSION=args.version, LIVE_BRIDGE=str(args.bridge).lower(), LIVE_SUITE=args.suite, LIVE_RESULT=str(result))
            completed = subprocess.run(["node", str(ROOT / "scripts/live/clients.cjs")], cwd=ROOT, env=env, timeout=900)
            suite_failed = completed.returncode != 0
        finally:
            if proc.poll() is None:
                try:
                    proc.stdin.write(b"stop\n"); proc.stdin.flush()
                    proc.wait(timeout=60)
                except (OSError, subprocess.TimeoutExpired):
                    proc.kill(); proc.wait()
    content = console.read_text(encoding="utf-8", errors="replace")
    errors = [line for line in content.splitlines() if " ERROR]" in line or "Exception" in line]
    if result.exists():
        data = json.loads(result.read_text(encoding="utf-8"))
        data.update(paperBuild=build["id"], serverLog=str(console.relative_to(ROOT)), serverErrors=errors,
                    java=str(java), serverSHA256=build["downloads"]["server:default"]["checksums"]["sha256"],
                    pluginSHA256=hashlib.sha256(release_jar().read_bytes()).hexdigest())
        result.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    print("RESULT:", result, "server errors:", len(errors), flush=True)
    if suite_failed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
