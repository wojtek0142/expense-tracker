package com.example.expense_tracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ustawienia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BUDŻET MIESIĘCZNY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Wybór miesiąca
                    Box {
                        OutlinedTextField(
                            value = state.selectedMonthLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Miesiąc") },
                            trailingIcon = {
                                IconButton(onClick = { showMonthPicker = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showMonthPicker = true }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Kwota
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Kwota (zł)") },
                        placeholder = { Text("np. 4200,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = viewModel::saveBudget,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Zapisz budżet")
                        }
                    }

                    state.message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                    state.errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Zapisane budżety",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.budgets.isEmpty()) {
            item {
                Text(
                    text = "Brak zapisanych budżetów.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            items(state.budgets, key = { it.month }) { entry ->
                BudgetRow(
                    entry = entry,
                    onClick = { viewModel.onMonthSelected(entry.month) },
                    onDelete = { viewModel.deleteBudget(entry.month) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialMonthKey = state.selectedMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { key ->
                viewModel.onMonthSelected(key)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun BudgetRow(
    entry: BudgetEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.monthLabel,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${"%.2f".format(Locale("pl", "PL"), entry.amount)} zł",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.isCurrentMonth) {
                AssistChip(
                    onClick = {},
                    label = { Text("Bieżący", fontSize = 10.sp) }
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń budżet",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MonthYearPickerDialog(
    initialMonthKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val locale = Locale("pl", "PL")
    val parts = initialMonthKey.split("-")
    var year by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 2025) }
    var monthIndex by remember {
        mutableStateOf(((parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1).coerceIn(0, 11))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val key = String.format(Locale.US, "%04d-%02d", year, monthIndex + 1)
                onConfirm(key)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
        title = { Text("Wybierz miesiąc") },
        text = {
            Column {
                // Rok
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { year-- }) { Text("−") }
                    Text("$year", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { year++ }) { Text("+") }
                }

                Spacer(Modifier.height(8.dp))

                // Miesiące w gridzie 3x4
                val monthNames = (0..11).map { m ->
                    val cal = Calendar.getInstance().apply {
                        clear()
                        set(year, m, 1)
                    }
                    cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
                        .uppercase(locale)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val selected = index == monthIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { monthIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthNames[index].take(3),
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}