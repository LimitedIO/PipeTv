<div align="center">

# ▶ PipeTv

**A fast, private, keyboard-first YouTube client for Android TV.**
Pure Kotlin · no XML layouts · no Google services · no account · no API key

</div>

## Why
- **Instant** – the UI is one custom-drawn view tree; the last feed is cached so it shows before the network answers.
- **Smooth everywhere** – virtualised canvas grid, spring physics, thumbnails decoded at card size. Runs on Android 5.0+ boxes.
- **Keep browsing while watching** – video docks to the side and keeps playing; the grid reflows around it.
- **Recommendations** – built from your watch history (related videos), mixed with trending and popular topics.
- **Custom everything** – own on-screen keyboard, nav rail, player HUD, animated focus ring.
- **Auto-update** – checks GitHub Releases, downloads in the background, installs on one click.
- **FOSS** – [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) + [Media3 ExoPlayer](https://github.com/androidx/media). GPL-3.0.

## Controls
| Where | Key | Action |
|---|---|---|
| Anywhere | `←` from first column | Open the nav rail |
| Nav rail | `↑` `↓` / `→` | Switch page / enter page |
| Grid | arrows, `OK` | Move, play fullscreen |
| Grid → right edge | `→` | Focus the docked player |
| Browsing | `Menu` | Expand the playing video |
| Docked player | `OK` | Expand |
| Fullscreen | `OK` / `Space` / `K` | Play / pause |
| Fullscreen | `←` `→` (hold to speed up) / `J` `L` | Seek 10 → 30 → 60 s |
| Fullscreen | `↑` / `↓` | Cycle quality / speed |
| Fullscreen | `0`–`9` / `M` | Jump to 0–90 % / mute |
| Fullscreen | `Back` | Minimise to the dock |
| Nav rail | `Back` | Close the player, then exit |
| Media keys | play, pause, next, stop, ffwd, rew | Work in every screen |
| Search | any physical keyboard | Type directly; results update as you type |

## Install & update
Grab the latest `PipeTv-*.apk` from [Releases](../../releases) and sideload it (`adb install`, Downloader, USB).
After that PipeTv updates itself: **Settings → Updates**. Every push to `main` is built and published by CI.
Works with [Obtainium](https://github.com/ImranR98/Obtainium) too.

## Build
```sh
./gradlew assembleRelease   # needs JDK 17 + Android SDK 35
```

## Code map (every file ≤ 100 lines)
```
data/    Yt (extractor facade) · Feed (recommendations) · Store · Prefs · Http
media/   Engine (ExoPlayer) · Session (now playing, quality fallback) · Sources
ui/      Root (layout + motion) · Controls/PlayerKeys (keys) · Grid · Nav · Keyboard
         Hud · Dock · SearchPage · SettingsView · Cards · Img · Anim (springs)
update/  Updater · Installer
```

<sub>Not affiliated with YouTube or Google. Releases are signed with the open key in `keystore/` so updates stay installable; fork and replace it for your own builds.</sub>
