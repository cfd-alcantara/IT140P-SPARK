package com.example.it140p_spark.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val days = listOf("M", "T", "W", "Th", "F", "S")
private const val startHour = 7
private const val startMinute = 0
private const val intervalMinutes = 75
private const val numRows = 12 // 7:00am to 8:45pm

private fun getCurrentDayIndex(): Int {
    // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ... Calendar.SATURDAY = 7
    return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> -1 // Sunday or error
    }
}

@SuppressLint("DefaultLocale")
private fun getTimeLabelParts(row: Int): Pair<String, String> {
    val totalMinutes = startHour * 60 + startMinute + row * intervalMinutes
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    val ampm = if (hour < 12) "am" else "pm"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d", displayHour, minute) to ampm
}

@Composable
fun TimeTable(
    modifier: Modifier = Modifier,
    cellHeight: Dp = 64.dp,
    cellWidth: Dp = 56.dp,
) {
    // Layout for Header Rows (Days) and is fixed to the top
    Column(modifier = modifier) {
        // Header Row (Days)
        val todayIndex = getCurrentDayIndex()
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(cellWidth)) // Empty for time labels
            // Display the days of the week with the current day highlighted
            days.forEachIndexed { index, day ->
                val isToday = index == todayIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(cellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            day,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Layout for Header Columns (Time) and Course Cells
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.verticalScroll(scrollState).padding(top = 16.dp, bottom = 24.dp)) {
            // Header Columns (Time)
            for (row in 0 until numRows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    val (time, ampm) = getTimeLabelParts(row)
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = (-10).dp) // Offset the time label upwards
                        ) {
                            Text(time, fontSize = 12.sp, lineHeight = 14.sp)
                            Text(ampm, fontSize = 10.sp, lineHeight = 10.sp)
                        }
                    }
                    // Course Cells
                    for (day in days.indices) {
                        // Values used for drawing borders
                        val isFirstColumn = day == 0
                        val isLastColumn = day == days.size - 1
                        val isFirstRow = row == 0
                        val isLastRow = row == numRows - 1
                        val borderColor = MaterialTheme.colorScheme.onBackground
                        val borderWidth = 0.1f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .drawBehind {
                                    // Draw top border of the first row
                                    if (isFirstRow) {
                                        drawLine(
                                            color = borderColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = borderWidth
                                        )
                                    }
                                    // Draw left border if not first column
                                    if (!isFirstColumn) {
                                        drawLine(
                                            color = borderColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = borderWidth
                                        )
                                    }
                                    // Draw right border if not last column
                                    if (!isLastColumn) {
                                        drawLine(
                                            color = borderColor,
                                            start = Offset(size.width, 0f),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = borderWidth
                                        )
                                    }
                                    // Draw bottom border if not last row
                                    if (!isLastRow) {
                                        drawLine(
                                            color = borderColor,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = borderWidth
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            /* todo: this is where courses are displayed */
                        }
                    }
                }
            }
        }
    }
}