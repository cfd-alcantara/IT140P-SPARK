package com.example.it140p_spark.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// ------------------------------
// Course Data Class & Sample Data
// ------------------------------

data class CourseSection(
    val dayIndex: Int,      // 0 = Mon, 1 = Tue, ..., 5 = Sat
    val startRow: Int,      // 0 = 7:00 AM, 1 = 8:15 AM, ...
    val rowSpan: Int,       // how many 75-min slots it spans
    val courseName: String // display name
)

private val sampleCourses = listOf(
    CourseSection(dayIndex = 0, startRow = 1, rowSpan = 2, courseName = "IT140P"), // Monday 8:15-11:00
    CourseSection(dayIndex = 0, startRow = 2, rowSpan = 3, courseName = "IT140P"), // Monday 8:15-11:00
    CourseSection(dayIndex = 4, startRow = 2, rowSpan = 3, courseName = "MATH110"), // Friday 7:00-11:00
    CourseSection(dayIndex = 2, startRow = 4, rowSpan = 1, courseName = "CS101"),   // Wednesday 1:45-3:00
    CourseSection(dayIndex = 4, startRow = 0, rowSpan = 3, courseName = "MATH110") // Friday 7:00-11:00
)

private val days = listOf("M", "T", "W", "Th", "F", "S")
private const val startHour = 7
private const val startMinute = 0
private const val intervalMinutes = 75
private const val numRows = 12 // from 7:00am

private fun getCurrentDayIndex(): Int {
    return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> -1
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

// -----------------------------
// Main Timetable Composable
// -----------------------------

@Composable
fun TimeTable_Sectioning(
    modifier: Modifier = Modifier,
    cellHeight: Dp = 64.dp,
    cellWidth: Dp = 56.dp,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()) // in case height overflows
    ) {
        val todayIndex = getCurrentDayIndex()

        // Header: Days of Week
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(cellWidth))
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
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text(day, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Timetable Grid
        for (row in 0 until numRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val (time, ampm) = getTimeLabelParts(row)

                // Time Label Column
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(cellHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-10).dp)) {
                        Text(time, fontSize = 12.sp, lineHeight = 14.sp)
                        Text(ampm, fontSize = 10.sp, lineHeight = 10.sp)
                    }
                }

                // Day Columns
                for (day in days.indices) {
                    // Check if a course starts here
                    val course = sampleCourses.find {
                        it.dayIndex == day && it.startRow == row
                    }

                    if (course != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight * course.rowSpan)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = course.courseName,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        // Empty cell with border
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
                                    if (isFirstRow) drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), borderWidth)
                                    if (!isFirstColumn) drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), borderWidth)
                                    if (!isLastColumn) drawLine(borderColor, Offset(size.width, 0f), Offset(size.width, size.height), borderWidth)
                                    if (!isLastRow) drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), borderWidth)
                                }
                        )
                    }
                }
            }
        }
    }
}
