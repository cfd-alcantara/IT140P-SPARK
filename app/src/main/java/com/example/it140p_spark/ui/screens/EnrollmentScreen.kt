package com.example.it140p_spark.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import com.example.it140p_spark.ui.components.TimeTable_Sectioning
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.material3.AlertDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

const val serverURL = "http://192.168.100.9/student_management_system/REST/"

@Serializable
data class Course(
    @SerialName("CourseID") val courseId: String,
    @SerialName("CourseName") val courseName: String,
    @SerialName("CourseCode") val courseCode: String,
    @SerialName("CourseUnits") val courseUnits: String
)

@Serializable
data class EnlistedCourse(
    @SerialName("CourseID") val courseId: String,
    @SerialName("CourseName") val courseName: String,
    @SerialName("CourseCode") val courseCode: String,
    @SerialName("SectionID") val sectionId: String,
    @SerialName("SectionCode") val sectionCode: String,
    @SerialName("CourseUnits") val courseUnits: String,
    @SerialName("Day") val day: String,
    @SerialName("StartTime") val startTime: String,
    @SerialName("EndTime") val endTime: String
)

data class GroupedSection(
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val sectionId: String,
    val sectionCode: String,
    val courseUnits: String,
    val schedules: List<ScheduleEntry>
)

data class ScheduleEntry(
    val day: String,
    val startTime: String,
    val endTime: String
)

@Serializable
data class CourseSearchResponse(
    val status: String,
    val message: String? = null,
    val data: List<Course>? = null
)

@Serializable
data class CourseSectionSearchResponse(
    val status: String,
    val message: String? = null,
    val data: List<EnlistedCourse>? = null
)

@Serializable
data class EnrollmentResponse(
    val status: String,
    val message: String
)

