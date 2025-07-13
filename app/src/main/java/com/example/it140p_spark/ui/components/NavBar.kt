package com.example.it140p_spark.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val label: String) {
    data object Schedule : NavItem("schedule", Icons.Filled.Today, Icons.Outlined.Today, "Schedule")
    data object Enrollment : NavItem("enrollment", Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark, "Enrollment")
    data object Billing : NavItem("billing", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "Billing")
    data object Records : NavItem("records", Icons.Filled.School, Icons.Outlined.School, "Records")
}

val navItems = listOf(
    NavItem.Schedule,
    NavItem.Enrollment,
    NavItem.Billing,
    NavItem.Records,
)

@Composable
fun NavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar {
        navItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) }
            )
        }
    }
}