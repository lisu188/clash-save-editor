#!/usr/bin/env bash
# Local-only acceptance harness. Retail assets and the Wine reference prefix are never published.
set -euo pipefail
mode=${1:?start, capture, input, collect-save or stop}; shift
if [[ "$mode" == start ]]; then
    reference=$(realpath "${1:?reference capture directory}")
    dat=$(realpath "${2:?exported DAT}")
    out=$(realpath -m "${3:?evidence directory}")
    test -f "$reference/session/clash95.exe"
    test -d "$reference/wine-prefix"
    test -f "${dat%.*}.fac"
    mkdir -p "$out"
    native=$(mktemp -d /tmp/clash-editor-validation.XXXXXXXX)
    printf '%s\n' "$native" > "$out/native-session.txt"
    mkdir "$native/game"
    cp -a "$reference/session/." "$native/game/"
    cp -a "$reference/wine-prefix" "$native/prefix"
    # Writable game directories must not retain links into an installed/prior run.
    for writable in save output; do
        if [[ -L "$native/game/$writable" ]]; then unlink "$native/game/$writable"; fi
        mkdir -p "$native/game/$writable"
    done
    cp "$dat" "$native/game/save/0.dat"
    cp "${dat%.*}.fac" "$native/game/save/0.fac"
    sha256sum "$dat" "${dat%.*}.fac" > "$out/editor-export.sha256"
    cp "$dat" "$out/editor-export.dat"
    cp "${dat%.*}.fac" "$out/editor-export.fac"
    test -L "$native/prefix/drive_c/clash"
    ln -sfnT "$native/game" "$native/prefix/drive_c/clash"
    sha256sum /mnt/c/clash/clash95.exe "$native/game/clash95.exe" > "$out/executable.sha256"
    cmp /mnt/c/clash/clash95.exe "$native/game/clash95.exe"
    # Choose an unused explicit display; -displayfd cannot create sockets reliably on WSLg.
    number=245
    while [[ -e /tmp/.X${number}-lock ]]; do number=$((number + 1)); done
    Xvfb ":$number" -screen 0 800x600x24 -nolisten tcp >"$out/xvfb.log" 2>&1 &
    printf '%s\n' "$!" > "$out/xvfb.pid"
    export DISPLAY=":$number"
    for attempt in {1..50}; do xdpyinfo >/dev/null 2>&1 && break; sleep 0.2; done
    xdpyinfo >/dev/null
    printf '%s\n' "$number" > "$out/display-number"
    export WINEPREFIX="$native/prefix" WINEARCH=win32 WINEDEBUG=-all
    # Audio stays silent and the original game's binary remains unmodified.
    export WINEDLLOVERRIDES='ddraw=b;winepulse.drv,winealsa.drv='
    printf 'export DISPLAY=%q WINEPREFIX=%q WINEARCH=win32 WINEDEBUG=-all WINEDLLOVERRIDES=%q\n' "$DISPLAY" "$WINEPREFIX" "$WINEDLLOVERRIDES" > "$out/environment.sh"
    cd "$native/game"
    setsid wine explorer /desktop=ClashOriginal,640x480 'C:\clash\clash95.exe' > "$out/wine.log" 2>&1 < /dev/null &
    printf '%s\n' "$!" > "$out/wine.pid"
    printf 'original_started=%s\nnative_session=%s\ndisplay=%s\n' "$(date -u +%FT%TZ)" "$native" "$DISPLAY"
    exit
fi
out=$(realpath "${1:?evidence directory}"); shift
source "$out/environment.sh"
case "$mode" in
    capture)
        name=${1:-frame}
        [[ "$name" =~ ^[A-Za-z0-9_-]+$ ]]
        import -window root "$out/$name.png"
        xwininfo -root -tree > "$out/$name.windows.txt"
        printf '%s\n' "$out/$name.png"
        ;;
    input)
        printf '%s input' "$(date -u +%FT%TZ)" >> "$out/input.log"
        printf ' %q' "$@" >> "$out/input.log"
        printf '\n' >> "$out/input.log"
        xdotool "$@"
        ;;
    collect-save)
        slot=${1:?physical save slot}; name=${2:?evidence label}
        [[ "$slot" =~ ^[0-9]$ && "$name" =~ ^[A-Za-z0-9_-]+$ ]]
        native=$(cat "$out/native-session.txt")
        [[ "$native" == /tmp/clash-editor-validation.* ]]
        test "$(realpath "$native/game/save")" = "$native/game/save"
        cp "$native/game/save/$slot.dat" "$out/$name.dat"
        cp "$native/game/save/$slot.fac" "$out/$name.fac"
        sha256sum "$out/$name.dat" "$out/$name.fac" > "$out/$name.sha256"
        ;;
    stop)
        native=$(cat "$out/native-session.txt")
        [[ "$native" == /tmp/clash-editor-validation.* ]]
        WINEPREFIX="$native/prefix" timeout 10s wineserver -k || true
        kill "$(cat "$out/xvfb.pid")" 2>/dev/null || true
        printf 'stopped=%s\n' "$(date -u +%FT%TZ)" > "$out/cleanup.txt"
        ;;
    *) echo 'Unknown operation' >&2; exit 2;;
esac
