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

const val serverURL = "http://192.168.56.1/student_management_system/REST/"

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
    @SerialName("SectionID") val sectionId: String?, // Made nullable
    @SerialName("SectionCode") val sectionCode: String?, // Made nullable
    @SerialName("CourseUnits") val courseUnits: String,
    @SerialName("Day") val day: String?,
    @SerialName("StartTime") val startTime: String?,
    @SerialName("EndTime") val endTime: String?
)

data class GroupedSection(
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val sectionId: String, // Non-nullable, so handle nulls from EnlistedCourse
    val sectionCode: String, // Non-nullable, so handle nulls from EnlistedCourse
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

    // Enlistment Tab States
    // This list holds the courses that are currently on the student's record (fetched from backend)
    val initialEnrolledCourses = remember { mutableStateListOf<Course>() }
    // This list holds the courses the user has selected/deselected in the UI.
    // It represents the desired final state after "Confirm Changes".
    val stagedCoursesForAction = remember { mutableStateListOf<Course>() } // Renamed for clarity
    // This list holds all available courses, filtered by search query, and excluding those in stagedCoursesForAction.
    var coursesList: List<Course> by remember { mutableStateOf(emptyList()) }

    // State to force a refresh of the LaunchedEffect when changes are confirmed
    var refreshTrigger by remember { mutableIntStateOf(0) } // New state variable

    // Sectioning Tab States
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

    // LaunchedEffect to manage data fetching based on tab, search query, and refreshTrigger
    LaunchedEffect(selectedTabIndex, searchCourseQuery, refreshTrigger) { // Add refreshTrigger here
        println("LaunchedEffect triggered. selectedTabIndex: $selectedTabIndex, searchCourseQuery: $searchCourseQuery, refreshTrigger: $refreshTrigger")
        fetchEnlistmentId(context, httpClient, currentStudentId) { fetchedId ->
            enlistmentIdState.value = fetchedId
            println("Fetched Enlistment ID: $fetchedId")
        }

        if (selectedTabIndex == 0) {
            // Fetch all courses from the server
            val allFetchedCourses = fetchCoursesSuspend(context, httpClient, searchCourseQuery)
            println("Fetched all available courses: ${allFetchedCourses.size} courses.")

            // Fetch courses currently enlisted by the student
            val enlistedCoursesFromBackend = fetchCourseSectionsSuspend(context, httpClient, currentStudentId)
            println("Fetched enlisted courses from backend: ${enlistedCoursesFromBackend.size} courses.")

            // Update initialEnrolledCourses (source of truth from backend)
            initialEnrolledCourses.clear()
            initialEnrolledCourses.addAll(enlistedCoursesFromBackend.map {
                Course(it.courseId, it.courseName, it.courseCode, it.courseUnits)
            })
            println("initialEnrolledCourses updated: ${initialEnrolledCourses.map { it.courseCode }}")

            // Update stagedCoursesForAction (UI's working set)
            stagedCoursesForAction.clear()
            stagedCoursesForAction.addAll(initialEnrolledCourses) // Initialize with current enrolled
            println("stagedCoursesForAction initialized/updated: ${stagedCoursesForAction.map { it.courseCode }}")

            // Update coursesList (available courses)
            coursesList = allFetchedCourses.filter { !stagedCoursesForAction.contains(it) }.sortedBy { it.courseCode }
            println("coursesList (available) after filtering: ${coursesList.map { it.courseCode }}")

        } else if (selectedTabIndex == 1) {
            // Only fetch enlisted courses for the Section tab
            enlistedCourses = fetchCourseSectionsSuspend(context, httpClient, currentStudentId)
            println("Enlisted courses for Section tab: ${enlistedCourses.size} courses.")
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
                0 -> { // Enlist Tab
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
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
                                // Display courses from 'coursesList' (which are not in 'stagedCoursesForAction)
                                items(coursesList) { course ->
                                    AvailableCourseItem(course = course) {
                                        println("Selecting course: ${course.courseCode}")
                                        // When 'Select' is pressed, add to 'stagedCoursesForAction'
                                        stagedCoursesForAction.add(it)
                                        // Update coursesList by removing 'it' and creating a new list instance
                                        coursesList = (coursesList.toMutableList() - it).sortedBy { c -> c.courseCode }
                                        println("stagedCoursesForAction after select: ${stagedCoursesForAction.map { c -> c.courseCode }}")
                                        println("coursesList after select: ${coursesList.map { c -> c.courseCode }}")
                                    }
                                }
                                if (coursesList.isEmpty() && searchCourseQuery.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "No courses found for '${searchCourseQuery}'",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (coursesList.isEmpty() && searchCourseQuery.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Loading courses...",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Selected Courses (Current Record)", // This table now shows staged changes
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
                                // Display courses in 'stagedCoursesForAction' (the current working set)
                                items(stagedCoursesForAction) { course ->
                                    // All items in stagedCoursesForAction should be actionable (can be deselected)
                                    ActionableCourseItem(course = course) {
                                        println("Deselecting course: ${course.courseCode}")
                                        // When 'Deselect' is pressed, remove from 'stagedCoursesForAction'
                                        stagedCoursesForAction.remove(course)
                                        // Add back to coursesList by creating a new list instance, maintaining sort order
                                        coursesList = (coursesList.toMutableList() + course).sortedBy { c -> c.courseCode }
                                        println("stagedCoursesForAction after deselect: ${stagedCoursesForAction.map { c -> c.courseCode }}")
                                        println("coursesList after deselect: ${coursesList.map { c -> c.courseCode }}")
                                    }
                                }
                                if (stagedCoursesForAction.isEmpty()) { // Check stagedCoursesForAction for display
                                    item {
                                        Text(
                                            text = "No courses selected. Select from the list above.",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Single "Confirm Changes" button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    println("Confirm Changes button pressed.")
                                    processConfirmAction(
                                        context,
                                        httpClient,
                                        currentStudentId,
                                        enlistmentId, // Pass the current enlistment ID
                                        initialEnrolledCourses, // The original state (from backend)
                                        stagedCoursesForAction // The desired final state (from UI)
                                    )
                                    // Trigger refresh
                                    refreshTrigger++ // Increment to trigger LaunchedEffect
                                    println("refreshTrigger incremented to: $refreshTrigger")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(
                                text = "Confirm Changes",
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                1 -> { // Section Tab (unchanged as per request scope)
                    LazyColumn(
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
                            .groupBy { it.courseId + (it.sectionId ?: "") } // Group by CourseID and actual SectionID (handle null)
                            .map { (_, entries) ->
                                val first = entries.first()
                                GroupedSection(
                                    courseId = first.courseId,
                                    courseName = first.courseName,
                                    courseCode = first.courseCode,
                                    sectionId = first.sectionId ?: "", // Provide default empty string
                                    sectionCode = first.sectionCode ?: "", // Provide default empty string
                                    courseUnits = first.courseUnits,
                                    schedules = entries.mapNotNull { enlistedCourse ->
                                        if (enlistedCourse.day != null && enlistedCourse.startTime != null && enlistedCourse.endTime != null) {
                                            ScheduleEntry(enlistedCourse.day, enlistedCourse.startTime, enlistedCourse.endTime)
                                        } else {
                                            null // Filter out schedules with null time info
                                        }
                                    }
                                )
                            }


                        items(groupedSections) { section ->
                            val uniqueId = section.courseId + section.sectionId // Use section.sectionId
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
                                            val existingUniqueId = existing.courseId + existing.sectionId
                                            selectedCourseSection.removeAt(existingIndex)
                                            selectedSectionIds.remove(existingUniqueId)
                                        }
                                        selectedSectionIds.add(uniqueId)
                                        // Pick one representative to insert
                                        val firstSchedule = toggled.schedules.firstOrNull() // Use toggled.schedules
                                        selectedCourseSection.add(
                                            EnlistedCourse(
                                                courseId = toggled.courseId,
                                                courseName = toggled.courseName,
                                                courseCode = toggled.courseCode,
                                                sectionId = toggled.sectionId,
                                                sectionCode = toggled.sectionCode,
                                                courseUnits = toggled.courseUnits,
                                                day = firstSchedule?.day,
                                                startTime = firstSchedule?.startTime,
                                                endTime = firstSchedule?.endTime
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
                                                val sectionId = course.sectionId ?: "" // Handle nullable SectionID
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
                        var showDialog by remember { mutableStateOf(false) }
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
                                            val uniqueId = grouped.courseId + grouped.sectionId // Use grouped.sectionId
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
                                Button(onClick = { showDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                }
                2 -> { // Finalize Tab (unchanged)
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

// --- Helper Functions (START) ---

suspend fun processConfirmAction(
    context: Context,
    httpClient: HttpClient,
    studentId: String,
    enlistmentId: String?, // The enlistment ID, could be null if no courses were ever added
    initialEnrolledCourses: List<Course>, // The courses that were initially on record
    finalSelectedCourses: List<Course> // The courses the user wants to have (current state of 'Selected Courses' table)
) {
    println("--- Processing Confirm Action ---")
    println("Initial Enrolled Courses: ${initialEnrolledCourses.map { it.courseCode }}")
    println("Final Selected Courses (staged): ${finalSelectedCourses.map { it.courseCode }}")

    // Identify courses to add: present in finalSelectedCourses but not in initialEnrolledCourses
    val coursesToAdd = finalSelectedCourses.filter { it !in initialEnrolledCourses }
    // Identify courses to remove: present in initialEnrolledCourses but not in finalSelectedCourses
    val coursesToRemove = initialEnrolledCourses.filter { it !in finalSelectedCourses }

    println("Courses to Add: ${coursesToAdd.map { it.courseCode }}")
    println("Courses to Remove: ${coursesToRemove.map { it.courseCode }}")

    var anyFailure = false
    var currentEnlistmentId = enlistmentId // Use a mutable copy for updates

    // Process additions
    for (course in coursesToAdd) {
        println("Attempting to add: ${course.courseCode}")
        val (success, newId) = ktorAddEnrollment(
            context = context,
            httpClient = httpClient,
            studentId = studentId,
            courseId = course.courseId,
            currentEnlistmentId = currentEnlistmentId // Pass the current or newly obtained enlistment ID
        )
        if (success) {
            // If a new enlistment ID was generated (e.g., first course added), store it
            if (currentEnlistmentId == null && newId != null) {
                currentEnlistmentId = newId
                println("New enlistment ID obtained: $newId")
            }
            context.toast("Added ${course.courseCode}")
        } else {
            context.toast("Failed to add ${course.courseCode}")
            anyFailure = true
        }
    }

    // Process removals
    for (course in coursesToRemove) {
        println("Attempting to remove: ${course.courseCode}")
        val success = ktorRemoveEnrollment(
            context = context,
            httpClient = httpClient,
            studentId = studentId,
            courseId = course.courseId
        )
        if (success) {
            context.toast("Removed ${course.courseCode}")
        } else {
            context.toast("Failed to remove ${course.courseCode}")
            anyFailure = true
        }
    }

    if (!anyFailure) {
        context.toast("All changes confirmed successfully!")
    } else {
        context.toast("Some changes failed to apply. Please check the console for details.")
    }
    println("--- Confirm Action Finished ---")
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

private suspend fun fetchCoursesSuspend(context: Context, httpClient: HttpClient, query: String): List<Course> {
    return try {
        val fullUrl = "${serverURL}search_courseinfo.php?query=${query}"
        println("Fetching all courses from: $fullUrl")
        val response: HttpResponse = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Raw JSON response for all courses: $responseBodyString")
        if (response.status.value == 200) {
            val parsedResponse = Json.decodeFromString<CourseSearchResponse>(responseBodyString)
            if (parsedResponse.status == "success" && parsedResponse.data != null) {
                parsedResponse.data
            } else {
                context.toast("Server reported error (all courses): ${parsedResponse.message ?: "Unknown error"}")
                emptyList()
            }
        } else {
            context.toast("HTTP Error fetching all courses: ${response.status.value} - ${response.status.description}")
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Error fetching all courses: ${e.localizedMessage}")
        emptyList()
    }
}

private suspend fun fetchCourseSectionsSuspend(context: Context, httpClient: HttpClient, studentID: String): List<EnlistedCourse> {
    return try {
        val fullUrl = "${serverURL}get_courseEnlisted.php?StudentID=${studentID}"
        println("Fetching enlisted courses from: $fullUrl")
        val response: HttpResponse = httpClient.get(fullUrl)
        val responseBodyString = response.bodyAsText()
        println("Raw JSON response for enlisted courses: $responseBodyString")
        if (response.status.value == 200) {
            val parsedResponse = Json.decodeFromString<CourseSectionSearchResponse>(responseBodyString)
            if (parsedResponse.status == "success" && parsedResponse.data != null) {
                parsedResponse.data
            } else {
                context.toast("Server reported error (enlisted courses): ${parsedResponse.message ?: "Unknown error"}")
                emptyList()
            }
        } else {
            context.toast("HTTP Error fetching enlisted courses: ${response.status.value} - ${response.status.description}")
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Error fetching enlisted courses: ${e.localizedMessage}")
        emptyList()
    }
}

suspend fun ktorAddEnrollment(
    context: Context,
    httpClient: HttpClient,
    studentId: String,
    courseId: String,
    currentEnlistmentId: String?
): Pair<Boolean, String?> {
    return try {
        val urlBuilder = StringBuilder("${serverURL}add_enlistment.php?student_id=$studentId&course_id=$courseId")
        if (currentEnlistmentId != null) {
            urlBuilder.append("&enlistment_id=$currentEnlistmentId")
        }
        val fullUrl = urlBuilder.toString()
        println("Sending add enrollment request to: $fullUrl")

        val response = httpClient.get(fullUrl)
        val responseText = response.bodyAsText()
        println("Server Response (add_enlistment): ${response.status} - $responseText")

        val json = Json.parseToJsonElement(responseText).jsonObject
        val success = json["status"]?.jsonPrimitive?.content == "success"
        val newEnlistmentId = json["enlistment_id"]?.jsonPrimitive?.content

        Pair(success, newEnlistmentId)
    } catch (e: Exception) {
        e.printStackTrace()
        context.toast("Enrollment failed: ${e.localizedMessage}")
        Pair(false, null)
    }
}


private suspend fun ktorRemoveEnrollment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
    try {
        if (studentId.isBlank() || courseId.isBlank()) {
            context.toast("Missing student or course ID.")
            return false
        }
        val fullUrl = "${serverURL}remove_enlistment.php?student_id=${studentId}&course_id=${courseId}"
        println("Sending remove enrollment request to: $fullUrl")
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
    val grouped = sections.groupBy { it.courseId + (it.sectionCode ?: "") } // Handle nullable sectionCode in grouping

    val groupedSections = grouped.map { (_, entries) ->
        val first = entries.first()
        GroupedSection(
            courseId = first.courseId,
            courseName = first.courseName,
            courseCode = first.courseCode,
            sectionId = first.sectionId ?: "", // Provide default empty string for non-nullable in GroupedSection
            sectionCode = first.sectionCode ?: "", // Provide default empty string for non-nullable in GroupedSection
            courseUnits = first.courseUnits,
            schedules = entries.mapNotNull { enlistedCourse ->
                if (enlistedCourse.day != null && enlistedCourse.startTime != null && enlistedCourse.endTime != null) {
                    ScheduleEntry(enlistedCourse.day, enlistedCourse.startTime, enlistedCourse.endTime)
                } else {
                    null // Filter out schedules with null time info
                }
            }
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
            // If the current section causes a conflict, try the next section for this course (if any)
        }

        return null
    }

    return backtrack(groupedSections.values.toList(), 0, mutableListOf(), mutableListOf())
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
