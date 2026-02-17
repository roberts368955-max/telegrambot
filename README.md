# MT5 Lot Calculator (Android)

Android app for MetaTrader 5 style position sizing.

## Features

- Dynamic lot sizing for forex, metals, and crypto
- Dark mode friendly Material 3 UI
- Compact dropdown selectors (less scrolling)
- Two valuation engines:
  - **MT5 Tick Value mode** (Tick Value Loss + Tick Value Profit)
  - **Contract Size mode** (Contract Size + Quote->Account conversion)
- Quick presets:
  - Forex majors (EURUSD-like)
  - XAUUSD (FTMO style)
  - XAGUSD (FTMO style)
  - BTCUSD (FTMO style)
  - ETHUSD (FTMO style)
  - Custom symbol
- Risk mode options:
  - Risk % of balance
  - Fixed SL in currency
- Lot mode options:
  - Auto lot from risk
  - Fixed lot (manual)
- Sizing adjustment mode:
  - Price move only (quick style)
  - Include spread + costs
- Commission mode options:
  - No commission
  - Fixed amount per lot
  - Percent of notional
- Take profit options:
  - Fixed R:R
  - Manual TP price
- Buy / Sell validation for SL and TP placement
- Output includes:
  - Requested risk and actual risk
  - Recommended lot and selected lot
  - Risk difference vs target
  - SL/TP distance (ticks or price distance, depending on mode)
  - Gross SL move, costs, effective risk and effective reward per lot
  - TP exit price, TP value in currency, and actual R:R

## Core logic

### MT5 Tick Value mode

- `slTicks = (abs(entry - sl) + spread) / tickSize`
- `tpTicks = max(abs(tp - entry) - spread, 0) / tickSize`
- `stopLossCostPerLot = slTicks * tickValueLoss`
- `rewardPerLot = tpTicks * tickValueProfit`

### Contract Size mode

- `valuePerPriceUnitPerLot = contractSize * conversionRate`
- `stopLossCostPerLot = (abs(entry - sl) + spread) * valuePerPriceUnitPerLot`
- `rewardPerLot = max(abs(tp - entry) - spread, 0) * valuePerPriceUnitPerLot`

Then:

- `effectiveRiskPerLot = stopLossCostPerLot + commissionPerLot + extraCostsPerLot`
- `effectiveRewardPerLot = rewardPerLot - commissionPerLot - extraCostsPerLot`
- `rawLot = requestedRiskAmount / effectiveRiskPerLot`
- `normalizedLot` is adjusted to broker min/step/max

When **Price move only** mode is selected, spread and costs are ignored in sizing (closer to quick calculators).

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

## Accuracy checklist (important)

For best results, copy values from **MT5 Symbol Specification**:

- Tick Size
- Tick Value Loss
- Tick Value Profit
- Contract Size
- Spread (approx)
- Commission (round-turn) and extra fees/swaps
- Volume min/step/max

If account currency is different from quote currency, set conversion rate accordingly in Contract Size mode.

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