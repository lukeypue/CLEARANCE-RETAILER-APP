"""Exercise the installed test APK through Android's real UI, using adb only."""
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.clearance.retailer.test"
COMPONENT = PACKAGE + "/com.clearance.retailer.MainActivity"
OUT = pathlib.Path("smoke-results")
OUT.mkdir(exist_ok=True)


def adb(*args, binary=False):
    return subprocess.check_output(["adb", *args], timeout=180, text=not binary)


def snapshot():
    adb("shell", "uiautomator", "dump", "/sdcard/clearance-window.xml")
    xml = adb("shell", "cat", "/sdcard/clearance-window.xml")
    (OUT / "last-window.xml").write_text(xml)
    return ET.fromstring(xml)


def locate(value, *, scroll=False, timeout=25):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        root = snapshot()
        for node in root.iter("node"):
            if any(node.get(k, "").casefold() == value.casefold() for k in ("text", "content-desc")):
                bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                    return bounds
        if scroll:
            width, height = map(int, re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))[-1])
            adb("shell", "input", "swipe", str(width // 2), str(int(height * .76)),
                str(width // 2), str(int(height * .30)), "350")
        else:
            time.sleep(.4)
    raise AssertionError("UI element not found: " + value)


def tap(value, **kwargs):
    left, top, right, bottom = locate(value, **kwargs)
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def screenshot(name):
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def launch():
    adb("shell", "am", "start", "-W", "-n", COMPONENT)


def fill(value, text):
    tap(value)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    for _ in range(8):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    adb("shell", "input", "text", text)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")


def first_save():
    end = time.monotonic() + 45
    while time.monotonic() < end:
        for node in snapshot().iter("node"):
            value = node.get("content-desc", "")
            if value.startswith("Save store osm-"):
                tap(value)
                return value
        width, height = map(int, re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))[-1])
        adb("shell", "input", "swipe", str(width // 2), str(int(height * .76)),
            str(width // 2), str(int(height * .30)), "350")
    raise AssertionError("No real store save control")


def main():
    adb("install", "-r", sys.argv[1])
    adb("shell", "pm", "clear", PACKAGE)
    adb("install", "-r", sys.argv[2])
    adb("shell", "svc", "wifi", "enable")
    adb("shell", "svc", "data", "enable")
    output = adb("shell", "am", "instrument", "-w", PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner")
    (OUT / "instrumentation.txt").write_text(output)
    print(output)
    if not re.search(r"OK \(6 tests\)", output):
        raise AssertionError("Android data/integration tests did not all pass")
    launch()
    locate("Start close to home.")
    screenshot("01-zip-search")
    fill("Five-digit US ZIP", "123")
    tap("Find stores")
    locate("Enter a five-digit US ZIP code.")
    fill("Five-digit US ZIP", "84043")
    tap("Find stores")
    locate("Walmart · 5 nearby", scroll=True)
    screenshot("02-real-walmart-stores")
    saved_control = first_save()
    locate("Saved store ✓")
    tap("Saved tab")
    locate("Your saved stores.")
    locate("Clearance & stock: unavailable", scroll=True)
    locate(saved_control, scroll=True)
    screenshot("03-saved-real-store")
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    locate(saved_control, scroll=True)
    tap(saved_control)
    locate("No saved stores in this selection", scroll=True)
    tap("Discover tab")
    tap("Closest 5")
    tap("Closest 3")
    locate("Walmart · 3 nearby", scroll=True)
    tap("Discover tab")
    tap("All 36 categories  ›", scroll=True)
    fill("Find a category", "Farm")
    tap("Farm & Ranch")
    locate("Farm & Ranch  ›", scroll=True)
    screenshot("04-categories")
    tap("Stores tab")
    locate("Walmart · 3 nearby", scroll=True)
    tap("Search retailer website", scroll=True)
    locate("Check Walmart")
    screenshot("05-website-handoff")
    tap("Cancel")
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap("Find stores")
    locate("Walmart · 3 nearby", scroll=True)
    screenshot("06-offline-cache")
    adb("shell", "svc", "wifi", "enable")
    adb("shell", "svc", "data", "enable")
    tap("Sources tab")
    locate("Store discovery is connected")
    tap("Try sample catalog", scroll=True, timeout=60)
    locate("SAMPLE INVENTORY")
    tap("Stores tab")
    tap("Browse Walmart · North · sample store", scroll=True)
    tap("View Compact air fryer at Walmart · North · sample store", scroll=True)
    locate("SAMPLE ITEM · NOT LIVE INVENTORY")
    locate("$24.00")
    locate("Not provided", scroll=True)
    screenshot("02-item-detail")
    tap("Save item")
    tap("Saved tab")
    locate("1 sample find saved", scroll=True)
    screenshot("03-saved")
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    locate("1 sample find saved", scroll=True)
    tap("Stores tab")
    tap("Browse Walmart · South · sample store", scroll=True)
    tap("View Compact air fryer at Walmart · South · sample store", scroll=True)
    locate("$32.00")
    locate("Save item")  # The same product at a different store must not be saved.
    tap("Close")
    tap("Sources tab")
    locate("Know your data.")
    screenshot("04-sources")
    adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
    adb("shell", "settings", "put", "system", "user_rotation", "1")
    locate("Sources tab")
    screenshot("05-landscape")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
    adb("shell", "settings", "put", "system", "font_scale", "1.5")
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    locate("Sources tab")
    screenshot("06-large-text")
    print("PASS: live ZIP lookup, 3/5 nearest stores, categories, saved locations, offline cache, sample isolation, saved deals, rotation and large-text navigation")


try:
    main()
except Exception:
    screenshot("failure")
    (OUT / "logcat.txt").write_text(adb("logcat", "-d", "-t", "1500"))
    (OUT / "connectivity.txt").write_text(adb("shell", "dumpsys", "connectivity"))
    raise
finally:
    adb("shell", "svc", "wifi", "enable")
    adb("shell", "svc", "data", "enable")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
