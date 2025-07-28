package com.example.it140p_spark.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import com.example.it140p_spark.presentation.toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import com.example.it140p_spark.data.SERVER_URL
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas

enum class Weekday(val display: String) {
    Monday("Monday"),
    Tuesday("Tuesday"),
    Wednesday("Wednesday"),
    Thursday("Thursday"),
    Friday("Friday"),
    Saturday("Saturday"),
    Sunday("Sunday");
}

data class SimpleScheduleEvent(
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
        "sunday" -> Weekday.Sunday
        else -> null
    }
}

// --- Conversion function ---
fun scheduleToSimpleScheduleEvent(schedule: Schedule, color: Color): SimpleScheduleEvent? {
    val dayEnum = schedule.day?.let { mapDayStringToEnum(it) } ?: return null
    val start = schedule.startTime?.let {
        try { parseTimeToFloat(it) } catch (e: Exception) { return null }
    } ?: return null
    val end = schedule.endTime?.let {
        try { parseTimeToFloat(it) } catch (e: Exception) { return null }
    } ?: return null
    return SimpleScheduleEvent(
        name = schedule.courseCode,
        color = color,
        day = dayEnum,
        startHour = start,
        endHour = end
    )
}

@Composable
fun StudentScheduleTimetable(
    context: Context,
    studentID: String,
    httpClient: HttpClient
) {
    var events by remember { mutableStateOf<List<SimpleScheduleEvent>>(emptyList()) }
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    var showEvents by remember { mutableStateOf(false) }

    // Add a 1 second delay before showing events to allow theme/colors to settle
    LaunchedEffect(studentID, containerColor) {
        showEvents = false
        val schedule = fetchStudentSchedule(context, httpClient, studentID)
        delay(300)
        events = schedule.mapNotNull { scheduleToSimpleScheduleEvent(it, containerColor) }
        showEvents = true
    }

    if (showEvents) {
        WeeklySchedule(events = mergeAdjacentEvents(events))
    }
}

