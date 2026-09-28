# Statusbarplus

Statusbarplus is a lightweight Android app that shows calendar information in the status bar through a normal Android notification icon.

The app is designed around a clean, expressive Material 3 interface inspired by the component and list patterns in Material_3_Expressive_List:
https://github.com/NicosNicolaou16/Material_3_Expressive_List

## Features

- **Day only**
- **Day + date**
- **Date + month**
- Adjustable status-bar text size.
- Horizontal and vertical text offset.
- Padding and line-spacing controls.
- System font or imported **TTF/OTF** font.
- Light, dark, or system app theme.
- Dynamic Material colors on supported Android versions.
- Large rounded surfaces and list-style settings inspired by Material 3 Expressive.
- Automatic refresh at the next calendar day.
- Persistent ongoing notification.
- Battery-optimization guidance.
- Android notification permission handling.
- No root, Xposed, Accessibility Service, overlay, or Shizuku required.

## What was removed

The interface intentionally no longer includes:

- **Day + date + month**
- **Display presets**
- **Text alignment selector**
- **Custom date-format selector**

Date and month use locale-aware Android formatting. The text is centered inside the notification bitmap; the app does not attempt to move the notification slot itself.

## How it works

Statusbarplus renders the selected calendar information into a bitmap and supplies it as the notification's small icon.

This architecture is deliberately no-root and uses only public Android notification APIs. Android System UI still controls the final notification slot, its ordering, spacing relative to the built-in clock, tinting, and scaling. Therefore, controls in this app affect the text inside the notification icon, not the physical notification slot.

## Interface design

The app uses Material 3 components with an expressive visual direction:

- Larger typography for primary sections.
- Generous spacing and 28dp rounded surfaces.
- Tonal surface containers and subtle outlines.
- Grouped list-like settings rows.
- Clear hierarchy between preview, controls, and secondary settings.
- System/light/dark theme choices with dynamic colors where supported.

This is a View-based Android implementation, while the referenced project is a Jetpack Compose showcase. The repository is used as a design reference rather than copied code.

## Usage

1. Install Statusbarplus.
2. Open the app.
3. Enable **Show in status bar**.
4. Grant notification permission when Android requests it.
5. Choose the status-bar content.
6. Adjust text size or layout if needed.
7. Choose the system font or import a TTF/OTF font.
8. Optionally allow the battery-optimization exemption.
9. Leave the notification enabled; the app refreshes it when the calendar date changes.

## Requirements

- Android 11 or newer.
- Notification permission on Android versions that require it.
- No root required.
- No Xposed required.
- No Accessibility Service required.
- No overlay required.
- No Shizuku required.

## Build

GitHub Actions builds an optimized debug APK on pushes to main and on manual workflow runs. The APK is uploaded as a workflow artifact named **Statusbarplus**.

The debug build uses the standard Android debug signing configuration, so no release keystore is required.

## Project structure

- MainActivity.kt — Material 3 settings UI and preferences.
- DayNotificationManager.kt — notification rendering and midnight scheduling.
- DayNotificationReceiver.kt — boot/date-change restoration.
- FontManager.kt — system/custom font handling.
- .github/workflows/build.yml — automated build and APK verification.

## Design reference

Primary UI reference:
https://github.com/NicosNicolaou16/Material_3_Expressive_List

The project is used for visual and interaction inspiration only; Statusbarplus keeps its own implementation and architecture.
