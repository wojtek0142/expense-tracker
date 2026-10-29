package com.example.expense_tracker.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Kolory z designu
val DarkGreen = Color(0xFF1B4332)
val LightGreen = Color(0xFFD4E88C)
val BackgroundLight = Color(0xFFF9FBF2)
val TextGray = Color(0xFF6B7280)

@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newItem by remember { mutableStateOf("") }
    val expenseItems = remember { mutableStateListOf<String>() }

    // Reakcja na zapis
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Niestandardowy TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOWA TRANSAKCJA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Text(
                        text = "Dodaj wydatek",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Przycisk zamknięcia
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Karta z kwotą
            AmountCard(
                amount = uiState.amount,
                onAmountChange = viewModel::onAmountChange
            )

            // 2. Nazwa wydatku
            InputLabel("Nazwa wydatku")
            CustomTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = "np. Zakupy spożywcze",
                leadingIcon = Icons.Outlined.EditNote
            )

            // 3. Data i Kategoria (Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Data
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("Data")
                    CustomTextField(
                        value = uiState.date,
                        onValueChange = {},
                        readOnly = true, // W prawdziwej apce otwierałby DatePicker
                        trailingIcon = Icons.Default.CalendarToday
                    )
                }
                // Kategoria
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("Kategoria")
                    CustomTextField(
                        value = uiState.category,
                        onValueChange = {},
                        readOnly = true, // Otwierałby dropdown
                        placeholder = "Wybierz",
                        trailingIcon = Icons.Default.ArrowDropDown
                    )
                }
            }

            // 4. Typ wydatku (Przyciski)
            InputLabel("typ wydatku")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseTypeButton(
                    text = "jednorazowy",
                    isSelected = uiState.expenseType == ExpenseType.FIXED,
                    onClick = { viewModel.onExpenseTypeChange(ExpenseType.FIXED) },
                    modifier = Modifier.weight(1f)
                )
                ExpenseTypeButton(
                    text = "okresowy",
                    isSelected = uiState.expenseType == ExpenseType.CASH,
                    onClick = { viewModel.onExpenseTypeChange(ExpenseType.CASH) },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = viewModel::addExpenseToList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dodaj wydatek",
                        color = DarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }



            // 5. Notatka
            InputLabel("Wydatki do zapisania")

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.pendingExpenses.forEach { expense ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.title,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "${expense.amount} zł • ${expense.category}",
                                color = TextGray
                            )

                            Text(
                                text = "${expense.date} • ${expense.expenseType}",
                                color = TextGray,
                                fontSize = 12.sp
                            )

                            if (expense.note.isNotBlank()) {
                                Text(
                                    text = expense.note,
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.removeExpenseFromList(expense.id)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Usuń wydatek",
                                tint = TextGray
                            )
                        }
                    }
                }
            }

// Pole do wpisywania nowej pozycji
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Np. chleb, mleko...", color = Color.LightGray)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = LightGreen,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                IconButton(
                    onClick = {
                        val item = newItem.trim()

                        if (item.isNotEmpty()) {
                            expenseItems.add(item)
                            newItem = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj pozycję",
                        tint = DarkGreen
                    )
                }
            }

// Lista dodanych pozycji


            Spacer(modifier = Modifier.height(8.dp))

            // 6. Przycisk Zapisz
            Button(
                onClick = viewModel::saveAllExpenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zapisz wszystkie",
                        color = DarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Komponenty pomocnicze ---

@Composable
fun AmountCard(amount: String, onAmountChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2D5A46), DarkGreen)
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Kwota wydatku",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 48.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                text = "0,00",
                                fontSize = 48.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        innerTextField()
                    }
                )
                Text(
                    text = "zł",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextGray,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.LightGray) },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = Color.LightGray) } },
        trailingIcon = trailingIcon?.let { { Icon(it, contentDescription = null, tint = Color.Gray) } },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        readOnly = readOnly,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun ExpenseTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) LightGreen else Color.White
    val borderColor = if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.3f)
    val textColor = if (isSelected) DarkGreen else TextGray

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}