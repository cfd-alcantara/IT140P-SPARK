package com.example.it140p_spark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.it140p_spark.ui.components.NavBar
import com.example.it140p_spark.ui.components.navItems
import com.example.it140p_spark.ui.layout.ScaffoldLayout
import com.example.it140p_spark.ui.screens.BillingScreen
import com.example.it140p_spark.ui.screens.EnrollmentScreen
import com.example.it140p_spark.ui.screens.RecordsScreen
import com.example.it140p_spark.ui.screens.ScheduleScreen

@Composable
fun App() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedNavItem = navItems[selectedIndex]

    ScaffoldLayout(
        bottomBar = {
            NavBar(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
    ) { padding ->
        when (selectedNavItem.route) {
            "schedule" -> ScheduleScreen(padding)
            "enrollment" -> EnrollmentScreen(padding)
            "billing" -> BillingScreen(padding)
            "records" -> RecordsScreen(padding)
        }
    }
}