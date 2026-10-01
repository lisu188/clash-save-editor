from pathlib import Path
import subprocess
import sys

blocked_exact = {"clash95.c"}
blocked_prefixes = ("save/", "retail/", "reference/")
blocked_suffixes = (
    ".exe", ".dll", ".res", ".avi", ".wav", ".iso", ".img",
    ".idb", ".i64", ".id0", ".id1", ".nam", ".til", ".gpr",
    ".rep", ".dmp", ".dump"
)
blocked_text = ("Hex-Rays", "clash-disassembly")

tracked = subprocess.check_output(["git", "ls-files", "-z"]).decode().split("\0")
violations = []

for raw in tracked:
    if not raw:
        continue
    path = raw.replace("\\", "/")
    lower = path.lower()
    if path in blocked_exact or path.startswith(blocked_prefixes) or lower.endswith(blocked_suffixes):
        violations.append(f"blocked tracked file: {path}")
        continue
    file = Path(raw)
    if not file.is_file() or file.stat().st_size > 1_000_000:
        continue
    try:
        text = file.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue
    for marker in blocked_text:
        if marker in text:
            violations.append(f"blocked provenance marker {marker!r}: {path}")

if violations:
    print("\n".join(violations), file=sys.stderr)
    sys.exit(1)

print("PUBLIC_BOUNDARY_OK")
