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
    return subprocess.check_output(["adb", *args], timeout=35, text=not binary)


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


def main():
    adb("install", "-r", sys.argv[1])
    adb("shell", "pm", "clear", PACKAGE)
    launch()
    locate("SAMPLE INVENTORY")
    screenshot("01-discover")
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
    print("PASS: Android UI launch, store browsing, details, saved persistence, store-specific prices, sources, rotation and large-text navigation")


try:
    main()
except Exception:
    screenshot("failure")
    (OUT / "logcat.txt").write_text(adb("logcat", "-d", "-t", "1500"))
    raise
finally:
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
