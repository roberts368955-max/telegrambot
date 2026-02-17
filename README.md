# MT5 Lot Calculator (Android)

Android app for MetaTrader 5 style position sizing.

## Features

- Dynamic lot sizing using MT5 symbol parameters:
  - Tick Size
  - Tick Value (for 1.0 lot)
  - Min Lot / Lot Step / Max Lot
- Risk mode options:
  - Risk % of balance
  - Fixed SL in currency
- Take profit options:
  - Fixed R:R
  - Manual TP price
- Buy / Sell validation for SL and TP placement
- Output includes:
  - Requested risk and actual risk
  - Raw lot and normalized lot
  - SL points and SL cost per 1 lot
  - TP price, expected reward, and actual R:R

## Core logic

- `stopLossPoints = abs(entryPrice - stopLossPrice) / tickSize`
- `stopLossCostPerLot = stopLossPoints * tickValue`
- `rawLot = requestedRiskAmount / stopLossCostPerLot`
- `normalizedLot` is adjusted to broker limits and lot step

## Build APK

### 1) Prerequisites

- JDK 17+ (already fine for this project)
- Android SDK installed
- `adb` installed (for device install)

### 2) Configure Android SDK path

Create `local.properties` in project root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Linux example:

```properties
sdk.dir=/home/your-user/Android/Sdk
```

### 3) Build Debug APK

```bash
./gradlew assembleDebug
```

Generated file:

`app/build/outputs/apk/debug/app-debug.apk`

### 4) Install APK on phone (USB debugging enabled)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If you copy APK manually to the phone, enable install from unknown sources for your file manager.

## Build APK on GitHub (no local SDK needed)

A workflow is included at:

`.github/workflows/build-debug-apk.yml`

How to use it:

1. Push your changes.
2. Open the repository on GitHub.
3. Go to **Actions** -> **Build Debug APK**.
4. Run workflow (or use the run created by push).
5. Download artifact: `mt5-lot-calculator-debug-apk`.

## Release APK (signed)

Unsigned release build:

```bash
./gradlew assembleRelease
```

Output:

`app/build/outputs/apk/release/app-release-unsigned.apk`

For Play Store or normal distribution, sign it with your keystore (Android Studio "Generate Signed Bundle / APK" is easiest).