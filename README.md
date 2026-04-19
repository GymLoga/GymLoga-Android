# IronLog

IronLog is a minimalist, 100% private, and open-source weightlifting logger. 

I built this app because I wanted a tracking tool that stayed out of my way, something that mirrors the speed and simplicity of a physical paper log while providing the modern benefits of automatic progress tracking and PR calculations. It's designed for lifters who prefer efficiency over complex, data-heavy "typical" fitness apps.

## Features

- **Blazing Fast Entry**: Log your sets with simple shorthand like `135x5x3` or `20x10`.
- **Automatic PR Tracking**: The app identifies your Personal Records (PRs) as you log.
- **Strength Analytics**: View your exercise history and estimated One Rep Max (1RM) progress.
- **Privacy First**: No tracking, no ads, and no cloud syncing. Your data never leaves your device.
- **Modern Native Feel**: Built with Kotlin and Jetpack Compose for a fast, responsive Android experience.

## Roadmap
- **Initial Release**
- **Version 1.1**
  - Data Export / Import
- **Version 2.0**
  - Optional web sync  

## Getting Started

### Installation
You can build the app from source to install it on your device.

### Building from Source
If you are on a Linux system (like Debian), ensure you have the Android SDK installed, then run:

```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
./gradlew assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. See the [LICENSE](LICENSE) file for details. 

I hope this app helps others who, like me, are looking for a simple and private way to track their strength journey.
