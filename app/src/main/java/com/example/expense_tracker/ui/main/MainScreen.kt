package com.example.expense_tracker.ui.main



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expense_tracker.ui.theme.*
import java.util.Locale


@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
    onManageCategoriesClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeaderSection(state.userName, state.currentDate) }
        item { BudgetCard(state.budget, state.currentMonth) }
        item { DailyStatsRow(state.budget) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val count by viewModel.monthlyExpenseCount.collectAsStateWithLifecycle()
                Text(
                    text = "Ostatnie wydatki",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "W tym miesiącu: $count wydatków",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { TransactionsList(state.recentTransactions) }
        item { TopCategoryCard(state) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nadchodzące płatności",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "3 płatności w tym tygodniu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { UpcomingPaymentsList(state.upcomingPayments) }

        if (state.hasOverduePayment) {
            item { OverduePaymentAlert() }
        }

        item { ManageCategoriesCard(onManageCategoriesClick = onManageCategoriesClick) }
    }
}

@Composable
fun ManageCategoriesCard(onManageCategoriesClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManageCategoriesClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Kategorie",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Zarządzaj kategoriami",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dodawaj i przeglądaj kategorie wydatków",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



// --- SEKCJE ---

@Composable
fun HeaderSection( userName: String, currentDate: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dzień dobry, $userName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Menu",
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---widget z budzetem na dany miesiac---
@Composable
fun BudgetCard(budget: BudgetState, currentMonth: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentMonth,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pozostało do wykorzystania",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.2f", budget.remaining).replace('.', ','),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val progress = (budget.percentageUsed / 100f).coerceIn(0f, 1f)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(progress)
                                .fillMaxHeight()
                                .background(RedAlert)
                        )
                    }
                    if (progress < 1f) {
                        Box(
                            modifier = Modifier
                                .weight(1f - progress)
                                .fillMaxHeight()
                                .background(AccentGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wydano ${budget.spent} zł",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "${budget.percentageUsed}%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val change = budget.monthOverMonthChangePercent
                        val changeAmount = budget.monthOverMonthChangeAmount

                        val changeText = when {
                            change == null -> "Brak danych z poprzedniego miesiąca"
                            change > 0.0   -> "+${"%.0f".format(change)}% więcej niż miesiąc temu"
                            change < 0.0   -> "${"%.0f".format(change)}% mniej niż miesiąc temu"
                            else           -> "Tyle samo co miesiąc temu"
                        }
                        val changeColor = when {
                            change == null -> Color.Gray
                            change < 0.0   -> Color(0xFF2E7D32) // zielony
                            change > 0.0   -> Color(0xFFC62828) // czerwony
                            else           -> Color.Gray
                        }

                        val changeAmountText = when {
                            changeAmount == null -> "Brak danych z poprzedniego miesiąca"
                            changeAmount > 0.0 -> "+${"%.2f".format(Locale("pl", "PL"), changeAmount)} zł więcej niż miesiąc temu"
                            changeAmount < 0.0 -> "${"%.2f".format(Locale("pl", "PL"), -changeAmount)} zł mniej niż miesiąc temu"  // <-- -changeAmount
                            else -> "Tyle samo co miesiąc temu"
                        }

                        Text(
                            text = changeText ,
                            color = changeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = changeAmountText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ---widget srednie dzienne wydatki---
@Composable
fun DailyStatsRow(budget: BudgetState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val projectedSpent = budget.projectedSpent
        val overUnder = budget.projectedOverUnder
        val overUnderPercent = budget.projectedOverUnderPercent

        val forecastText = when {
            overUnder > 0.0 ->
                "Prognoza: ${"%.2f".format(projectedSpent)} zł " +
                        "(przekroczysz o ${"%.2f".format(overUnder)} zł / " +
                        "+${"%.0f".format(overUnderPercent)}%)"

            overUnder < 0.0 ->
                "Prognoza: ${"%.2f".format(projectedSpent)} zł " +
                        "(zaoszczędzisz ${"%.2f".format(-overUnder)} zł / " +
                        "${"%.0f".format(overUnderPercent)}%)"

            else ->
                "Prognoza: ${"%.2f".format(projectedSpent)} zł (dokładnie w budżecie)"
        }

        val forecastColor = when {
            overUnder > 0.0 -> Color(0xFFC62828)  // czerwony – przekroczenie
            overUnder < 0.0 -> Color(0xFF2E7D32)  // zielony – oszczędność
            else            -> Color.Gray
        }
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.BarChart,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            iconBg = MaterialTheme.colorScheme.errorContainer,
            title = "Średnio dziennie",
            value = "${String.format("%.2f", budget.dailyAverage).replace('.', ',')} zł",
            subtitle = forecastText,
            subtitleColor = forecastColor
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.AccountBalanceWallet,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            iconBg = MaterialTheme.colorScheme.secondaryContainer,
            title = "Możesz wydać",
            value = "${String.format("%.2f", budget.canSpendDaily).replace('.', ',')} zł",
            subtitle = "dziennie do końca miesiąca",
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---- widget pomocniczy dla srednich dziennych ----
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 10.sp, color = subtitleColor, lineHeight = 14.sp)
        }
    }
}

@Composable
fun TransactionsList(transactions: List<Transaction>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            transactions.forEach { transaction ->
                TransactionRow(transaction)
                if (transaction != transactions.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colors = when (transaction.iconType) {
            IconType.FOOD -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            IconType.TRANSPORT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            IconType.SHOPPING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
        val bgColor = colors.first
        val tint = colors.second

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (transaction.iconType) {
                IconType.FOOD -> Icons.Default.Restaurant
                IconType.TRANSPORT -> Icons.Default.DirectionsCar
                IconType.SHOPPING -> Icons.Default.ShoppingBag
                else -> Icons.Default.AttachMoney
            }
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${transaction.category} • ${transaction.date}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "- ${String.format("%.2f", transaction.amount).replace('.', ',')} zł",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error
        )
    }
}


// --- widged z najczesciej uzywana kategoria ---
@Composable
fun TopCategoryCard(state: MainScreenState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TOP KATEGORIA MIESIĄCA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.topCategory,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.topCategoryAmount > 0.0) {
                        "${"%.2f".format(Locale("pl", "PL"), state.topCategoryAmount)} zł • ${state.topCategoryPercent}% wydatków"
                    } else {
                        "Brak wydatków w tym miesiącu"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategory(state.topCategory),   // <-- dynamiczna ikona
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// -- widget nadchodzace platnosci
@Composable
fun UpcomingPaymentsList(payments: List<UpcomingPayment>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            payments.forEach { payment ->
                UpcomingPaymentRow(payment)
                if (payment != payments.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

// -- pojedynczy wpis w nadchodzacych platnosciach ---
@Composable
fun UpcomingPaymentRow(payment: UpcomingPayment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = payment.date.split(" ")[0],
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = payment.date.split(" ")[1],
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payment.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Autorenew,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = payment.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${String.format("%.2f", payment.amount).replace('.', ',')} zł",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = payment.daysLeft,
                fontSize = 11.sp,
                color = if (payment.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OverduePaymentAlert() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Zaległa płatność",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp
                )
                Text(
                    text = "Ubezpieczenie • 89 zł • 3 dni po terminie",
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun iconForCategory(categoryName: String): ImageVector {
    val key = categoryName.lowercase(Locale("pl", "PL"))
    return when {
        key.contains("jedzen") || key.contains("restaur") -> Icons.Default.Restaurant
        key.contains("transport") || key.contains("paliw") -> Icons.Default.DirectionsCar
        key.contains("zakup") -> Icons.Default.ShoppingCart
        key.contains("dom") || key.contains("czynsz") -> Icons.Default.Home
        key.contains("subskryp") || key.contains("abonament") -> Icons.Default.Subscriptions
        key.contains("telefon") || key.contains("komórk") -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Category
    }
}