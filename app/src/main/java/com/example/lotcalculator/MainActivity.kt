package com.example.lotcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

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

private data class LotCalculation(
    val riskAmount: Double,
    val lotSize: Double
)

@Composable
private fun LotCalculatorScreen() {
    var balanceInput by rememberSaveable { mutableStateOf("") }
    var riskInput by rememberSaveable { mutableStateOf("1") }
    var stopLossInput by rememberSaveable { mutableStateOf("") }
    var pipValueInput by rememberSaveable { mutableStateOf("10") }

    var calculationResult by remember { mutableStateOf<LotCalculation?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Lot Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Calculate position size based on account risk.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = balanceInput,
            onValueChange = { balanceInput = it },
            label = { Text("Account Balance") },
            placeholder = { Text("e.g. 10000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = riskInput,
            onValueChange = { riskInput = it },
            label = { Text("Risk % per trade") },
            placeholder = { Text("e.g. 1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = stopLossInput,
            onValueChange = { stopLossInput = it },
            label = { Text("Stop Loss (pips)") },
            placeholder = { Text("e.g. 25") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pipValueInput,
            onValueChange = { pipValueInput = it },
            label = { Text("Pip Value per 1.0 Lot") },
            placeholder = { Text("e.g. 10") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMessage = null
                calculationResult = null

                val balance = parsePositiveNumber(balanceInput)
                val riskPercent = parsePositiveNumber(riskInput)
                val stopLossPips = parsePositiveNumber(stopLossInput)
                val pipValue = parsePositiveNumber(pipValueInput)

                if (balance == null || riskPercent == null || stopLossPips == null || pipValue == null) {
                    errorMessage = "Please enter valid values greater than zero for all fields."
                    return@Button
                }

                calculationResult = calculateLotSize(
                    balance = balance,
                    riskPercent = riskPercent,
                    stopLossPips = stopLossPips,
                    pipValuePerLot = pipValue
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

        if (calculationResult != null) {
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
                        text = "Risk Amount: ${formatValue(calculationResult!!.riskAmount)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Recommended Lot Size: ${formatValue(calculationResult!!.lotSize, 2)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun parsePositiveNumber(rawValue: String): Double? {
    val normalized = rawValue.trim().replace(",", ".")
    val parsed = normalized.toDoubleOrNull() ?: return null
    return parsed.takeIf { it > 0.0 }
}

private fun calculateLotSize(
    balance: Double,
    riskPercent: Double,
    stopLossPips: Double,
    pipValuePerLot: Double
): LotCalculation {
    val riskAmount = balance * (riskPercent / 100.0)
    val lotSize = riskAmount / (stopLossPips * pipValuePerLot)
    val roundedLotSize = (lotSize * 100).roundToInt() / 100.0
    return LotCalculation(riskAmount = riskAmount, lotSize = roundedLotSize)
}

private fun formatValue(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%,.${decimals}f", value)
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