// --- SimpleScheduleEventBox (grid style) ---
@Composable
fun SimpleScheduleEventBox(
    event: SimpleScheduleEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(end = 2.dp, bottom = 2.dp)
            .clipToBounds()
            .background(
                event.color,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        Text(
            text = "${formatTimeSlot(event.startHour)} - ${formatTimeSlot(event.endHour)}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// --- WeeklyDaysHeader (grid style) ---
@Composable
fun WeeklyDaysHeader(
    modifier: Modifier = Modifier,
    dayWidth: Dp = 128.dp
) {
    val today = remember { java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } }
    Row(modifier = modifier) {
        Weekday.entries.forEach { day ->
            val isToday = day.display.equals(today, ignoreCase = true)
            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.display,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .then(
                            if (isToday) Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                            else Modifier
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// --- SimpleTimesSidebar (grid style) ---
@Composable
fun SimpleTimesSidebar(
    modifier: Modifier = Modifier,
    hourHeight: Dp = 72.dp,
    minTime: LocalTime = LocalTime.of(7, 0),
    maxTime: LocalTime = LocalTime.of(22, 0),
) {
    // Build time slots with 1 hour 15 min interval
    val intervalMinutes = 75
    val timeSlots = mutableListOf<LocalTime>()
    var current = minTime
    while (!current.isAfter(maxTime)) {
        timeSlots.add(current)
        current = current.plusMinutes(intervalMinutes.toLong())
    }
    Column(modifier = modifier) {
        timeSlots.forEach { labelTime ->
            Box(
                modifier = Modifier.height(hourHeight).padding(end = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = labelTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(2.dp)
                )
            }
        }
    }
}

// --- WeeklySchedule (grid layout) ---
@Composable
fun WeeklySchedule(
    events: List<SimpleScheduleEvent>,
    modifier: Modifier = Modifier,
    dayWidth: Dp = 128.dp,
    hourHeight: Dp = 72.dp,
    minTime: LocalTime = LocalTime.of(7, 0),
    maxTime: LocalTime = LocalTime.of(22, 0),
    eventBox: @Composable (SimpleScheduleEvent) -> Unit = { SimpleScheduleEventBox(it) }
) {
    // Build time slots for grid lines
    val intervalMinutes = 75
    val timeSlots = mutableListOf<LocalTime>()
    var current = minTime
    while (!current.isAfter(maxTime)) {
        timeSlots.add(current)
        current = current.plusMinutes(intervalMinutes.toLong())
    }
    val hoursCount = timeSlots.size - 1
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var sidebarWidth by remember { mutableIntStateOf(0) }
    var headerHeight by remember {mutableIntStateOf(0) }

    val colorPrimary = MaterialTheme.colorScheme.primary
    // --- Current time state ---
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(60_000)
        }
    }

    Box (modifier = modifier) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .padding(start = with(LocalDensity.current) { sidebarWidth.toDp() })
                    .onGloballyPositioned { headerHeight = it.size.height }
                    .horizontalScroll(horizontalScrollState)
            ) {
                WeeklyDaysHeader(dayWidth = dayWidth)
            }
            // Content
            Row(
                modifier = Modifier.weight(1f)
            ) {
                // Times Sidebar
                SimpleTimesSidebar(
                    hourHeight = hourHeight,
                    minTime = minTime,
                    maxTime = maxTime,
                    modifier = Modifier
                        .onGloballyPositioned { sidebarWidth = it.size.width }
                        .verticalScroll(verticalScrollState)
                )
                // Schedule Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Draw events (below)
                    Layout(
                        content = {
                            events.forEach { event ->
                                Box {
                                    eventBox(event)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { measurables, constraints ->
                        val dayCount = Weekday.entries.size
                        val gridWidth = (dayWidth.toPx() * dayCount).roundToInt()
                        val gridHeight = (hourHeight.toPx() * hoursCount).roundToInt()
                        val placeables = measurables.mapIndexed { idx, measurable ->
                            val event = events[idx]
                            val eventDayIdx = event.day.ordinal
                            // Calculate top and height based on 75 min intervals
                            val startTotalMinutes = (event.startHour * 60).toInt()
                            val endTotalMinutes = (event.endHour * 60).toInt()
                            val minTotalMinutes = minTime.hour * 60 + minTime.minute
                            val top = ((startTotalMinutes - minTotalMinutes) / intervalMinutes.toFloat()) * hourHeight.toPx()
                            val height = ((endTotalMinutes - startTotalMinutes) / intervalMinutes.toFloat()) * hourHeight.toPx()
                            val left = eventDayIdx * dayWidth.toPx()
                            val width = dayWidth.toPx()

                            val placeable = measurable.measure(constraints.copy(
                                minWidth = width.roundToInt(), maxWidth = width.roundToInt(),
                                minHeight = height.roundToInt(), maxHeight = height.roundToInt()
                            ))
                            Triple(placeable, left.roundToInt(), top.roundToInt())
                        }
                        layout(gridWidth, gridHeight) {
                            placeables.forEach { (placeable, left, top) ->
                                placeable.place(left, top)
                            }
                        }
                    }
                    // Draw time line and circle (above events)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        // Horizontal lines for each interval
                        repeat(hoursCount + 1) { i ->
                            val y = i * hourHeight.toPx()
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        // Vertical day dividers
                        repeat(Weekday.entries.size + 1) { i ->
                            val x = i * dayWidth.toPx()
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        // Draw current time line if within range
                        val nowMinutes = now.hour * 60 + now.minute
                        val minMinutes = minTime.hour * 60 + minTime.minute
                        val maxMinutes = maxTime.hour * 60 + maxTime.minute
                        if (nowMinutes in minMinutes..maxMinutes) {
                            val y = ((nowMinutes - minMinutes) / intervalMinutes.toFloat()) * hourHeight.toPx()
                            // Draw the current time line
                            drawLine(
                                color = colorPrimary,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            // Draw the current time circle for the current day
                            val todayIdx = java.time.LocalDate.now().dayOfWeek.value - 1 // 0=Monday
                            if (todayIdx in 0 until Weekday.entries.size) {
                                val x = todayIdx * dayWidth.toPx() + 5.dp.toPx()
                                drawCircle(
                                    color = colorPrimary,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
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
        val fullUrl = "${SERVER_URL}get_studentSchedule.php?StudentID=${studentID}"
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

// --- Merge adjacent events function ---
fun mergeAdjacentEvents(events: List<SimpleScheduleEvent>): List<SimpleScheduleEvent> {
    if (events.isEmpty()) return emptyList()
    val sorted = events.sortedWith(compareBy({ it.day.ordinal }, { it.name }, { it.startHour }))
    val merged = mutableListOf<SimpleScheduleEvent>()
    var current = sorted.first()
    for (i in 1 until sorted.size) {
        val next = sorted[i]
        if (
            current.name == next.name &&
            current.day == next.day &&
            current.endHour == next.startHour &&
            current.color == next.color
        ) {
            // Merge with current
            current = current.copy(endHour = next.endHour)
        } else {
            merged.add(current)
            current = next
        }
    }
    merged.add(current)
    return merged
}
