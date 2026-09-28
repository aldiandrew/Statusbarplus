# Statusbarplus

Statusbarplus is a lightweight Android utility for displaying calendar information directly in the status bar.

Built with a clean Material 3 interface and designed around Android's native notification framework, the app provides a focused way to keep the current day, date, and month visible without modifying the system or requiring privileged access.

## Features

- **Day** — display the current day of the week.
- **Day + date** — display the day together with the current date.
- **Date + month** — display the current date and month.
- Adjustable status-bar text size.
- Horizontal and vertical text positioning controls.
- Padding and line-spacing controls.
- System font support.
- Custom **TTF/OTF** font support.
- Light, dark, or system theme.
- Dynamic Material colors on supported Android versions.
- Material 3 switches, buttons, sliders, cards, and dialogs.
- Locale-aware day, date, and month formatting.
- Automatic refresh when the calendar date changes.
- Persistent notification for reliable status-bar display.
- Notification permission handling.
- Battery-optimization guidance.
- No root or system modification required.

## How It Works

Statusbarplus renders the selected calendar information as the small icon of an Android notification.

The application controls the content rendered inside that icon, including its typography, size, font, padding, and internal positioning. Android System UI remains responsible for the notification area itself.

This approach keeps the application lightweight and compatible with standard Android APIs without requiring root access, system overlays, Accessibility Service, Xposed, or Shizuku.

## Using Statusbarplus

### 1. Enable the status-bar display

Open Statusbarplus and turn on **Show in status bar**.

Android may request notification permission the first time the feature is enabled. Grant the permission so the status-bar notification can be displayed.

### 2. Choose the information to display

Select the format that best fits your status bar:

- **Day**
- **Day + date**
- **Date + month**

The displayed calendar information follows the device locale.

### 3. Adjust the appearance

Use the available controls to refine the status-bar text:

- **Text size** — change the rendered text size.
- **Horizontal offset** — adjust the text position horizontally within the icon.
- **Vertical offset** — adjust the text position vertically.
- **Padding** — control the internal spacing around the rendered text.
- **Line spacing** — control spacing when the selected layout uses multiple lines.

### 4. Choose a font

Statusbarplus can use the system font or a custom **TTF/OTF** font.

When using a custom font, select the font file from the device and apply it to the status-bar text.

### 5. Customize the app theme

Choose between:

- **System** — follow the device appearance.
- **Light**
- **Dark**

Dynamic Material colors are used where supported by the Android version and device configuration.

### 6. Keep the notification active

Statusbarplus uses an ongoing notification to maintain the status-bar display. The notification is refreshed automatically when the calendar date changes.

For devices with aggressive background restrictions, the app also provides battery-optimization guidance to help maintain reliable operation.

## Requirements

- Android 11 or newer.
- Notification permission on Android versions that require it.
- No root required.
- No Xposed required.
- No Accessibility Service required.
- No overlay required.
- No Shizuku required.

## Build

The project uses Gradle and Android Gradle Plugin tooling and is built automatically with GitHub Actions.

Each build verifies the generated APK before publishing it as a workflow artifact.

## Project Structure

- `MainActivity.kt` — Material 3 settings interface and application preferences.
- `DayNotificationManager.kt` — status-bar notification rendering and refresh scheduling.
- `DayNotificationReceiver.kt` — restoration after system events and date changes.
- `FontManager.kt` — system and custom font handling.
- `.github/workflows/build.yml` — automated Android build and APK verification.

## Architecture

Statusbarplus is intentionally built around public Android APIs and a small, focused application architecture.

The rendering pipeline is separated from the settings interface, allowing the notification presentation to remain predictable while keeping configuration simple and maintainable.

The application does not require elevated privileges or system modification, making it suitable for standard, non-root Android installations.
