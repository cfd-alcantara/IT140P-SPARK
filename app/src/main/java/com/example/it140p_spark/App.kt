package com.example.it140p_spark

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.it140p_spark.ui.components.BillingTopBar
import com.example.it140p_spark.ui.components.EnrollTopBar
import com.example.it140p_spark.ui.components.NavBar
import com.example.it140p_spark.ui.components.RecordsTopBar
import com.example.it140p_spark.ui.components.ScheduleTopBar
import com.example.it140p_spark.ui.components.navItems
import com.example.it140p_spark.ui.layout.ScaffoldLayout
import com.example.it140p_spark.ui.screens.BillingScreen
import com.example.it140p_spark.ui.screens.EnrollScreen
import com.example.it140p_spark.ui.screens.RecordsScreen
import com.example.it140p_spark.ui.screens.ScheduleScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedNavItem = navItems[selectedIndex]
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ScaffoldLayout(
        topBar = {
            when (selectedNavItem.route) {
                "schedule" -> ScheduleTopBar(scrollBehavior = scrollBehavior)
                "enroll" -> EnrollTopBar(scrollBehavior = scrollBehavior)
                "billing" -> BillingTopBar(scrollBehavior = scrollBehavior)
                "records" -> RecordsTopBar(scrollBehavior = scrollBehavior)
            }
        },
        bottomBar = {
            NavBar(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        },
    ) { padding ->
        when (selectedNavItem.route) {
            "schedule" -> ScheduleScreen(padding)
            "enroll" -> EnrollScreen(padding)
            "billing" -> BillingScreen(padding)
            "records" -> RecordsScreen(padding)
        }
    }
}