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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expense_tracker.ui.main.MainScreen
import com.example.expense_tracker.ui.main.MainViewModel
import com.example.expense_tracker.ui.receipt.AddReceiptScreen
import com.example.expense_tracker.ui.reports.AllCategoriesScreen
import com.example.expense_tracker.ui.reports.ReportsScreen
import com.example.expense_tracker.ui.reports.ReportsViewModel
import com.example.expense_tracker.ui.settings.SettingsScreen

// Definicja tras nawigacji
sealed class Destination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Main : Destination("main", "Główny", Icons.Default.Home)
    object AddReceipt : Destination("add_receipt", "Skaner", Icons.Default.AddAPhoto)
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
        Destination.Settings
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
                val mainViewModel: MainViewModel = hiltViewModel()
                MainScreen(
                    viewModel = mainViewModel,
                    contentPadding = innerPadding
                )
            }
            composable(Destination.AddReceipt.route) {
                AddReceiptScreen()
            }
            composable(Destination.Reports.route) {
                val reportsViewModel: ReportsViewModel = hiltViewModel()
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onSeeAllCategoriesClick = {
                        navController.navigate("all_categories")
                    }
                )
            }
            composable("all_categories") {
                val reportsViewModel: ReportsViewModel = hiltViewModel()
                AllCategoriesScreen(
                    viewModel = reportsViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}