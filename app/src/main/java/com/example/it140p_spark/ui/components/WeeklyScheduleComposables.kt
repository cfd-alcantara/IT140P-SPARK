package com.example.it140p_spark.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier

enum class Weekday(val label: String) {
    Monday("Mon"),
    Tuesday("Tue"),
    Wednesday("Wed"),
    Thursday("Thu"),
    Friday("Fri"),
    Saturday("Sat")
}

data class SimpleEvent(
    val name: String,
    val day: Weekday,
    val startHour: Int,
    val endHour: Int,
    val color: Color
)

val sampleEvents = listOf(
    SimpleEvent("Math", Weekday.Monday, 8, 10, Color(0xFF81C784)),
    SimpleEvent("Physics", Weekday.Wednesday, 11, 13, Color(0xFF64B5F6)),
    SimpleEvent("CS", Weekday.Friday, 14, 16, Color(0xFFFFB74D))
)

@Composable
fun Timetable(events: List<SimpleEvent>) {
    val hours = 7..18
    val days = Weekday.values()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(50.dp))
            days.forEach {
                Text(
                    text = it.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Time rows
        hours.forEach { hour ->
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                Text(
                    text = "$hour:00",
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall
                )
                days.forEach { day ->
                    val matched = events.find {
                        it.day == day && hour >= it.startHour && hour < it.endHour
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(1.dp)
                            .background(matched?.color ?: Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (matched != null && hour == matched.startHour) {
                            Text(matched.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
