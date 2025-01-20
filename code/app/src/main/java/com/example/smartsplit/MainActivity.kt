package com.example.smartsplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartsplit.ui.theme.SmartSplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSplitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BillSplitterApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BillSplitterApp(modifier: Modifier = Modifier) {
    var totalBill by remember { mutableStateOf("") }
    var numPeople by remember { mutableStateOf("") }
    var tipPercentage by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Input: Total Bill Amount
        OutlinedTextField(
            value = totalBill,
            onValueChange = { totalBill = it },
            label = { Text("Total Bill Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Input: Number of People
        OutlinedTextField(
            value = numPeople,
            onValueChange = { numPeople = it },
            label = { Text("Number of People") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Input: Tip Percentage
        OutlinedTextField(
            value = tipPercentage,
            onValueChange = { tipPercentage = it },
            label = { Text("Tip Percentage (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Calculate Button
        Button(
            onClick = {
                val bill = totalBill.toDoubleOrNull()
                val people = numPeople.toIntOrNull()
                val tip = tipPercentage.toDoubleOrNull() ?: 0.0

                if (bill == null || bill <= 0) {
                    result = "Please enter a valid total bill amount."
                } else if (people == null || people <= 0) {
                    result = "Please enter a valid number of people."
                } else {
                    val tipAmount = bill * (tip / 100)
                    val totalAmount = bill + tipAmount
                    val amountPerPerson = totalAmount / people
                    result = """
                        Total Bill: $%.2f
                        Tip Amount: $%.2f
                        Total Amount: $%.2f
                        Each Person Owes: $%.2f
                    """.trimIndent().format(bill, tipAmount, totalAmount, amountPerPerson)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        // Result Display
        Text(
            text = result,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BillSplitterPreview() {
    SmartSplitTheme {
        BillSplitterApp()
    }
}
