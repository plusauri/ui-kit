package plusauri.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import plusauri.ui.IntegerInput
import plusauri.ui.NumberEditKeys
import plusauri.ui.NumericInput
import plusauri.ui.PercentInput
import plusauri.ui.YearInput

internal const val NumericInputBlurb =
    "NumericInput edits a Double; IntegerInput is a thin wrapper that forces whole numbers and " +
        "converts to and from Int."

internal const val PercentInputBlurb =
    "Values are stored as fractions (0.07 = 7%) and shown as percentages. Every NumericInput key " +
        "works here too."

internal const val YearInputBlurb =
    "An integer input held to a range of years, sized for four digits. Up and Down step by one; " +
        "Shift+Up and Shift+Down step the digit at the caret."

@Composable
fun NumericInputPage() = PageColumn {
    ExampleCard("Keys (the field must have focus)") {
        KeyTable()
    }

    var decimal by remember { mutableStateOf<Double?>(1_234.5) }
    ExampleCard("Decimal, no range") {
        NumericInput(
            value = decimal,
            onValueChange = { decimal = it },
            placeholder = "enter amount",
            modifier = Modifier.width(180.dp),
        )
        Readout("value: $decimal")
    }

    var dollars by remember { mutableStateOf<Double?>(50_000.0) }
    ExampleCard("\$ prefix, not negative, range 0 to 1,000,000, step 1,000") {
        NumericInput(
            value = dollars,
            onValueChange = { dollars = it },
            allowNegative = false,
            range = 0.0..1_000_000.0,
            prefix = "$",
            incrementStep = 1_000.0,
            modifier = Modifier.width(180.dp),
        )
        Readout("value: $dollars")
    }

    var rate by remember { mutableStateOf<Double?>(7.5) }
    ExampleCard("% suffix, range -100 to 100, step 0.5") {
        NumericInput(
            value = rate,
            onValueChange = { rate = it },
            range = -100.0..100.0,
            suffix = "%",
            incrementStep = 0.5,
            modifier = Modifier.width(140.dp),
        )
        Readout("value: $rate")
    }

    var years by remember { mutableStateOf<Int?>(30) }
    ExampleCard("IntegerInput, range 1 to 50, suffix \" yr\"") {
        IntegerInput(
            value = years,
            onValueChange = { years = it },
            allowNegative = false,
            range = 1..50,
            suffix = " yr",
            modifier = Modifier.width(120.dp),
        )
        Readout("value: $years")
    }

    var empty by remember { mutableStateOf<Double?>(null) }
    ExampleCard("Starts empty, with a placeholder") {
        NumericInput(
            value = empty,
            onValueChange = { empty = it },
            placeholder = "enter value",
            modifier = Modifier.width(180.dp),
        )
        Readout("value: $empty")
    }

    var bigAmount by remember { mutableStateOf<Double?>(1_234_567.89) }
    ExampleCard("Thousands separator, \$ prefix") {
        NumericInput(
            value = bigAmount,
            onValueChange = { bigAmount = it },
            allowNegative = false,
            prefix = "$",
            showSeparator = true,
            modifier = Modifier.width(200.dp),
        )
        Readout("value: $bigAmount")
    }

    var precise by remember { mutableStateOf<Double?>(1.0 / 3.0) }
    ExampleCard("decimalPlaces = 2: steps round to two places") {
        NumericInput(
            value = precise,
            onValueChange = { precise = it },
            decimalPlaces = 2,
            incrementStep = 0.1,
            modifier = Modifier.width(180.dp),
        )
        Readout("value: $precise")
    }

    var labeled by remember { mutableStateOf<Double?>(250.0) }
    ExampleCard("Floating label, and an error state") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumericInput(
                value = labeled,
                onValueChange = { labeled = it },
                label = "Monthly amount",
                prefix = "$",
                modifier = Modifier.width(180.dp),
            )
            NumericInput(
                value = null,
                onValueChange = {},
                label = "Required",
                isError = true,
                modifier = Modifier.width(180.dp),
            )
        }
    }

    ExampleCard("Disabled") {
        NumericInput(value = 42.0, onValueChange = {}, enabled = false, modifier = Modifier.width(180.dp))
    }
}

