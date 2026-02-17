package com.example.lotcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
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

private data class Mt5LotCalculation(
    val requestedRiskAmount: Double,
    val actualRiskAmount: Double,
    val stopLossCostPerLot: Double,
    val stopLossPoints: Double,
    val rawLotSize: Double,
    val normalizedLotSize: Double,
    val takeProfitPrice: Double,
    val expectedRewardAmount: Double,
    val actualRr: Double,
    val warnings: List<String>
)

@Composable
private fun LotCalculatorScreen() {
    var tradeSideName by rememberSaveable { mutableStateOf(TradeSide.BUY.name) }
    var riskModeName by rememberSaveable { mutableStateOf(RiskMode.PERCENT.name) }
    var tpModeName by rememberSaveable { mutableStateOf(TakeProfitMode.FIXED_RR.name) }

    var balanceInput by rememberSaveable { mutableStateOf("10000") }
    var riskPercentInput by rememberSaveable { mutableStateOf("1") }
    var fixedRiskInput by rememberSaveable { mutableStateOf("100") }

    var entryPriceInput by rememberSaveable { mutableStateOf("1.10000") }
    var stopLossPriceInput by rememberSaveable { mutableStateOf("1.09500") }
    var rrInput by rememberSaveable { mutableStateOf("2") }
    var manualTpPriceInput by rememberSaveable { mutableStateOf("1.11000") }

    var tickSizeInput by rememberSaveable { mutableStateOf("0.0001") }
    var tickValueInput by rememberSaveable { mutableStateOf("10") }
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
            text = "Dynamic lot sizing with risk mode, fixed R:R and broker limits.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))
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
        SectionTitle("MT5 Symbol Settings")
        NumericInputField(
            value = tickSizeInput,
            onValueChange = { tickSizeInput = it },
            label = "Tick Size",
            placeholder = "e.g. 0.0001"
        )

        Spacer(modifier = Modifier.height(12.dp))
        NumericInputField(
            value = tickValueInput,
            onValueChange = { tickValueInput = it },
            label = "Tick Value (for 1.0 lot)",
            placeholder = "e.g. 10"
        )

        Spacer(modifier = Modifier.height(12.dp))
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

                val entryPrice = parsePositiveNumber(entryPriceInput)
                val stopLossPrice = parsePositiveNumber(stopLossPriceInput)
                val tickSize = parsePositiveNumber(tickSizeInput)
                val tickValue = parsePositiveNumber(tickValueInput)
                val minLot = parsePositiveNumber(minLotInput)
                val lotStep = parsePositiveNumber(lotStepInput)
                val maxLot = parsePositiveNumber(maxLotInput)

                if (
                    entryPrice == null ||
                    stopLossPrice == null ||
                    tickSize == null ||
                    tickValue == null ||
                    minLot == null ||
                    lotStep == null ||
                    maxLot == null
                ) {
                    errorMessage = "Please enter valid values greater than zero for all required fields."
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

                calculationResult = calculateMt5Lot(
                    tradeSide = tradeSide,
                    requestedRiskAmount = requestedRiskAmount,
                    entryPrice = entryPrice,
                    stopLossPrice = stopLossPrice,
                    takeProfitPrice = takeProfitPrice,
                    tickSize = tickSize,
                    tickValue = tickValue,
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
                        text = "Requested Risk: ${formatValue(result.requestedRiskAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Actual Risk (normalized lot): ${formatValue(result.actualRiskAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "SL Cost per 1.0 Lot: ${formatValue(result.stopLossCostPerLot)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "SL Distance: ${formatValue(result.stopLossPoints, 1)} points",
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

private fun calculateMt5Lot(
    tradeSide: TradeSide,
    requestedRiskAmount: Double,
    entryPrice: Double,
    stopLossPrice: Double,
    takeProfitPrice: Double,
    tickSize: Double,
    tickValue: Double,
    minLot: Double,
    maxLot: Double,
    lotStep: Double
): Mt5LotCalculation {
    val stopLossDistance = abs(entryPrice - stopLossPrice)
    val stopLossPoints = stopLossDistance / tickSize
    val stopLossCostPerLot = stopLossPoints * tickValue
    val rawLotSize = requestedRiskAmount / stopLossCostPerLot

    val (normalizedLot, warnings) = normalizeLotSize(
        rawLot = rawLotSize,
        minLot = minLot,
        maxLot = maxLot,
        lotStep = lotStep
    )

    val actualRiskAmount = stopLossCostPerLot * normalizedLot

    val takeProfitDistance = if (tradeSide == TradeSide.BUY) {
        takeProfitPrice - entryPrice
    } else {
        entryPrice - takeProfitPrice
    }
    val takeProfitPoints = takeProfitDistance / tickSize
    val rewardPerLot = takeProfitPoints * tickValue
    val expectedRewardAmount = rewardPerLot * normalizedLot
    val actualRr = expectedRewardAmount / actualRiskAmount

    return Mt5LotCalculation(
        requestedRiskAmount = requestedRiskAmount,
        actualRiskAmount = actualRiskAmount,
        stopLossCostPerLot = stopLossCostPerLot,
        stopLossPoints = stopLossPoints,
        rawLotSize = rawLotSize,
        normalizedLotSize = normalizedLot,
        takeProfitPrice = takeProfitPrice,
        expectedRewardAmount = expectedRewardAmount,
        actualRr = actualRr,
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
