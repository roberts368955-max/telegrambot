# Lot Calculator (Android)

Simple Android app to calculate recommended lot size for a trade based on:

- Account balance
- Risk percentage per trade
- Stop loss in pips
- Pip value for 1.0 lot

## Formula

- `riskAmount = balance * (riskPercent / 100)`
- `lotSize = riskAmount / (stopLossPips * pipValuePerLot)`

The app rounds lot size to 2 decimal places.

## Build

```bash
./gradlew tasks
```

To run the app, open the project in Android Studio and run on an emulator or device.