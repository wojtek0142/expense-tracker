package com.example.expense_tracker.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expense_tracker.ui.theme.ExpensetrackerTheme
private val BudgetGreen = Color(0xFF4CAF50)
private val BudgetRed   = Color(0xFFE53935)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        budget = uiState.budget,
        spent = uiState.spent,
        recentExpenses = uiState.recentExpenses,
        onBudgetChange = viewModel::onBudgetChange // todo: przesunac do settings
    )
}

@Composable
private fun MainScreenContent(
    budget: String,
    spent: Double,
    recentExpenses: List<ExpenseRow>,
    onBudgetChange: (String) -> Unit
) {
    val budgetValue = budget.toDoubleOrNull() ?: 0.0
    val remaining = (budgetValue - spent).coerceAtLeast(0.0)
    val usedFraction = if (budgetValue > 0) {
        (spent / budgetValue).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ------------------------------------------------------------
        // 1. Budżet na ten miesiąc + pole
        // ------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budżet na ten miesiąc:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = budget,
                onValueChange = onBudgetChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Podsumowanie liczbowe
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Wykorzystano: %.2f zł".format(spent),
                color = BudgetRed,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Pozostało: %.2f zł".format(remaining),
                color = BudgetGreen,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ------------------------------------------------------------
        // 2. Pasek wypełnienia (zielony = pozostało, czerwony = użyte)
        // ------------------------------------------------------------
        BudgetBar(
            usedFraction = usedFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ------------------------------------------------------------
        // 3. Tabela: Ostatnie wydatki
        // ------------------------------------------------------------
        Text(
            text = "Ostatnie wydatki",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Nagłówek tabeli
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Data",
                        modifier = Modifier.weight(0.28f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Przedmiot",
                        modifier = Modifier.weight(0.52f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Cena",
                        modifier = Modifier.weight(0.20f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Wiersze z ViewModelu
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentExpenses) { expense ->
                        ExpenseRowItem(expense)
                    }
                }
            }
        }
    }
}

// ============================================================
// Pasek budżetu
// ============================================================
@Composable
private fun BudgetBar(
    usedFraction: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BudgetGreen)
    ) {
        // Czerwona część – od lewej, proporcjonalna do wykorzystania
        Box(
            modifier = Modifier
                .fillMaxWidth(usedFraction)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BudgetRed)
        )
    }
}

// ============================================================
// Pojedynczy wiersz wydatku
// ============================================================
@Composable
private fun ExpenseRowItem(expense: ExpenseRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = expense.date,
            modifier = Modifier.weight(0.28f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = expense.productName,
            modifier = Modifier.weight(0.52f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "%.2f zł".format(expense.amount),
            modifier = Modifier.weight(0.20f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================
// Podgląd w Android Studio
// ============================================================
@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    ExpensetrackerTheme {
        MainScreenContent(
            budget = "2000",
            spent = 1500.0,
            recentExpenses = listOf(
                ExpenseRow("2026-10-15", "Chleb", 4.50),
                ExpenseRow("2026-10-14", "Mleko", 3.20),
                ExpenseRow("2026-10-13", "Masło", 7.99),
                ExpenseRow("2026-10-12", "Ser", 12.40),
                ExpenseRow("2026-10-11", "Szynka", 9.80)
            ),
            onBudgetChange = {}
        )
    }
}
