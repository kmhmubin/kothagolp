#!/usr/bin/env python3
"""On-device UI automation: drive the real app through every source via
uiautomator dumps. Per source: open from browse list -> novels load ->
open first novel -> details render -> scroll chapters -> Start Reading ->
reader shows text -> scroll reader (next-chapter path) -> back out.
Collects frame stats, crash log, memory trend."""
import re, subprocess, time, sys, os

PKG = "com.kmhmubin.kothagolp.debug"
SP = "/tmp/claude-1000/-home-mubin-Projects-kothagolp/30dadc72-c5ea-4b57-a9c2-064eef3c270d/scratchpad"
SHOTS = f"{SP}/ui_shots"
os.makedirs(SHOTS, exist_ok=True)

SOURCES = [
    "AllNovel", "Cyrisia", "Fenrir Realm", "FreeWebNovel", "FuckNovelPia",
    "LibRead", "Light Novel Translations", "Light Novel World", "Lnori",
    "Novel Archive", "Novel Buddy", "NovelArrow", "NovelBin",
    "NovelDex", "NovelFire", "PawRead", "Royal Road", "Webnovel",
]  # NovelsOnline skipped: offline

def sh(cmd, timeout=30):
    return subprocess.run(["adb", "shell"] + cmd, capture_output=True, text=True, timeout=timeout).stdout

def tap(x, y): sh(["input", "tap", str(x), str(y)]); time.sleep(0.8)
def swipe(y1, y2, ms=250): sh(["input", "swipe", "360", str(y1), "360", str(y2), str(ms)]); time.sleep(0.7)
def back(): sh(["input", "keyevent", "KEYCODE_BACK"]); time.sleep(1.2)

def dump():
    sh(["uiautomator", "dump", "/sdcard/ui.xml"])
    subprocess.run(["adb", "pull", "/sdcard/ui.xml", f"{SP}/ui.xml"], capture_output=True)
    try:
        return open(f"{SP}/ui.xml", encoding="utf-8", errors="ignore").read()
    except Exception:
        return ""

def _bounds_center(b):
    m = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
    if not m: return None
    x1, y1, x2, y2 = map(int, m.groups())
    if x2 <= x1 or y2 <= y1: return None  # zero-size (Compose text node)
    return ((x1 + x2) // 2, (y1 + y2) // 2)

def find_node(xml, text_exact):
    """Find a node by exact text. Compose text nodes often report
    bounds=[0,0][0,0]; fall back to the nearest ancestor with real bounds."""
    idx = 0
    while True:
        i = xml.find(f'text="{text_exact}"', idx)
        if i < 0:
            return None
        idx = i + 1
        # Verify exact match (not a prefix)
        tag_start = xml.rfind('<node', 0, i)
        tag_end = xml.find('>', i)
        tag = xml[tag_start:tag_end]
        own = re.search(r'bounds="(\[[^"]*\])"', tag)
        if own:
            c = _bounds_center(own.group(1))
            if c: return c
        # Walk up: nearest preceding bounds="..." with real size
        head = xml[:tag_start]
        for b in reversed(re.findall(r'bounds="(\[\d+,\d+\]\[\d+,\d+\])"', head)):
            c = _bounds_center(b)
            if c: return c
    return None

def texts(xml):
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]

def cdescs(xml):
    return [c for c in re.findall(r'content-desc="([^"]*)"', xml) if c]

def novel_titles(xml):
    # Novel cards expose the title as both text and content-desc; either counts
    seen = set(t for t in texts(xml) + cdescs(xml) if len(t) >= 8)
    # drop obvious chrome
    return [t for t in seen if not any(k in t.lower()
            for k in ("search", "genre", "in library", "discover", "no downloads"))]

def screenshot(name):
    subprocess.run(f"adb exec-out screencap -p > {SHOTS}/{name}.png", shell=True)

def crashes():
    log = subprocess.run(["adb", "logcat", "-d", "-s", "AndroidRuntime:E"], capture_output=True, text=True).stdout
    return [l for l in log.splitlines() if "FATAL" in l or "Process: " + PKG in l]

def gfx_reset(): sh(["dumpsys", "gfxinfo", PKG, "reset"])
def gfx_stats():
    out = sh(["dumpsys", "gfxinfo", PKG])
    total = re.search(r"Total frames rendered: (\d+)", out)
    janky = re.search(r"Janky frames: (\d+) \(([\d.]+)%\)", out)
    p90 = re.search(r"90th percentile: (\d+)ms", out)
    if total and janky and p90:
        return f"{janky.group(2)}% janky ({janky.group(1)}/{total.group(1)}), p90={p90.group(1)}ms"
    return "n/a"

def meminfo():
    try:
        out = sh(["dumpsys", "meminfo", PKG], timeout=60)
    except Exception:
        return -1
    m = re.search(r"TOTAL PSS:\s+(\d+)", out) or re.search(r"^\s*TOTAL\s+(\d+)", out, re.M)
    return int(m.group(1)) // 1024 if m else -1  # MB

def dismiss_dialogs():
    # Startup update/changelog dialog blocks everything until dismissed
    for _ in range(3):
        xml = dump()
        node = find_node(xml, "Later") or find_node(xml, "Close") or find_node(xml, "Got it")
        if node:
            tap(*node); time.sleep(1)
        else:
            return

def launch():
    sh(["am", "start", "-n", f"{PKG}/com.kmhmubin.kothagolp.MainActivity"])
    time.sleep(9)
    dismiss_dialogs()

def foreground_ok():
    out = sh(["dumpsys", "window"])
    m = re.search(r"mCurrentFocus=[^\n]*", out)
    return PKG in (m.group(0) if m else "")

