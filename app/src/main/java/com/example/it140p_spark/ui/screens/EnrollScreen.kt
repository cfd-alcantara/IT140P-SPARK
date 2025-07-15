package com.example.it140p_spark.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EnrollScreen(padding: PaddingValues) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Enlist", "Section", "Finalize")

    Box(modifier = Modifier.padding(padding)) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTabIndex) {
            0 -> Text(text = "Enlist Content", modifier = Modifier.padding(top = 48.dp))
            1 -> Text(text = "Section Content", modifier = Modifier.padding(top = 48.dp))
            2 -> Text(text = "Finalize Content", modifier = Modifier.padding(top = 48.dp))
        }
    }
}