fun Context.toast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(studentId: String, padding: PaddingValues) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentStudentId = studentId
    val enlistmentIdState = remember { mutableStateOf<String?>(null) }
    val enlistmentId = enlistmentIdState.value

    //Enlistment
    val selectedCourses = remember { mutableStateListOf<Course>() }
    var coursesList: List<Course> by remember { mutableStateOf(emptyList()) }

    //Sectioning
    val selectedCourseSection = remember { mutableStateListOf<EnlistedCourse>() }
    var enlistedCourses: List<EnlistedCourse> by remember { mutableStateOf(emptyList()) }
    val selectedSectionIds = remember { mutableStateListOf<String>() }
    var showSuggestionDialog by remember { mutableStateOf(false) }
    var suggestedSchedule by remember { mutableStateOf<List<GroupedSection>>(emptyList()) }

    var searchCourseQuery by remember { mutableStateOf("") }

    val httpClient = remember {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Enlist", "Section", "Finalize")

    DisposableEffect(Unit) {
        onDispose {
            httpClient.close()
        }
    }

    LaunchedEffect(selectedTabIndex, searchCourseQuery, selectedCourses.size) {
        if (selectedTabIndex == 0) {
            fetchCourses(context, httpClient, searchCourseQuery) { updatedList ->
                coursesList = updatedList
            }
        }
        else if (selectedTabIndex == 1) {

            fetchEnlistmentId(context, httpClient, currentStudentId) { fetchedId ->
                enlistmentIdState.value = fetchedId
            }

            fetchCourseSections(context, httpClient, searchCourseQuery) { updatedList ->
                enlistedCourses = updatedList
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                0 -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Student Enrollment",
                            fontSize = 28.sp,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Enrolling for Student ID: $currentStudentId",
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = searchCourseQuery,
                            onValueChange = { newValue ->
                                searchCourseQuery = newValue
                            },
                            label = { Text("Search Courses") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Text(
                            text = "List of Available Courses",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(coursesList) { course ->
                                    if (!selectedCourses.contains(course)) {
                                        AvailableCourseItem(course = course) {
                                            selectedCourses.add(course)
                                        }
                                    }
                                }
                                if (coursesList.isEmpty() && searchCourseQuery.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "No courses found for '${searchCourseQuery}'",
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (coursesList.isEmpty() && searchCourseQuery.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Loading courses...",
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (selectedCourses.isNotEmpty()) {
                            Text(
                                text = "Selected Courses for Action",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(selectedCourses) { course ->
                                        ActionableCourseItem(course = course) {
                                            selectedCourses.remove(course)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        processSelectedCoursesForAction(
                                            context,
                                            httpClient,
                                            currentStudentId,
                                            selectedCourses,
                                            isAddAction = true
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "Add Selected",
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        processSelectedCoursesForAction(
                                            context,
                                            httpClient,
                                            currentStudentId,
                                            selectedCourses,
                                            isAddAction = false
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .padding(start = 4.dp)
                            ) {
                                Text(
                                    text = "Remove Selected",
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                1 -> {LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Available Sections for Enlisted Courses",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val suggestion = suggestValidSchedule(enlistedCourses)
                                    if (suggestion != null) {
                                        suggestedSchedule = suggestion
                                        showSuggestionDialog = true
                                    } else {
                                        context.toast("No valid schedule found.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Suggest Schedule")
                            }
                        }
                    }

                    val groupedSections = enlistedCourses
                        .groupBy { it.courseId + it.sectionCode }
                        .map { (_, entries) ->
                            val first = entries.first()
                            GroupedSection(
                                courseId = first.courseId,
                                courseName = first.courseName,
                                courseCode = first.courseCode,
                                sectionId = first.sectionId,
                                sectionCode = first.sectionCode,
                                courseUnits = first.courseUnits,
                                schedules = entries.map {
                                    ScheduleEntry(it.day, it.startTime, it.endTime)
                                }
                            )
                        }


                    items(groupedSections) { section ->
                        val uniqueId = section.courseId + section.sectionCode
                        val isSelected = selectedSectionIds.contains(uniqueId)

                        AvailableGroupedSection(
                            section = section,
                            isSelected = isSelected,
                            onToggleSelect = { toggled ->
                                val existingIndex = selectedCourseSection.indexOfFirst {
                                    it.courseId == toggled.courseId
                                }

                                if (isSelected) {
                                    selectedSectionIds.remove(uniqueId)
                                    selectedCourseSection.removeAt(existingIndex)
                                } else {
                                    if (existingIndex != -1) {
                                        val existing = selectedCourseSection[existingIndex]
                                        val existingUniqueId = existing.courseId + existing.sectionCode
                                        selectedCourseSection.removeAt(existingIndex)
                                        selectedSectionIds.remove(existingUniqueId)
                                    }
                                    selectedSectionIds.add(uniqueId)
                                    // Pick one representative to insert
                                    val firstSchedule = section.schedules.first()
                                    selectedCourseSection.add(
                                        EnlistedCourse(
                                            courseId = section.courseId,
                                            courseName = section.courseName,
                                            courseCode = section.courseCode,
                                            sectionId = section.sectionId,
                                            sectionCode = section.sectionCode,
                                            courseUnits = section.courseUnits,
                                            day = firstSchedule.day,
                                            startTime = firstSchedule.startTime,
                                            endTime = firstSchedule.endTime
                                        )
                                    )
                                }
                            },
                            onSectionConfirmed = {
                                coroutineScope.launch {
                                    if (enlistmentId != null) {
                                        ktorInsertSection(
                                            context, httpClient, enlistmentId,
                                            section.courseId, section.sectionId
                                        )
                                    } else {
                                        context.toast("Enlistment ID missing.")
                                    }
                                }
                            }
                        )
                    }


                    item {
                        Box(modifier = Modifier.height(600.dp)) {
                            TimeTable_Sectioning()
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (enlistmentId != null) {
                                        selectedCourseSection.forEach { course ->
                                            val courseId = course.courseId
                                            val sectionId = course.sectionId
                                            ktorInsertSection(context, httpClient, enlistmentId, courseId, sectionId)
                                        }
                                        context.toast("Sections submitted.")
                                    } else {
                                        context.toast("Enlistment ID not found.")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Section Selection")
                        }
                    }

                }
                    if (showSuggestionDialog) {
                        AlertDialog(
                            onDismissRequest = { showSuggestionDialog = false },
                            title = { Text("Suggested Schedule") },
                            text = {
                                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                                Column(
                                    modifier = Modifier
                                        .heightIn(min = 100.dp, max = 400.dp) // limit height of scrollable area
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    suggestedSchedule.forEach { section ->
                                        Text(
                                            text = "${section.courseCode} - ${section.courseName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Section: ${section.sectionCode}",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        section.schedules
                                            .groupBy { it.day }
                                            .forEach { (day, times) ->
                                                val mergedRanges = times.mapNotNull {
                                                    try {
                                                        val start = LocalTime.parse(it.startTime, timeFormatter)
                                                        val end = LocalTime.parse(it.endTime, timeFormatter)
                                                        start to end
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                }.sortedBy { it.first }
                                                    .fold(mutableListOf<Pair<LocalTime, LocalTime>>()) { acc, current ->
                                                        if (acc.isEmpty()) {
                                                            acc.add(current)
                                                        } else {
                                                            val last = acc.last()
                                                            if (!current.first.isAfter(last.second)) {
                                                                acc[acc.lastIndex] = last.first to maxOf(last.second, current.second)
                                                            } else {
                                                                acc.add(current)
                                                            }
                                                        }
                                                        acc
                                                    }

                                                val displayFormatter = DateTimeFormatter.ofPattern("h:mm a")
                                                val timeRanges = mergedRanges.joinToString(", ") {
                                                    "${it.first.format(displayFormatter)} - ${it.second.format(displayFormatter)}"
                                                }

                                                Text(
                                                    text = "$day: $timeRanges",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            },

                            confirmButton = {
                                Button(
                                    onClick = {
                                        selectedCourseSection.clear()
                                        selectedSectionIds.clear()
                                        suggestedSchedule.forEach { grouped ->
                                            val uniqueId = grouped.courseId + grouped.sectionCode
                                            grouped.schedules.forEach { schedule ->
                                                selectedCourseSection.add(
                                                    EnlistedCourse(
                                                        courseId = grouped.courseId,
                                                        courseName = grouped.courseName,
                                                        courseCode = grouped.courseCode,
                                                        sectionId = grouped.sectionId,
                                                        sectionCode = grouped.sectionCode,
                                                        courseUnits = grouped.courseUnits,
                                                        day = schedule.day,
                                                        startTime = schedule.startTime,
                                                        endTime = schedule.endTime
                                                    )
                                                )
                                            }
                                            selectedSectionIds.add(uniqueId)
                                        }
                                        context.toast("Suggested schedule applied.")
                                        showSuggestionDialog = false
                                    }
                                ) {
                                    Text("Apply Schedule")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showSuggestionDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Finalize Enrollment",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Summary of selected courses and finalization options will go here.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { /* Handle finalization */ }) {
                            Text("Submit Enrollment")
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchEnlistmentId(
    context: Context,
    httpClient: HttpClient,
    studentId: String,
    onFetched: (String?) -> Unit
) {
    try {
        val response = httpClient.get("${serverURL}get_enlistmentID.php?student_id=$studentId")
        val body = response.bodyAsText()
        println("Fetched enlistment ID response: $body")
        val json = Json.parseToJsonElement(body).jsonObject
        val status = json["status"]?.jsonPrimitive?.contentOrNull
        if (status == "success") {
            val enlistmentId = json["enlistment_id"]?.jsonPrimitive?.contentOrNull
            onFetched(enlistmentId)
        } else {
            context.toast(json["message"]?.jsonPrimitive?.content ?: "No enlistment found.")
            onFetched(null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Failed to fetch enlistment ID: ${e.localizedMessage}")
        onFetched(null)
    }
}


@Composable
fun AvailableCourseItem(course: Course, onSelect: (Course) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${course.courseCode} - ${course.courseName} (${course.courseUnits} units)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = { onSelect(course) },
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("Select")
        }
    }
}

@Composable
fun AvailableCourseSections(
    course: EnlistedCourse,
    isSelected: Boolean,
    onToggleSelect: (EnlistedCourse) -> Unit,
    onSectionConfirmed: (EnlistedCourse) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${course.courseCode} - ${course.courseName}\n" +
                    "Section: ${course.sectionCode}\n" +
                    "${course.day}: ${course.startTime} - ${course.endTime}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Button(
            onClick = {
                if (isSelected) {
                    // Immediately deselect without confirmation
                    onToggleSelect(course)
                } else {
                    // Ask confirmation before selecting
                    showDialog = true
                }
            },
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isSelected)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(if (isSelected) "Deselect" else "Select")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Selection") },
            text = {
                Text("Are you sure you want to select this section?\n\n${course.courseCode} (${course.sectionCode}) | ${course.startTime} - ${course.endTime}")
            },
            confirmButton = {
                Button(onClick = {
                    onToggleSelect(course)
                    onSectionConfirmed(course)
                    showDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun ActionableCourseItem(course: Course, onUnselect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${course.courseCode} - ${course.courseName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onUnselect,
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("Deselect")
        }
    }
}

private suspend fun processSelectedCoursesForAction(
    context: Context,
    httpClient: HttpClient,
    studentId: String,
    selectedCourses: MutableList<Course>,
    isAddAction: Boolean
) {
    if (selectedCourses.isEmpty()) {
        context.toast("Please select at least one course for action.")
        return
    }
    val coursesToProcess = selectedCourses.toList()
    var anyActionFailed = false
    val successfullyProcessedCourses = mutableListOf<Course>()
    for (course in coursesToProcess) {
        println("${if (isAddAction) "Enrolling" else "Removing"}: StudentID = ${studentId}, CourseID = ${course.courseId}")
        val success = if (isAddAction) {
            ktorAddEnrollment(context, httpClient, studentId, course.courseId)
        } else {
            ktorRemoveEnrollment(context, httpClient, studentId, course.courseId)
        }
        if (success) {
            successfullyProcessedCourses.add(course)
        } else {
            anyActionFailed = true
        }
    }
    selectedCourses.removeAll(successfullyProcessedCourses)
    val actionType = if (isAddAction) "enrolled" else "removed"
    val verb = if (isAddAction) "enrolled" else "removed"
    if (!anyActionFailed && successfullyProcessedCourses.isNotEmpty()) {
        context.toast("All selected courses $actionType successfully!")
    } else if (anyActionFailed && successfullyProcessedCourses.isNotEmpty()) {
        context.toast("Some courses ${verb}, but others failed.")
    } else {
        context.toast("No courses were processed for action.")
    }
}

private suspend fun fetchCourses(context: Context, httpClient: HttpClient, query: String, onCoursesFetched: (List<Course>) -> Unit) {
    try {
        val fullUrl = "${serverURL}search_courseinfo.php?query=${query}"
        println("Fetching courses from: $fullUrl")
        val response: HttpResponse = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Raw JSON response for courses: $responseBodyString")
        if (response.status.value == 200) {
            val parsedResponse = Json.decodeFromString<CourseSearchResponse>(responseBodyString)
            if (parsedResponse.status == "success" && parsedResponse.data != null) {
                onCoursesFetched(parsedResponse.data)
            } else {
                context.toast("Server reported error: ${parsedResponse.message ?: "Unknown error"}")
                onCoursesFetched(emptyList())
            }
        } else {
            context.toast("HTTP Error fetching courses: ${response.status.value} - ${response.status.description}")
            onCoursesFetched(emptyList())
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Error fetching courses: ${e.localizedMessage}")
        onCoursesFetched(emptyList())
    }
}

private suspend fun fetchCourseSections(context: Context, httpClient: HttpClient, query: String, onCoursesFetched: (List<EnlistedCourse>) -> Unit) {
    try {
        val fullUrl = "${serverURL}get_courseEnlisted.php?query=${query}"
        println("Fetching courses from: $fullUrl")
        val response: HttpResponse = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Raw JSON response for courses: $responseBodyString")
        if (response.status.value == 200) {
            val parsedResponse = Json.decodeFromString<CourseSectionSearchResponse>(responseBodyString)
            if (parsedResponse.status == "success" && parsedResponse.data != null) {
                onCoursesFetched(parsedResponse.data)
            } else {
                context.toast("Server reported error: ${parsedResponse.message ?: "Unknown error"}")
                onCoursesFetched(emptyList())
            }
        } else {
            context.toast("HTTP Error fetching courses: ${response.status.value} - ${response.status.description}")
            onCoursesFetched(emptyList())
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Error fetching courses: ${e.localizedMessage}")
        onCoursesFetched(emptyList())
    }
}

private suspend fun ktorAddEnrollment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
    try {
        if (studentId.isBlank() || courseId.isBlank()) {
            context.toast("Missing student or course ID.")
            return false
        }
        val fullUrl = "${serverURL}add_enlistment.php?student_id=${studentId}&course_id=${courseId}"
        println("Requesting: $fullUrl")
        val response = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Response from server: ${response.status} - $responseBodyString")
        val enrollmentResponse = Json.decodeFromString<EnrollmentResponse>(responseBodyString)
        if (enrollmentResponse.status == "success") {
            return true
        } else {
            context.toast("Failed to enroll $courseId: ${enrollmentResponse.message}")
            return false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Enrollment error for $courseId: ${e.localizedMessage ?: "Unknown"}")
        return false
    }
}

private suspend fun ktorRemoveEnrollment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
    try {
        if (studentId.isBlank() || courseId.isBlank()) {
            context.toast("Missing student or course ID.")
            return false
        }
        val fullUrl = "${serverURL}remove_enlistment.php?student_id=${studentId}&course_id=${courseId}"
        println("Requesting removal: $fullUrl")
        val response = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Response from server for removal: ${response.status} - $responseBodyString")
        val enrollmentResponse = Json.decodeFromString<EnrollmentResponse>(responseBodyString)
        if (enrollmentResponse.status == "success") {
            return true
        } else {
            context.toast("Failed to remove $courseId: ${enrollmentResponse.message}")
            return false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Removal error for $courseId: ${e.localizedMessage ?: "Unknown"}")
        return false
    }
}

suspend fun ktorInsertSection(
    context: Context,
    httpClient: HttpClient,
    enlistmentId: String,
    courseId: String,
    sectionId: String
): Boolean {
    return try {
        val fullUrl = "${serverURL}insert_section.php?EnlistmentID=$enlistmentId&CourseID=$courseId&SectionID=$sectionId"
        println("Inserting section via: $fullUrl")
        val response = httpClient.get(fullUrl)
        val body = response.bodyAsText()
        println("Insert section response: ${response.status} - $body")
        val result = Json.decodeFromString<EnrollmentResponse>(body)
        if (result.status == "success") {
            true
        } else {
            context.toast("Failed to insert section: ${result.message}")
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Error inserting section: ${e.localizedMessage}")
        false
    }
}

private fun suggestValidSchedule(sections: List<EnlistedCourse>): List<GroupedSection>? {
    val grouped = sections.groupBy { it.courseId + it.sectionCode }

    val groupedSections = grouped.map { (_, list) ->
        val first = list.first()
        GroupedSection(
            courseId = first.courseId,
            courseName = first.courseName,
            courseCode = first.courseCode,
            sectionId = first.sectionId,
            sectionCode = first.sectionCode,
            courseUnits = first.courseUnits,
            schedules = list.map { ScheduleEntry(it.day, it.startTime, it.endTime) }
        )
    }.groupBy { it.courseId }  // group by courseId to pick one section per course

    fun hasConflict(existing: List<ScheduleEntry>, new: List<ScheduleEntry>): Boolean {
        for (e in existing) {
            for (n in new) {
                if (e.day == n.day &&
                    !(e.endTime <= n.startTime || n.endTime <= e.startTime)) {
                    return true
                }
            }
        }
        return false
    }

    fun backtrack(
        groupedList: List<List<GroupedSection>>,
        index: Int,
        currentSchedule: MutableList<GroupedSection>,
        accumulatedSchedules: MutableList<ScheduleEntry>
    ): List<GroupedSection>? {
        if (index == groupedList.size) return currentSchedule.toList()

        for (section in groupedList[index]) {
            if (!hasConflict(accumulatedSchedules, section.schedules)) {
                currentSchedule.add(section)
                accumulatedSchedules.addAll(section.schedules)
                val result = backtrack(groupedList, index + 1, currentSchedule, accumulatedSchedules)
                if (result != null) return result
                currentSchedule.removeAt(currentSchedule.size - 1)
                accumulatedSchedules.removeAll(section.schedules)
            }
        }

        return null
    }

    return backtrack(groupedSections.values.toList(), 0, mutableListOf(), mutableListOf())
}


@Composable
fun AvailableGroupedSection(
    section: GroupedSection,
    isSelected: Boolean,
    onToggleSelect: (GroupedSection) -> Unit,
    onSectionConfirmed: (GroupedSection) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = "${section.courseCode} - ${section.courseName}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Section: ${section.sectionCode}",
            style = MaterialTheme.typography.bodySmall
        )
        section.schedules
            .groupBy { it.day }
            .forEach { (day, times) ->
                val mergedRanges = times.mapNotNull {
                    try {
                        val start = LocalTime.parse(it.startTime, timeFormatter)
                        val end = LocalTime.parse(it.endTime, timeFormatter)
                        start to end
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.sortedBy { it.first }
                    .fold(mutableListOf<Pair<LocalTime, LocalTime>>()) { acc, current ->
                        if (acc.isEmpty()) {
                            acc.add(current)
                        } else {
                            val last = acc.last()
                            if (!current.first.isAfter(last.second)) {
                                acc[acc.lastIndex] = last.first to maxOf(last.second, current.second)
                            } else {
                                acc.add(current)
                            }
                        }
                        acc
                    }

                val displayFormatter = DateTimeFormatter.ofPattern("h:mm a")

                val timeRanges = mergedRanges.joinToString(", ") {
                    "${it.first.format(displayFormatter)} - ${it.second.format(displayFormatter)}"
                }

                Text(
                    text = "$day: $timeRanges",
                    style = MaterialTheme.typography.labelSmall
                )

            }


        Button(
            onClick = {
                if (isSelected) {
                    onToggleSelect(section)
                } else {
                    showDialog = true
                }
            },
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isSelected) "Deselect" else "Select")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Selection") },
            text = {
                Text("Are you sure you want to select this section?\n\n${section.courseCode} (${section.sectionCode})")
            },
            confirmButton = {
                Button(onClick = {
                    onToggleSelect(section)
                    onSectionConfirmed(section)
                    showDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