def goto_browse_root():
    """Deterministic: cold-relaunch, dismiss dialogs, tap Browse."""
    sh(["am", "force-stop", PKG])
    launch()  # launch() already dismisses dialogs
    node = find_node(dump(), "Browse")
    if not node:
        dismiss_dialogs()
        node = find_node(dump(), "Browse")
    if node:
        tap(*node); time.sleep(2)
    dismiss_dialogs()
    xml = dump()
    if "All Sources" in xml or "Discover novels from your sources" in xml:
        return True
    return "All Sources" in dump()

def find_source_card(name, max_scrolls=10):
    for _ in range(max_scrolls):
        xml = dump()
        pos = find_node(xml, name)
        if pos and pos[1] < 1350:  # above bottom nav
            return pos
        swipe(1250, 550, 350)
    return None

def wait_for(predicate, timeout_s, poll=2.0):
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        xml = dump()
        if predicate(xml):
            return xml
        time.sleep(poll)
    return None

def test_source(name):
    steps = {"open": "-", "novels": "-", "details": "-", "reader": "-", "frames": "-"}
    issues = []

    if not goto_browse_root():
        return steps, ["could not reach browse source list"]

    pos = find_source_card(name)
    if not pos:
        return steps, ["source card not found in list"]
    tap(*pos)
    steps["open"] = "ok"

    # novels grid loads?
    xml = wait_for(lambda x: ("No Novels Found" in x) or len(novel_titles(x)) >= 3, 35)
    if xml is None or "No Novels Found" in (xml or ""):
        screenshot(f"fail_{name.replace(' ', '_')}_browse")
        return steps, ["source browse empty / not loading"]
    steps["novels"] = "ok"

    gfx_reset()
    # scroll browse a bit (perf sample) then open first novel
    swipe(1250, 500, 250); swipe(500, 1250, 250)
    xml = dump()
    # first novel card: first long text below header area
    novel = None
    for m in re.finditer(r'<node[^>]*text="([^"]{10,60})"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t = m.group(1)
        y = (int(m.group(3)) + int(m.group(5))) // 2
        if 380 < y < 1300 and t not in (name,) and "Search" not in t and "genre" not in t.lower():
            novel = ((int(m.group(2)) + int(m.group(4))) // 2, y, t)
            break
    if not novel:
        screenshot(f"fail_{name.replace(' ', '_')}_pick")
        return steps, ["could not locate a novel card"]
    tap(novel[0], novel[1])

    # details: wait for Start Reading / Add to Library
    xml = wait_for(lambda x: "Start Reading" in x or "Add to Library" in x or "Retry" in x, 40)
    if xml is None or "Retry" in (xml or "") and "Start Reading" not in (xml or ""):
        screenshot(f"fail_{name.replace(' ', '_')}_details")
        issues.append(f"details failed for '{novel[2][:30]}'")
        return steps, issues
    steps["details"] = "ok"

    # scroll chapter area on details
    swipe(1250, 500, 250); swipe(1250, 500, 250); swipe(500, 1250, 250); swipe(500, 1250, 250)

    srb = find_node(dump(), "Start Reading")
    if not srb:
        swipe(500, 1250, 250)
        srb = find_node(dump(), "Start Reading")
    if not srb:
        screenshot(f"fail_{name.replace(' ', '_')}_srb")
        return steps, issues + ["Start Reading button not found"]
    tap(*srb)

    # reader: wait for long text node (chapter prose)
    xml = wait_for(lambda x: any(len(t) > 120 for t in texts(x)) or "Failed" in x, 45)
    if xml is None or (xml and "Failed" in xml and not any(len(t) > 120 for t in texts(xml))):
        screenshot(f"fail_{name.replace(' ', '_')}_reader")
        return steps, issues + ["reader did not show chapter text"]
    steps["reader"] = "ok"

    # scroll reader toward next chapter (infinite scroll path); watch for crash
    for _ in range(10):
        swipe(1300, 350, 200)
    time.sleep(2)
    steps["frames"] = gfx_stats()

    if not sh(["pidof", PKG]).strip():
        return steps, issues + ["APP PROCESS DIED during reader scroll"]

    screenshot(f"ok_{name.replace(' ', '_')}_reader")
    return steps, issues

def main():
    subprocess.run(["adb", "logcat", "-c"], capture_output=True)
    sh(["am", "force-stop", PKG])
    launch()
    if not foreground_ok():
        print("FATAL: app did not come to foreground - is it installed?"); sys.exit(1)
    xml = dump()
    node = find_node(xml, "Browse")
    if node: tap(*node); time.sleep(3)

    mem_start = meminfo()
    results = []
    for name in SOURCES:
        print(f"--- {name} ---", flush=True)
        try:
            steps, issues = test_source(name)
        except Exception as e:
            steps, issues = {}, [f"driver error: {e}"]
        mem = meminfo()
        results.append((name, steps, issues, mem))
        print(f"    {steps} mem={mem}MB issues={issues}", flush=True)
        cr = crashes()
        if cr:
            print(f"    !!! CRASH LOG: {cr[:3]}", flush=True)
            subprocess.run(["adb", "logcat", "-c"], capture_output=True)
            launch()

    print("\n========== UI TEST SUMMARY ==========")
    print(f"start memory: {mem_start}MB")
    for name, steps, issues, mem in results:
        ok = all(v != "-" for k, v in steps.items() if k != "frames") and not issues
        print(f"[{'PASS' if ok else 'FAIL'}] {name}: {steps} mem={mem}MB {('ISSUES: ' + '; '.join(issues)) if issues else ''}")

if __name__ == "__main__":
    main()
