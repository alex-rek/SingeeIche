# SingeeIche

**SingeeIche** is an unofficial YouTube client for Android phones and tablets, built on the
SmartTube client engine. The project starts from [NewTube](https://github.com/aleixrodriala/newtube),
the phone-oriented SmartTube fork, and keeps the upstream MIT license and third-party notices.

## Features

- Sign in to YouTube using Google's device-code flow.
- SponsorBlock segment skipping and playback controls from SmartTube.
- Picture-in-picture, a draggable mini-player, and background audio playback.
- Touch-first browsing, search, subscriptions, playlists, and watch history.
- Optional offline video and audio downloads.
- Horizontal YouTube category navigation, Shorts, and a dedicated downloads tab.
- In-app checks and signed APK downloads from GitHub Releases.

SingeeIche is an independent community client. It is not affiliated with or endorsed by Google,
YouTube, or the SmartTube maintainers. YouTube availability and playback behavior can change.

## Build

Install JDK 17 and Android SDK platforms/build-tools required by the project, initialize the Git
submodules, then run:

```powershell
.\gradlew.bat :smarttubetv:assembleStmobileDebug
```

The debug APK is written under `smarttubetv/build/outputs/apk/stmobile/debug/`.

## Credits and license

The app is based on [SmartTube](https://github.com/yuliskov/SmartTube) and its contributors. See
[LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for attributions and license details.
