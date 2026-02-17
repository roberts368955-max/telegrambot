package com.example.lotcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LotCalculatorScreen()
                }
            }
        }
    }
}

private enum class TradeSide(val label: String) {
    BUY("Buy"),
    SELL("Sell")
}

private enum class RiskMode(val label: String) {
    PERCENT("Risk % of balance"),
    FIXED_CURRENCY("Fixed SL in currency")
}

private enum class TakeProfitMode(val label: String) {
    FIXED_RR("Fixed R:R"),
    MANUAL_PRICE("Manual TP price")
}

private enum class ValuationMode(val label: String) {
    MT5_TICK_VALUE("MT5 Tick Value (loss/profit)"),
    CONTRACT_SIZE("Contract Size + conversion")
}

private data class SymbolPresetValues(
    val tickSize: String,
    val tickValueLoss: String,
    val tickValueProfit: String,
    val contractSize: String,
    val conversionRate: String,
    val minLot: String,
    val lotStep: String,
    val maxLot: String
)

private enum class SymbolPreset(
    val label: String,
    val values: SymbolPresetValues?
) {
    FOREX_MAJOR(
        label = "Forex majors (EURUSD-like)",
        values = SymbolPresetValues(
            tickSize = "0.00001",
            tickValueLoss = "1",
            tickValueProfit = "1",
            contractSize = "100000",
            conversionRate = "1",
            minLot = "0.01",
            lotStep = "0.01",
            maxLot = "100"
        )
    ),
    XAUUSD(
        label = "Metals (XAUUSD typical)",
        values = SymbolPresetValues(
            tickSize = "0.01",
            tickValueLoss = "1",
            tickValueProfit = "1",
            contractSize = "100",
            conversionRate = "1",
            minLot = "0.01",
            lotStep = "0.01",
            maxLot = "100"
        )
    ),
    BTCUSD(
        label = "Crypto CFD (BTCUSD, 10 coins/lot common)",
        values = SymbolPresetValues(
            tickSize = "0.01",
            tickValueLoss = "0.1",
            tickValueProfit = "0.1",
            contractSize = "10",
            conversionRate = "1",
            minLot = "0.01",
            lotStep = "0.01",
            maxLot = "100"
        )
    ),
    ETHUSD(
        label = "Crypto CFD (ETHUSD, 10 coins/lot common)",
        values = SymbolPresetValues(
            tickSize = "0.01",
            tickValueLoss = "0.1",
            tickValueProfit = "0.1",
            contractSize = "10",
            conversionRate = "1",
            minLot = "0.01",
            lotStep = "0.01",
            maxLot = "100"
        )
    ),
    XAGUSD(
        label = "Metals (XAGUSD, 5000 oz contract common)",
        values = SymbolPresetValues(
            tickSize = "0.001",
            tickValueLoss = "5",
            tickValueProfit = "5",
            contractSize = "5000",
            conversionRate = "1",
            minLot = "0.01",
            lotStep = "0.01",
            maxLot = "100"
        )
    ),
    CUSTOM(
        label = "Custom symbol",
        values = null
    )
}

private data class Mt5LotCalculation(
    val requestedRiskAmount: Double,
    val actualRiskAmount: Double,
    val stopLossCostPerLot: Double,
    val totalCostsPerLot: Double,
    val effectiveRiskPerLot: Double,
    val effectiveRewardPerLot: Double,
    val stopLossDistanceDisplay: Double,
    val takeProfitDistanceDisplay: Double,
    val distanceUnitLabel: String,
    val rawLotSize: Double,
    val normalizedLotSize: Double,
    val takeProfitPrice: Double,
    val expectedRewardAmount: Double,
    val actualRr: Double,
    val valuationMode: ValuationMode,
    val warnings: List<String>
)

