package com.example.it140p_spark.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.it140p_spark.ui.components.CourseCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollScreen(padding: PaddingValues) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Enlist", "Section", "Finalize")

    Box(modifier = Modifier.padding(padding)) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                val selected = selectedTabIndex == index
                Tab(
                    selected = selected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onBackground
                        )
                    }
                )
            }
        }
        when (selectedTabIndex) {
            0 -> LazyColumn(
                modifier = Modifier.padding(top = 64.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CourseCard(
                        courseCode = "IT200-1D",
                        description = "IT Capstone Project 1",
                        units = "3 Units",
                        yearAndTerm = "Y4T1",
                        available = true,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            1 -> Text(text = "Section Content", modifier = Modifier.padding(top = 48.dp))
            2 -> Text(text = "Finalize Content", modifier = Modifier.padding(top = 48.dp))
        }
    }
}