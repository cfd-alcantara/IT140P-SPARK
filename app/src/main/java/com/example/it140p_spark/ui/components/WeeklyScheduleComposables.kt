package com.example.it140p_spark.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import com.example.it140p_spark.ui.screens.serverURL
import com.example.it140p_spark.ui.screens.toast
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val startHour: Float,
    val endHour: Float,
    val color: Color
)

@Serializable
data class Schedule(
    @SerialName("CourseID") val courseId: String,
    @SerialName("CourseCode") val courseCode: String,
    @SerialName("Day") val day: String?,
    @SerialName("StartTime") val startTime: String?,
    @SerialName("EndTime") val endTime: String?
)

@Serializable
data class ScheduleSearchResponse(
    val status: String,
    val message: String? = null,
    val data: List<Schedule>? = null
)

fun parseTimeToFloat(time: String): Float {
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    val localTime = LocalTime.parse(time, formatter)
    return localTime.hour + localTime.minute / 60f
}

fun mapDayStringToEnum(day: String): Weekday? {
    return when (day.lowercase()) {
        "monday" -> Weekday.Monday
        "tuesday" -> Weekday.Tuesday
        "wednesday" -> Weekday.Wednesday
        "thursday" -> Weekday.Thursday
        "friday" -> Weekday.Friday
        "saturday" -> Weekday.Saturday
        else -> null
    }
}

@Composable
fun StudentScheduleTimetable(
    context: Context,
    studentID: String,
    httpClient: HttpClient
) {
    var events by remember { mutableStateOf<List<SimpleEvent>>(emptyList()) }

    LaunchedEffect(studentID) {
        val schedule = fetchStudentSchedule(context, httpClient, studentID)
        events = schedule.mapNotNull { entry ->
            val dayEnum = entry.day?.let { mapDayStringToEnum(it) }
            val start = entry.startTime?.let { parseTimeToFloat(it) }
            val end = entry.endTime?.let { parseTimeToFloat(it) }

            if (dayEnum != null && start != null && end != null) {
                SimpleEvent(
                    name = entry.courseCode,
                    day = dayEnum,
                    startHour = start,
                    endHour = end,
                    color = Color(0xFF90CAF9) // Light blue
                )
            } else null
        }
    }

    Timetable(events)
}

@Composable
fun Timetable(events: List<SimpleEvent>) {
    val days = Weekday.values()

    val startTime = 7.0f
    val endTime = 20.75f // 8:45 PM
    val interval = 1.25f
    val timeSlots = buildList {
        var current = startTime
        while (current < endTime) {
            add(current)
            current += interval
        }
        add(endTime) // include 8:45 PM explicitly
    }

    val borderColor = Color.Gray.copy(alpha = 0.3f)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header row
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(60.dp))
            days.forEach {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = it.label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Time rows
        timeSlots.forEach { time ->
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .border(1.dp, borderColor),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = formatTimeSlot(time),
                        modifier = Modifier.padding(end = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                days.forEach { day ->
                    val matched = events.find {
                        it.day == day && time >= it.startHour && time < it.endHour
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, borderColor)
                            .background(matched?.color ?: Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (matched != null && time == matched.startHour) {
                            Text(
                                matched.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTimeSlot(time: Float): String {
    val hour = time.toInt()
    val minute = ((time - hour) * 60).toInt()
    val localTime = LocalTime.of(hour, minute)
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    return localTime.format(formatter)
}

suspend fun fetchStudentSchedule(context: Context, httpClient: HttpClient, studentID: String): List<Schedule> {
    return try {
        val fullUrl = "${serverURL}get_studentSchedule.php?StudentID=${studentID}"
        val response: HttpResponse = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        if (response.status.value == 200) {
            val parsedResponse = Json.decodeFromString<ScheduleSearchResponse>(responseBodyString)
            if (parsedResponse.status == "success" && parsedResponse.data != null) {
                parsedResponse.data
            } else {
                context.toast("Server error: ${parsedResponse.message ?: "Unknown"}")
                emptyList()
            }
        } else {
            context.toast("HTTP ${response.status.value}: ${response.status.description}")
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Fetch failed: ${e.localizedMessage}")
        emptyList()
    }
}