@Composable
private fun LotCalculatorScreen() {
    var symbolPresetName by rememberSaveable { mutableStateOf(SymbolPreset.FOREX_MAJOR.name) }
    var tradeSideName by rememberSaveable { mutableStateOf(TradeSide.BUY.name) }
    var riskModeName by rememberSaveable { mutableStateOf(RiskMode.PERCENT.name) }
    var tpModeName by rememberSaveable { mutableStateOf(TakeProfitMode.FIXED_RR.name) }
    var valuationModeName by rememberSaveable { mutableStateOf(ValuationMode.MT5_TICK_VALUE.name) }

    var balanceInput by rememberSaveable { mutableStateOf("10000") }
    var riskPercentInput by rememberSaveable { mutableStateOf("1") }
    var fixedRiskInput by rememberSaveable { mutableStateOf("100") }

    var entryPriceInput by rememberSaveable { mutableStateOf("1.10000") }
    var stopLossPriceInput by rememberSaveable { mutableStateOf("1.09500") }
    var spreadInput by rememberSaveable { mutableStateOf("0") }
    var rrInput by rememberSaveable { mutableStateOf("2") }
    var manualTpPriceInput by rememberSaveable { mutableStateOf("1.11000") }

    var tickSizeInput by rememberSaveable { mutableStateOf("0.00001") }
    var tickValueLossInput by rememberSaveable { mutableStateOf("1") }
    var tickValueProfitInput by rememberSaveable { mutableStateOf("1") }
    var contractSizeInput by rememberSaveable { mutableStateOf("100000") }
    var conversionRateInput by rememberSaveable { mutableStateOf("1") }
    var commissionPerLotInput by rememberSaveable { mutableStateOf("0") }
    var extraCostsPerLotInput by rememberSaveable { mutableStateOf("0") }

    var minLotInput by rememberSaveable { mutableStateOf("0.01") }
    var lotStepInput by rememberSaveable { mutableStateOf("0.01") }
    var maxLotInput by rememberSaveable { mutableStateOf("100") }

    var calculationResult by remember { mutableStateOf<Mt5LotCalculation?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "MT5 Lot Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Supports forex, metals and crypto with MT5-aware sizing.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionTitle("Quick Symbol Preset")
        EnumSelector(
            options = SymbolPreset.entries.map { it.name to it.label },
            selectedName = symbolPresetName,
            onSelect = { selectedName ->
                symbolPresetName = selectedName
                val preset = SymbolPreset.valueOf(selectedName)
                val values = preset.values
                if (values != null) {
                    tickSizeInput = values.tickSize
                    tickValueLossInput = values.tickValueLoss
                    tickValueProfitInput = values.tickValueProfit
                    contractSizeInput = values.contractSize
                    conversionRateInput = values.conversionRate
                    minLotInput = values.minLot
                    lotStepInput = values.lotStep
                    maxLotInput = values.maxLot
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Trade Setup")
        EnumSelector(
            options = TradeSide.entries.map { it.name to it.label },
            selectedName = tradeSideName,
            onSelect = { tradeSideName = it }
        )

        Spacer(modifier = Modifier.height(8.dp))
        NumericInputField(
            value = entryPriceInput,
            onValueChange = { entryPriceInput = it },
            label = "Entry Price",
            placeholder = "e.g. 1.10000"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = stopLossPriceInput,
            onValueChange = { stopLossPriceInput = it },
            label = "Stop Loss Price",
            placeholder = "e.g. 1.09500"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = spreadInput,
            onValueChange = { spreadInput = it },
            label = "Approx spread (price units)",
            placeholder = "e.g. 0.6 on ETHUSD, 0 on idealized backtest"
        )

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Risk Mode")
        EnumSelector(
            options = RiskMode.entries.map { it.name to it.label },
            selectedName = riskModeName,
            onSelect = { riskModeName = it }
        )

        if (RiskMode.valueOf(riskModeName) == RiskMode.PERCENT) {
            Spacer(modifier = Modifier.height(8.dp))
            NumericInputField(
                value = balanceInput,
                onValueChange = { balanceInput = it },
                label = "Account Balance",
                placeholder = "e.g. 10000"
            )
            Spacer(modifier = Modifier.height(12.dp))
            NumericInputField(
                value = riskPercentInput,
                onValueChange = { riskPercentInput = it },
                label = "Risk % per trade",
                placeholder = "e.g. 1"
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            NumericInputField(
                value = fixedRiskInput,
                onValueChange = { fixedRiskInput = it },
                label = "Fixed SL in currency",
                placeholder = "e.g. 100"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Take Profit")
        EnumSelector(
            options = TakeProfitMode.entries.map { it.name to it.label },
            selectedName = tpModeName,
            onSelect = { tpModeName = it }
        )

        if (TakeProfitMode.valueOf(tpModeName) == TakeProfitMode.FIXED_RR) {
            Spacer(modifier = Modifier.height(8.dp))
            NumericInputField(
                value = rrInput,
                onValueChange = { rrInput = it },
                label = "R:R Ratio (reward side)",
                placeholder = "e.g. 2 means 1:2"
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            NumericInputField(
                value = manualTpPriceInput,
                onValueChange = { manualTpPriceInput = it },
                label = "Manual TP Price",
                placeholder = "e.g. 1.11000"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Symbol Valuation Model")
        EnumSelector(
            options = ValuationMode.entries.map { it.name to it.label },
            selectedName = valuationModeName,
            onSelect = { valuationModeName = it }
        )

        Spacer(modifier = Modifier.height(8.dp))
        NumericInputField(
            value = tickSizeInput,
            onValueChange = { tickSizeInput = it },
            label = "Tick Size",
            placeholder = "e.g. 0.00001 or 0.01"
        )

        val valuationMode = ValuationMode.valueOf(valuationModeName)
        if (valuationMode == ValuationMode.MT5_TICK_VALUE) {
            Spacer(modifier = Modifier.height(12.dp))
            NumericInputField(
                value = tickValueLossInput,
                onValueChange = { tickValueLossInput = it },
                label = "Tick Value Loss (1.0 lot)",
                placeholder = "from MT5 symbol specification"
            )

            Spacer(modifier = Modifier.height(12.dp))
            NumericInputField(
                value = tickValueProfitInput,
                onValueChange = { tickValueProfitInput = it },
                label = "Tick Value Profit (1.0 lot)",
                placeholder = "from MT5 symbol specification"
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            NumericInputField(
                value = contractSizeInput,
                onValueChange = { contractSizeInput = it },
                label = "Contract Size (1.0 lot)",
                placeholder = "e.g. 100000 forex, 100 XAU, 1 BTC CFD"
            )

            Spacer(modifier = Modifier.height(12.dp))
            NumericInputField(
                value = conversionRateInput,
                onValueChange = { conversionRateInput = it },
                label = "Quote->Account currency conversion",
                placeholder = "1 if already same currency"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Costs (for net match with history)")
        NumericInputField(
            value = commissionPerLotInput,
            onValueChange = { commissionPerLotInput = it },
            label = "Commission per 1.0 lot (round-turn)",
            placeholder = "0 if no commission"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = extraCostsPerLotInput,
            onValueChange = { extraCostsPerLotInput = it },
            label = "Swap + extra fees per 1.0 lot",
            placeholder = "0 if ignored"
        )

        Spacer(modifier = Modifier.height(12.dp))
        SectionTitle("Broker Volume Limits")
        NumericInputField(
            value = minLotInput,
            onValueChange = { minLotInput = it },
            label = "Min Lot",
            placeholder = "e.g. 0.01"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = lotStepInput,
            onValueChange = { lotStepInput = it },
            label = "Lot Step",
            placeholder = "e.g. 0.01"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = maxLotInput,
            onValueChange = { maxLotInput = it },
            label = "Max Lot",
            placeholder = "e.g. 100"
        )

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                errorMessage = null
                calculationResult = null

                val tradeSide = TradeSide.valueOf(tradeSideName)
                val riskMode = RiskMode.valueOf(riskModeName)
                val tpMode = TakeProfitMode.valueOf(tpModeName)
                val selectedValuationMode = ValuationMode.valueOf(valuationModeName)

                val entryPrice = parsePositiveNumber(entryPriceInput)
                val stopLossPrice = parsePositiveNumber(stopLossPriceInput)
                val spread = parseNonNegativeNumber(spreadInput)
                val tickSize = parsePositiveNumber(tickSizeInput)
                val minLot = parsePositiveNumber(minLotInput)
                val lotStep = parsePositiveNumber(lotStepInput)
                val maxLot = parsePositiveNumber(maxLotInput)
                val commissionPerLot = parseNonNegativeNumber(commissionPerLotInput)
                val extraCostsPerLot = parseNonNegativeNumber(extraCostsPerLotInput)

                if (
                    entryPrice == null ||
                    stopLossPrice == null ||
                    spread == null ||
                    tickSize == null ||
                    minLot == null ||
                    lotStep == null ||
                    maxLot == null ||
                    commissionPerLot == null ||
                    extraCostsPerLot == null
                ) {
                    errorMessage = "Please enter valid values for all required fields."
                    return@Button
                }

                if (maxLot < minLot) {
                    errorMessage = "Max lot must be greater than or equal to min lot."
                    return@Button
                }

                if (lotStep > maxLot) {
                    errorMessage = "Lot step cannot be greater than max lot."
                    return@Button
                }

                if (tradeSide == TradeSide.BUY && stopLossPrice >= entryPrice) {
                    errorMessage = "For BUY, stop loss price must be below entry price."
                    return@Button
                }

                if (tradeSide == TradeSide.SELL && stopLossPrice <= entryPrice) {
                    errorMessage = "For SELL, stop loss price must be above entry price."
                    return@Button
                }

                val requestedRiskAmount = when (riskMode) {
                    RiskMode.PERCENT -> {
                        val balance = parsePositiveNumber(balanceInput)
                        val riskPercent = parsePositiveNumber(riskPercentInput)
                        if (balance == null || riskPercent == null) {
                            errorMessage = "Please enter valid balance and risk % values."
                            return@Button
                        }
                        balance * (riskPercent / 100.0)
                    }

                    RiskMode.FIXED_CURRENCY -> {
                        val fixedRisk = parsePositiveNumber(fixedRiskInput)
                        if (fixedRisk == null) {
                            errorMessage = "Please enter a valid fixed SL currency value."
                            return@Button
                        }
                        fixedRisk
                    }
                }

                val stopLossDistance = abs(entryPrice - stopLossPrice)
                val stopLossDistanceForRisk = stopLossDistance + spread
                val takeProfitPrice = when (tpMode) {
                    TakeProfitMode.FIXED_RR -> {
                        val rr = parsePositiveNumber(rrInput)
                        if (rr == null) {
                            errorMessage = "Please enter a valid R:R ratio greater than zero."
                            return@Button
                        }
                        if (tradeSide == TradeSide.BUY) {
                            entryPrice + stopLossDistance * rr
                        } else {
                            entryPrice - stopLossDistance * rr
                        }
                    }

                    TakeProfitMode.MANUAL_PRICE -> {
                        val manualTp = parsePositiveNumber(manualTpPriceInput)
                        if (manualTp == null) {
                            errorMessage = "Please enter a valid manual TP price."
                            return@Button
                        }
                        if (tradeSide == TradeSide.BUY && manualTp <= entryPrice) {
                            errorMessage = "For BUY, TP price must be above entry price."
                            return@Button
                        }
                        if (tradeSide == TradeSide.SELL && manualTp >= entryPrice) {
                            errorMessage = "For SELL, TP price must be below entry price."
                            return@Button
                        }
                        manualTp
                    }
                }

                val tpDistance = abs(takeProfitPrice - entryPrice)
                val tpDistanceNet = tpDistance - spread
                if (tpDistanceNet <= 0.0) {
                    errorMessage = "TP distance must be greater than spread to have net reward."
                    return@Button
                }
                if (selectedValuationMode == ValuationMode.MT5_TICK_VALUE) {
                    if (stopLossDistanceForRisk < tickSize) {
                        errorMessage = "SL distance must be at least one tick for MT5 tick-value mode."
                        return@Button
                    }
                    if (tpDistanceNet < tickSize) {
                        errorMessage = "TP distance must be at least one tick for MT5 tick-value mode."
                        return@Button
                    }
                }

                val tickValueLoss = if (selectedValuationMode == ValuationMode.MT5_TICK_VALUE) {
                    parsePositiveNumber(tickValueLossInput) ?: run {
                        errorMessage = "Enter valid Tick Value Loss for MT5 mode."
                        return@Button
                    }
                } else {
                    null
                }

                val tickValueProfit = if (selectedValuationMode == ValuationMode.MT5_TICK_VALUE) {
                    parsePositiveNumber(tickValueProfitInput) ?: run {
                        errorMessage = "Enter valid Tick Value Profit for MT5 mode."
                        return@Button
                    }
                } else {
                    null
                }

                val contractSize = if (selectedValuationMode == ValuationMode.CONTRACT_SIZE) {
                    parsePositiveNumber(contractSizeInput) ?: run {
                        errorMessage = "Enter valid contract size for Contract Size mode."
                        return@Button
                    }
                } else {
                    null
                }

                val conversionRate = if (selectedValuationMode == ValuationMode.CONTRACT_SIZE) {
                    parsePositiveNumber(conversionRateInput) ?: run {
                        errorMessage = "Enter valid quote->account conversion rate."
                        return@Button
                    }
                } else {
                    null
                }

                calculationResult = calculateMt5Lot(
                    requestedRiskAmount = requestedRiskAmount,
                    entryPrice = entryPrice,
                    stopLossPrice = stopLossPrice,
                    takeProfitPrice = takeProfitPrice,
                    valuationMode = selectedValuationMode,
                    tickSize = tickSize,
                    tickValueLoss = tickValueLoss,
                    tickValueProfit = tickValueProfit,
                    contractSize = contractSize,
                    conversionRate = conversionRate,
                    spread = spread,
                    commissionPerLot = commissionPerLot,
                    extraCostsPerLot = extraCostsPerLot,
                    minLot = minLot,
                    maxLot = maxLot,
                    lotStep = lotStep
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Lot Size")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val result = calculationResult
        if (result != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valuation Model: ${result.valuationMode.label}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Requested Risk: ${formatValue(result.requestedRiskAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Actual Risk (normalized lot): ${formatValue(result.actualRiskAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Gross SL move cost per 1.0 lot: ${formatValue(result.stopLossCostPerLot)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Costs per 1.0 lot (commission+fees): ${formatValue(result.totalCostsPerLot)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Effective risk per 1.0 lot: ${formatValue(result.effectiveRiskPerLot)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Effective reward per 1.0 lot: ${formatValue(result.effectiveRewardPerLot)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "SL Distance (incl spread): ${formatValue(result.stopLossDistanceDisplay, 2)} ${result.distanceUnitLabel}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "TP Distance (net of spread): ${formatValue(result.takeProfitDistanceDisplay, 2)} ${result.distanceUnitLabel}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Raw Lot Size: ${formatValue(result.rawLotSize, 4)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Normalized Lot Size: ${formatValue(result.normalizedLotSize, 4)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Take Profit Price: ${formatPrice(result.takeProfitPrice, tickSizeInput)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Expected Reward: ${formatValue(result.expectedRewardAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Actual R:R = 1:${formatValue(result.actualRr, 2)}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (result.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.warnings.joinToString(separator = "\n"),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun EnumSelector(
    options: List<Pair<String, String>>,
    selectedName: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { (name, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.RadioButton) { onSelect(name) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedName == name,
                    onClick = { onSelect(name) }
                )
                Text(text = label)
            }
        }
    }
}

@Composable
private fun NumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun parsePositiveNumber(rawValue: String): Double? {
    val normalized = rawValue.trim().replace(",", ".")
    val parsed = normalized.toDoubleOrNull() ?: return null
    return parsed.takeIf { it > 0.0 }
}

private fun parseNonNegativeNumber(rawValue: String): Double? {
    val normalized = rawValue.trim().replace(",", ".")
    val parsed = normalized.toDoubleOrNull() ?: return null
    return parsed.takeIf { it >= 0.0 }
}

private fun calculateMt5Lot(
    requestedRiskAmount: Double,
    entryPrice: Double,
    stopLossPrice: Double,
    takeProfitPrice: Double,
    valuationMode: ValuationMode,
    tickSize: Double,
    tickValueLoss: Double?,
    tickValueProfit: Double?,
    contractSize: Double?,
    conversionRate: Double?,
    spread: Double,
    commissionPerLot: Double,
    extraCostsPerLot: Double,
    minLot: Double,
    maxLot: Double,
    lotStep: Double
): Mt5LotCalculation {
    val warnings = mutableListOf<String>()
    val slDistancePrice = abs(entryPrice - stopLossPrice) + spread
    val tpDistancePrice = max(abs(takeProfitPrice - entryPrice) - spread, 0.0)

    val stopLossCostPerLot: Double
    val rewardPerLot: Double
    val slDistanceDisplay: Double
    val tpDistanceDisplay: Double
    val distanceUnitLabel: String

    if (valuationMode == ValuationMode.MT5_TICK_VALUE) {
        val safeTickValueLoss = tickValueLoss ?: 0.0
        val safeTickValueProfit = tickValueProfit ?: 0.0

        val rawSlTicks = slDistancePrice / tickSize
        val rawTpTicks = tpDistancePrice / tickSize
        val nearestSlTick = floor(rawSlTicks + 0.5)
        val nearestTpTick = floor(rawTpTicks + 0.5)

        if (abs(rawSlTicks - nearestSlTick) > 1e-6) {
            warnings += "SL distance is not aligned to full ticks."
        }
        if (abs(rawTpTicks - nearestTpTick) > 1e-6) {
            warnings += "TP distance is not aligned to full ticks."
        }
        if (rawTpTicks <= 0.0) {
            warnings += "TP distance is very small after spread; expected reward may be near 0."
        }

        stopLossCostPerLot = rawSlTicks * safeTickValueLoss
        rewardPerLot = rawTpTicks * safeTickValueProfit
        slDistanceDisplay = rawSlTicks
        tpDistanceDisplay = rawTpTicks
        distanceUnitLabel = "ticks"
    } else {
        val safeContractSize = contractSize ?: 0.0
        val safeConversionRate = conversionRate ?: 0.0
        val valuePerPriceUnit = safeContractSize * safeConversionRate

        stopLossCostPerLot = slDistancePrice * valuePerPriceUnit
        rewardPerLot = tpDistancePrice * valuePerPriceUnit
        slDistanceDisplay = slDistancePrice
        tpDistanceDisplay = tpDistancePrice
        distanceUnitLabel = "price"
    }

    val totalCostsPerLot = commissionPerLot + extraCostsPerLot
    val effectiveRiskPerLot = stopLossCostPerLot + totalCostsPerLot
    val effectiveRewardPerLot = rewardPerLot - totalCostsPerLot

    if (spread > 0.0) {
        warnings += "Spread was included in risk/reward distance."
    }
    if (effectiveRewardPerLot <= 0.0) {
        warnings += "Net reward per lot is <= 0 after costs."
    }

    val rawLotSize = requestedRiskAmount / effectiveRiskPerLot
    val (normalizedLot, lotWarnings) = normalizeLotSize(
        rawLot = rawLotSize,
        minLot = minLot,
        maxLot = maxLot,
        lotStep = lotStep
    )
    warnings += lotWarnings

    val actualRiskAmount = effectiveRiskPerLot * normalizedLot
    val expectedRewardAmount = effectiveRewardPerLot * normalizedLot
    val actualRr = if (actualRiskAmount > 0.0) {
        expectedRewardAmount / actualRiskAmount
    } else {
        0.0
    }

    return Mt5LotCalculation(
        requestedRiskAmount = requestedRiskAmount,
        actualRiskAmount = actualRiskAmount,
        stopLossCostPerLot = stopLossCostPerLot,
        totalCostsPerLot = totalCostsPerLot,
        effectiveRiskPerLot = effectiveRiskPerLot,
        effectiveRewardPerLot = effectiveRewardPerLot,
        stopLossDistanceDisplay = slDistanceDisplay,
        takeProfitDistanceDisplay = tpDistanceDisplay,
        distanceUnitLabel = distanceUnitLabel,
        rawLotSize = rawLotSize,
        normalizedLotSize = normalizedLot,
        takeProfitPrice = takeProfitPrice,
        expectedRewardAmount = expectedRewardAmount,
        actualRr = actualRr,
        valuationMode = valuationMode,
        warnings = warnings
    )
}

private fun normalizeLotSize(
    rawLot: Double,
    minLot: Double,
    maxLot: Double,
    lotStep: Double
): Pair<Double, List<String>> {
    val warnings = mutableListOf<String>()
    var boundedLot = rawLot

    if (rawLot < minLot) {
        warnings += "Calculated lot is below broker minimum. Minimum lot applied."
        boundedLot = minLot
    } else if (rawLot > maxLot) {
        warnings += "Calculated lot is above broker maximum. Maximum lot applied."
        boundedLot = maxLot
    }

    val stepsFromMin = floor((boundedLot - minLot) / lotStep + 1e-9)
    val roundedLot = (minLot + max(stepsFromMin, 0.0) * lotStep).coerceIn(minLot, maxLot)

    if (roundedLot + 1e-9 < boundedLot) {
        warnings += "Lot was rounded down to match lot step."
    }

    return roundedLot to warnings
}

private fun formatValue(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%,.${decimals}f", value)
}

private fun formatPrice(price: Double, tickSizeInput: String): String {
    val tickSize = parsePositiveNumber(tickSizeInput)
    val decimals = tickSize?.let { decimalPlacesForIncrement(it) } ?: 5
    return String.format(Locale.US, "%.${decimals}f", price)
}

private fun decimalPlacesForIncrement(increment: Double): Int {
    return BigDecimal.valueOf(increment)
        .stripTrailingZeros()
        .scale()
        .coerceAtLeast(0)
        .coerceAtMost(8)
}

@Preview(showBackground = true)
@Composable
private fun LotCalculatorPreview() {
    MaterialTheme {
        Surface {
            LotCalculatorScreen()
        }
    }
}
