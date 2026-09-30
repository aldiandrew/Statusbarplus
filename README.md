# Statusbarplus

Statusbarplus is an Android app that displays the current day and date directly in the status bar using Android's notification system. It is designed for modern Android devices and works without root, Xposed, Accessibility Service, overlays, or Shizuku.

## What it does

Statusbarplus lets you see calendar information without opening a clock or calendar app.

Available display modes:

- **Day only** — one line.
- **Day + date** — two lines.
- **Day + date + month** — two lines.
- **Date + month** — two lines.

The weekday is always displayed in its short, locale-aware form. There is no long weekday-name setting.

Additional features:

- Adjustable status-bar text size.
- Bold text rendering for better legibility in the small notification icon area.
- Horizontal text fitting for long localized weekday and month names, preserving vertical text size whenever possible.
- System font or an imported custom **TTF/OTF** font.
- Light, dark, or system application theme.
- Material 3-based application interface.
- Dynamic Material colors on supported Android versions.
- Automatic refresh when the calendar date changes.
- Persistent ongoing notification behavior so the status-bar item is not normally dismissible with a swipe.
- Tapping the status-bar notification opens an Android calendar chooser when one or more calendar apps are installed.
- Background/battery-optimization guidance for devices that aggressively stop background work.
- Full calendar information remains available in the notification shade.
- Notification permission is requested using Android's official permission mechanism.
- Designed for low battery usage.

## How to use

1. Install **Statusbarplus**.
2. Open the app.
3. Turn on **Show day**.
4. Allow notification permission when Android asks for it.
5. Select the information to display under **Status bar content**.
6. Adjust **Status bar text size**.
7. Select **System font** or import your own **TTF/OTF** font.
8. If your device frequently stops background apps, use the battery-optimization option provided by the app.
9. Once enabled, the information appears as a persistent text-based notification icon in the status bar and refreshes when the date changes.
10. Tap the notification to open the installed calendar app chooser.

## After restarting the device

Statusbarplus can receive the Android boot event and restore its scheduled date update when the device starts. If the status-bar information does not return after a restart, open the app once and verify that notification permission and background restrictions are not preventing the notification.

## How it works

Statusbarplus uses an Android **notification small icon**. The day/date text is rendered into a bitmap and supplied as the notification icon. This allows the app to work without drawing directly over System UI.

Android System UI still controls the final notification-icon slot, position, tinting, and scaling. An ongoing notification is used so users normally cannot dismiss the status-bar item with a swipe; Android System UI can still remove notifications in exceptional system conditions. Therefore, the exact physical size and position can vary between Android versions and device manufacturers. Long text is horizontally condensed before the app reduces its font size so that localized weekday names remain as readable as possible.

## Material 3 interface

The application interface follows Material 3 theming principles and uses the device's Android Dynamic Color (Monet) palette on Android 12 and newer, including:

- Light and dark color schemes.
- System theme following.
- Dynamic colors from the device wallpaper/system palette (Monet) on supported Android versions.
- Theme-aware system-bar icon contrast.
- Edge-to-edge layout with system-bar insets handled by the application.

Material 3 uses coordinated color, typography, and shape systems, while Android's current system-bar guidance recommends transparent/translucent bars and correctly contrasted system-bar icons.

## Privacy

Statusbarplus does not require internet access for its core functionality. The displayed day and date are generated from the device's local time and locale.

## Requirements

- Android 11 or newer.
- Notification permission on Android versions that require it.
- No root required.
- No Xposed required.
- No Accessibility Service required.
- No overlay required.
- No Shizuku required.

## Build

The project uses Gradle and GitHub Actions.

Every push to the **main** branch runs a **debug build** and produces **app-debug.apk** as a workflow artifact. The debug build does not require the user to provide or manage a release signing keystore.

## Notes

Statusbarplus focuses on providing lightweight calendar information in the status bar. The available notification-icon space and the final rendering of notification icons are controlled by Android System UI and may differ across devices and Android versions.
