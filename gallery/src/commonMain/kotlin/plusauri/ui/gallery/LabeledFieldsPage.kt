package plusauri.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import plusauri.ui.EnumSelect
import plusauri.ui.FieldLabelPlacement
import plusauri.ui.GroupBoxLabelClearance
import plusauri.ui.LabeledField
import plusauri.ui.LabeledGroupBox
import plusauri.ui.NumericInput
import plusauri.ui.TwoOptionToggle
import plusauri.ui.YearInput

internal const val LabeledFieldsBlurb =
    "LabeledField puts a caption beside or above a control, in place of a floating label. " +
        "LabeledGroupBox draws a titled outline around fields that are read together."

private enum class Frequency { Monthly, Quarterly, Yearly }

@Composable
fun LabeledFieldsPage() = PageColumn {
    var amount by remember { mutableStateOf<Double?>(1_200.0) }
    var frequency by remember { mutableStateOf(Frequency.Monthly) }
    ExampleCard("LabeledField, caption at the start (the default)") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledField("Amount") {
                NumericInput(
                    value = amount,
                    onValueChange = { amount = it },
                    prefix = "$",
                    showSeparator = true,
                    modifier = Modifier.width(140.dp),
                )
            }
            LabeledField("Every") {
                EnumSelect(
                    options = Frequency.entries,
                    value = frequency,
                    onValueChange = { frequency = it },
                )
            }
        }
        Readout("amount: $amount, every: $frequency")
    }

    var start by remember { mutableStateOf<Int?>(2030) }
    var end by remember { mutableStateOf<Int?>(2055) }
    ExampleCard("LabeledField, caption above") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledField("Start year", placement = FieldLabelPlacement.Above) {
                YearInput(value = start, onValueChange = { start = it })
            }
            LabeledField("End year", placement = FieldLabelPlacement.Above) {
                YearInput(value = end, onValueChange = { end = it })
            }
        }
        Readout("start: $start, end: $end")
    }

    var loan by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf<Double?>(6.5) }
    ExampleCard("LabeledGroupBox around a group") {
        LabeledGroupBox("Financing", modifier = Modifier.padding(top = GroupBoxLabelClearance)) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TwoOptionToggle("No loan", "Loan", secondSelected = loan, onSelect = { loan = it })
                LabeledField("Rate") {
                    NumericInput(
                        value = rate,
                        onValueChange = { rate = it },
                        suffix = "%",
                        incrementStep = 0.125,
                        enabled = loan,
                        modifier = Modifier.width(110.dp),
                    )
                }
            }
        }
    }

    ExampleCard("LabeledGroupBox, error and disabled") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledGroupBox("Needs a value", isError = true, modifier = Modifier.padding(top = GroupBoxLabelClearance)) {
                NumericInput(value = null, onValueChange = {}, modifier = Modifier.padding(12.dp).width(120.dp))
            }
            LabeledGroupBox("Disabled", enabled = false, modifier = Modifier.padding(top = GroupBoxLabelClearance)) {
                NumericInput(
                    value = 10.0,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.padding(12.dp).width(120.dp),
                )
            }
        }
    }
}
