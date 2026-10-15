package com.example.expense_tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expense_tracker.ui.main.MainScreen
import com.example.expense_tracker.ui.receipt.AddReceiptScreen
import com.example.expense_tracker.ui.reports.ReportsScreen
import com.example.expense_tracker.ui.settings.SettingsScreen

// Definicja tras nawigacji
sealed class Destination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Main : Destination("main", "Główny", Icons.Default.Home)
    object AddReceipt : Destination("add_receipt", "Paragon", Icons.Default.AddAPhoto)
    object AddReceiptv2 : Destination("add_receipt", "Paragon", Icons.Default.AddAPhoto)
    object Reports : Destination("reports", "Raporty", Icons.Default.BarChart)
    object Settings : Destination("settings", "Ustawienia", Icons.Default.Settings)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val bottomBarItems = listOf(
        Destination.Main,
        Destination.AddReceipt,
        Destination.Reports,
        Destination.Settings,
        Destination.AddReceiptv2
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                items = bottomBarItems
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Main.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Main.route) {
                MainScreen()
            }
            composable(Destination.AddReceipt.route) {
                AddReceiptScreen()
            }
            composable(Destination.Reports.route) {
                ReportsScreen()
            }
            composable(Destination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}