@Composable
private fun KeyTable() {
    val k = NumberEditKeys.THOUSANDS
    val m = NumberEditKeys.MILLIONS
    val b = NumberEditKeys.BILLIONS
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KeyRow("Up / Down", "Step the value up or down by the field's step")
        KeyRow("Shift+Up / Shift+Down", "Step the digit at the caret up or down")
        KeyRow(
            "$k / $m / $b",
            "Multiply by a thousand, a million, or a billion. With Shift " +
                "(${k.uppercaseChar()}, ${m.uppercaseChar()}, ${b.uppercaseChar()}), divide instead",
        )
        KeyRow("${NumberEditKeys.NEGATE}", "Flip the sign of the value, wherever the caret is")
        KeyRow("Enter", "Commit the text and show the value as the field formats it")
    }
}

@Composable
private fun KeyRow(key: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            key,
            modifier = Modifier.width(180.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PercentInputPage() = PageColumn {
    var growthRate by remember { mutableStateOf<Double?>(0.07) }
    ExampleCard("No range, not negative (the default)") {
        PercentInput(value = growthRate, onValueChange = { growthRate = it })
        Readout("value: $growthRate")
    }

    var allocation by remember { mutableStateOf<Double?>(0.60) }
    ExampleCard("Range 0% to 100% (0.0..1.0)") {
        PercentInput(value = allocation, onValueChange = { allocation = it }, range = 0.0..1.0)
        Readout("value: $allocation")
    }

    var alpha by remember { mutableStateOf<Double?>(0.025) }
    ExampleCard("Negative allowed, range -50% to 50%") {
        PercentInput(
            value = alpha,
            onValueChange = { alpha = it },
            allowNegative = true,
            range = -0.5..0.5,
        )
        Readout("value: $alpha")
    }

    var precise by remember { mutableStateOf<Double?>(0.07) }
    ExampleCard("decimalPlaces = 2") {
        PercentInput(value = precise, onValueChange = { precise = it }, decimalPlaces = 2)
        Readout("value: $precise")
    }

    var labeled by remember { mutableStateOf<Double?>(0.04) }
    ExampleCard("Floating label") {
        PercentInput(value = labeled, onValueChange = { labeled = it }, label = "Withdrawal rate")
        Readout("value: $labeled")
    }

    ExampleCard("Disabled") {
        PercentInput(value = 0.035, onValueChange = {}, enabled = false)
    }
}

@Composable
fun YearInputPage() = PageColumn {
    var birthYear by remember { mutableStateOf<Int?>(1990) }
    ExampleCard("Default range 1900 to 2400, no label") {
        YearInput(value = birthYear, onValueChange = { birthYear = it })
        Readout("value: $birthYear")
    }

    var birthYearLabeled by remember { mutableStateOf<Int?>(1985) }
    ExampleCard("Floating label on the border") {
        YearInput(value = birthYearLabeled, onValueChange = { birthYearLabeled = it }, label = "Birth year")
        Readout("value: $birthYearLabeled")
    }

    var retirementYear by remember { mutableStateOf<Int?>(2045) }
    ExampleCard("Range 2025 to 2075, placeholder \"Retire\"") {
        YearInput(
            value = retirementYear,
            onValueChange = { retirementYear = it },
            yearRange = 2025..2075,
            placeholder = "Retire",
        )
        Readout("value: $retirementYear")
    }

    var blank by remember { mutableStateOf<Int?>(null) }
    ExampleCard("Empty: shows the host's placeholder word (LocalYearInputWords)") {
        YearInput(value = blank, onValueChange = { blank = it })
        Readout("value: $blank")
    }

    ExampleCard("Disabled") {
        YearInput(value = 2024, onValueChange = {}, enabled = false)
    }
}
