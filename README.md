# MT5 EA + Custom Indicator (Android Workflow)

Yes — this repo now includes both:

- `mt5/Experts/EMA_Cross_EA.mq5` (Expert Advisor)
- `mt5/Indicators/EMA_Cross_Arrows.mq5` (Custom Indicator)

## Important Android MT5 limitation

The **MetaTrader 5 Android app cannot run EAs or custom indicators directly**.

Use this workflow instead:

1. Run EA/indicator on **MT5 Desktop** (Windows/macOS terminal) or **MQL5 VPS**.
2. Keep that terminal/VPS online.
3. Use **Android MT5** to monitor and manage trades remotely.

## What the included code does

### 1) `EMA_Cross_Arrows.mq5` (indicator)
- Draws green up arrows on bullish EMA crossover.
- Draws red down arrows on bearish EMA crossover.
- Inputs:
  - `FastEMAPeriod` (default `20`)
  - `SlowEMAPeriod` (default `50`)
  - `ArrowOffsetPoints` (default `100`)
  - `SignalOnClosedBar` (default `true`)

### 2) `EMA_Cross_EA.mq5` (EA)
- Trades EMA crossover on new bars.
- Uses fixed lot sizing and optional SL/TP.
- Filters by magic number and supports one-position mode.
- Inputs:
  - `FastEMAPeriod` / `SlowEMAPeriod`
  - `FixedLot`
  - `StopLossPoints` / `TakeProfitPoints`
  - `MagicNumber`
  - `OnePositionPerSymbol`
  - `CloseOppositeSignal`

## Install instructions

1. Open **MT5 Desktop** -> `File` -> `Open Data Folder`.
2. Copy files:
   - `mt5/Experts/EMA_Cross_EA.mq5` -> `MQL5/Experts/`
   - `mt5/Indicators/EMA_Cross_Arrows.mq5` -> `MQL5/Indicators/`
3. Open **MetaEditor**, compile both files.
4. In MT5:
   - Attach indicator to chart for visual signals.
   - Attach EA to chart and enable AutoTrading.
5. If you want phone-only monitoring, migrate to **MQL5 VPS**.

## Notes

- Start on demo first.
- Tune periods/SL/TP per symbol and timeframe.
- This is a baseline strategy, not financial advice.