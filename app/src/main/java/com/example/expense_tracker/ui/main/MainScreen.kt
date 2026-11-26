package com.example.expense_tracker.ui.main



import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onManageCategoriesClick: () -> Unit = {},
    onExpenseClick: (String) -> Unit = {},
    onSeeAllExpensesClick: () -> Unit = {},
    onManageRecurringClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Komunikat po dodaniu opłaty cyklicznej do wydatków
    LaunchedEffect(state.paymentMessage) {
        state.paymentMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumePaymentMessage()
        }
    }

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
        // Bez spacedBy: odstępy dodaje section(), dzięki czemu wiersze
        // nadchodzących płatności mogą być osobnymi elementami (animateItem)
        // i nadal wyglądać jak jedna karta.
    ) {
        section("header") { HeaderSection(state.userName, state.currentDate) }
        section("budget") { BudgetCard(state.budget, state.currentMonth) }
        section("daily_stats") { DailyStatsRow(state.budget) }

        section("recent_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ostatnie wydatki",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Zobacz wszystkie",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSeeAllExpensesClick() }
                )
            }
        }
        section("recent_list") { TransactionsList(state.recentTransactions, onExpenseClick = onExpenseClick) }
        section("top_category") { TopCategoryCard(state) }

        section("upcoming_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nadchodzące płatności",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Płynna zmiana licznika "X płatności w tym tygodniu"
                    AnimatedContent(
                        targetState = upcomingThisWeekText(state.upcomingThisWeekCount),
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                        label = "upcoming_count"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Zarządzaj",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onManageRecurringClick() }
                )
            }
        }

        if (state.upcomingPayments.isEmpty()) {
            item(key = "upcoming_empty") {
                Box(
                    Modifier
                        .animateItem()
                        .padding(bottom = SectionSpacing)
                ) {
                    EmptyUpcomingPaymentsCard(onClick = onManageRecurringClick)
                }
            }
        } else {
            // Każda płatność to osobny element listy → animowane pojawianie się,
            // znikanie i przesuwanie (np. po kliknięciu "+" opłata zjeżdża niżej)
            val payments = state.upcomingPayments
            itemsIndexed(payments, key = { _, p -> "upcoming_${p.id}" }) { index, payment ->
                UpcomingPaymentListItem(
                    payment = payment,
                    isFirst = index == 0,
                    isLast = index == payments.lastIndex,
                    isPaying = payment.id in state.payingPaymentIds,
                    onPayClick = { viewModel.onPayRecurringPayment(payment.id) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(300),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = tween(200)
                    )
                )
            }
        }

        // Element jest zawsze obecny – pokazuje/ukrywa się animacją
        item(key = "overdue_alert") {
            AnimatedOverdueAlert(
                payment = state.overduePayment,
                onClick = onManageRecurringClick
            )
        }

        section("manage_categories", bottomSpacing = 0.dp) {
            ManageCategoriesCard(onManageCategoriesClick = onManageCategoriesClick)
        }
    }
}

private val SectionSpacing = 16.dp

/** Element listy z odstępem pod spodem (zastępuje Arrangement.spacedBy). */
private fun LazyListScope.section(
    key: String,
    bottomSpacing: Dp = SectionSpacing,
    content: @Composable () -> Unit
) {
    item(key = key) {
        Box(Modifier.padding(bottom = bottomSpacing)) { content() }
    }
}

private fun upcomingThisWeekText(count: Int): String = when {
    count == 0 -> "Brak płatności w tym tygodniu"
    count == 1 -> "1 płatność w tym tygodniu"
    else -> "$count płatności w tym tygodniu"
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
fun TransactionsList(
    transactions: List<Transaction>,
    onExpenseClick: (String) -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            transactions.forEach { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onExpenseClick = onExpenseClick
                )
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
fun TransactionRow(
    transaction: Transaction,
    onExpenseClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick(transaction.id) }
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

// -- widget nadchodzace platnosci (opłaty cykliczne z bazy)
@Composable
fun UpcomingPaymentsList(
    payments: List<UpcomingPayment>,
    payingIds: Set<String> = emptySet(),
    onPayClick: (String) -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            payments.forEach { payment ->
                UpcomingPaymentRow(
                    payment = payment,
                    isPaying = payment.id in payingIds,
                    onPayClick = { onPayClick(payment.id) }
                )
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
fun UpcomingPaymentRow(
    payment: UpcomingPayment,
    isPaying: Boolean = false,
    onPayClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dateParts = payment.date.split(" ")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    if (payment.isOverdue) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val dateColor = if (payment.isOverdue) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = dateParts.getOrElse(0) { "" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = dateColor
            )
            Text(
                text = dateParts.getOrElse(1) { "" },
                fontSize = 10.sp,
                color = dateColor
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
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

        Spacer(modifier = Modifier.width(4.dp))

        // "+" – dodaje opłatę do listy wydatków
        FilledTonalIconButton(
            onClick = onPayClick,
            enabled = !isPaying,
            modifier = Modifier.size(40.dp)
        ) {
            if (isPaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj ${payment.title} do wydatków"
                )
            }
        }
    }
}

/**
 * Pojedyncza płatność jako osobny element LazyColumn.
 * Zaokrąglenia i separator zależą od pozycji, więc razem wyglądają jak jedna karta.
 */
@Composable
fun UpcomingPaymentListItem(
    payment: UpcomingPayment,
    isFirst: Boolean,
    isLast: Boolean,
    isPaying: Boolean,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radius = 24.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) radius else 0.dp,
        topEnd = if (isFirst) radius else 0.dp,
        bottomStart = if (isLast) radius else 0.dp,
        bottomEnd = if (isLast) radius else 0.dp
    )

    Column(modifier = modifier.padding(bottom = if (isLast) SectionSpacing else 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            UpcomingPaymentRow(
                payment = payment,
                isPaying = isPaying,
                onPayClick = onPayClick
            )
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/**
 * Alert o zaległej płatności: wysuwa się i zanika płynnie.
 * Ostatnia wartość jest zapamiętana, żeby podczas animacji znikania
 * alert wciąż miał co wyświetlić.
 */
@Composable
fun AnimatedOverdueAlert(
    payment: UpcomingPayment?,
    onClick: () -> Unit = {}
) {
    var lastPayment by remember { mutableStateOf(payment) }
    if (payment != null && payment != lastPayment) lastPayment = payment

    AnimatedVisibility(
        visible = payment != null,
        enter = fadeIn(tween(300)) +
                expandVertically(tween(300), expandFrom = Alignment.Top) +
                slideInVertically(tween(300)) { -it / 3 },
        exit = fadeOut(tween(200)) +
                shrinkVertically(tween(250), shrinkTowards = Alignment.Top)
    ) {
        lastPayment?.let {
            Box(Modifier.padding(bottom = SectionSpacing)) {
                OverduePaymentAlert(payment = it, onClick = onClick)
            }
        }
    }
}

@Composable
fun EmptyUpcomingPaymentsCard(onClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Brak opłat cyklicznych",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Dodaj czynsz, abonamenty i subskrypcje",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OverduePaymentAlert(
    payment: UpcomingPayment,
    onClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delikatne pulsowanie ikony ostrzeżenia
            val pulse = rememberInfiniteTransition(label = "overdue_pulse")
            val iconScale by pulse.animateFloat(
                initialValue = 1f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "overdue_icon_scale"
            )
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
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
                    text = "${payment.title} • ${"%.2f".format(Locale("pl", "PL"), payment.amount)} zł • ${payment.daysLeft}",
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