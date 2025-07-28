package com.example.it140p_spark.presentation

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
import kotlinx.serialization.json.Json
import androidx.compose.material3.AlertDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import com.example.it140p_spark.data.SERVER_URL
import com.example.it140p_spark.ui.components.StudentScheduleTimetable
import com.example.it140p_spark.data.models.*
import com.example.it140p_spark.data.functions.EnlistmentFunctions
import com.example.it140p_spark.data.functions.SectioningFunctions
import com.example.it140p_spark.data.functions.FinalizeFunctions

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
    LaunchedEffect(selectedTabIndex, searchCourseQuery, refreshTrigger, selectedCourses.size) { // Add refreshTrigger here
        println("LaunchedEffect triggered. selectedTabIndex: $selectedTabIndex, searchCourseQuery: $searchCourseQuery, refreshTrigger: $refreshTrigger")
        EnlistmentFunctions.fetchEnlistmentId(context, httpClient, currentStudentId) { fetchedId ->
            enlistmentIdState.value = fetchedId
            println("Fetched Enlistment ID: $fetchedId")
        }

        if (selectedTabIndex == 0) {
            // Fetch all courses from the server
            val allFetchedCourses = EnlistmentFunctions.fetchCoursesSuspend(context, httpClient, searchCourseQuery)
            println("Fetched all available courses: ${allFetchedCourses.size} courses.")

            // Fetch courses currently enlisted by the student
            val enlistedCoursesFromBackend = EnlistmentFunctions.fetchCourseEnlistedSuspend(context, httpClient, currentStudentId)
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
            enlistedCourses = SectioningFunctions.fetchCourseSectionsSuspend(context, httpClient, currentStudentId)
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
                0 -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                                    EnlistmentFunctions.processConfirmAction(
                                        context = context,
                                        httpClient = httpClient,
                                        studentId = currentStudentId,
                                        enlistmentId = enlistmentId, // Pass the current enlistment ID
                                        initialEnrolledCourses = initialEnrolledCourses,
                                        finalSelectedCourses = stagedCoursesForAction,
                                        toast = { context.toast(it) }
                                    )
                                    // Trigger refresh
                                    refreshTrigger += 1 // Use += 1 to increment Int
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
                1 -> { // Section Tab
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
                                        val suggestion = SectioningFunctions.suggestValidSchedule(enlistedCourses)
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
                            .groupBy { it.courseId + (it.sectionId ?: "") }
                            .map { (_, entries) ->
                                val first = entries.first()
                                GroupedSection(
                                    courseId = first.courseId,
                                    courseName = first.courseName,
                                    courseCode = first.courseCode,
                                    sectionId = first.sectionId ?: "",
                                    sectionCode = first.sectionCode ?: "",
                                    courseUnits = first.courseUnits,
                                    schedules = entries.mapNotNull { enlistedCourse ->
                                        if (enlistedCourse.day != null && enlistedCourse.startTime != null && enlistedCourse.endTime != null) {
                                            ScheduleEntry(enlistedCourse.day, enlistedCourse.startTime, enlistedCourse.endTime)
                                        } else null
                                    }
                                )
                            }

                        items(groupedSections) { section ->
                            val uniqueId = section.courseId + section.sectionId
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
                                        val firstSchedule = toggled.schedules.firstOrNull()
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
                                            SectioningFunctions.ktorInsertSection(
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
                            StudentScheduleTimetable(context, currentStudentId, httpClient)
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (enlistmentId != null) {
                                            selectedCourseSection.forEach { course ->
                                                val courseId = course.courseId
                                                val sectionId = course.sectionId ?: ""
                                                SectioningFunctions.ktorInsertSection(context, httpClient, enlistmentId, courseId, sectionId)
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

                    // Suggested Schedule Dialog
                    if (showSuggestionDialog) {
                        var showDialog by remember { mutableStateOf(false) }
                        AlertDialog(
                            onDismissRequest = { showSuggestionDialog = false },
                            title = { Text("Suggested Schedule") },
                            text = {
                                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                                Column(
                                    modifier = Modifier
                                        .heightIn(min = 100.dp, max = 400.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    suggestedSchedule.forEach { section ->
                                        Text("${section.courseCode} - ${section.courseName}")
                                        Text("Section: ${section.sectionCode}")
                                        section.schedules.groupBy { it.day }.forEach { (day, times) ->
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

                                            Text("$day: $timeRanges")
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
                                            val uniqueId = grouped.courseId + grouped.sectionId
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
                // NEW
                2 -> {
                    val finalizeEnlistedCourses = remember { mutableStateOf<List<FinalizationEnlistedCourse>>(emptyList()) }
                    var finalizeStudentTerm by remember { mutableStateOf("Loading...") }
                    val finalizePaymentTypes = listOf("Installment 1", "Installment 2", "Full Payment")
                    var finalizeSelectedPaymentType by remember { mutableStateOf(finalizePaymentTypes[0]) }

                    val finalizeEnrollmentStatuses = listOf("Not Paid")
                    var finalizeSelectedEnrollmentStatus by remember { mutableStateOf(finalizeEnrollmentStatuses[0]) }

                    var finalizeIsLoading by remember { mutableStateOf(true) }
                    var finalizeErrorMessage by remember { mutableStateOf<String?>(null) }
                    var finalizeIsSubmitting by remember { mutableStateOf(false) }

                    LaunchedEffect(currentStudentId) {
                        finalizeIsLoading = true
                        finalizeErrorMessage = null
                        var rawResponseContent: String? = null
                        try {
                            val response = httpClient.get("${SERVER_URL}get_studentFinalization.php?student_id=$currentStudentId")
                            rawResponseContent = response.bodyAsText()
                            val parsedData = Json.decodeFromString<StudentFinalizationDataResponse>(rawResponseContent)

                            if (parsedData.status == "success") {
                                finalizeEnlistedCourses.value = parsedData.courses?.filter { it.SectionID != null } ?: emptyList()
                                finalizeStudentTerm = parsedData.term ?: "N/A"

                                if (finalizeEnlistedCourses.value.isEmpty()) {
                                    finalizeErrorMessage = "You have no enlisted and sectioned courses to finalize."
                                }
                                if (parsedData.term == null || parsedData.term.isNullOrBlank()) {
                                    val currentMessage = finalizeErrorMessage ?: ""
                                    finalizeErrorMessage = currentMessage + (if (currentMessage.isNotEmpty()) ". " else "") + "Could not retrieve student's current term."
                                    if (finalizeErrorMessage?.trim() == "") finalizeErrorMessage = "Could not retrieve student's current term."
                                }
                            } else {
                                finalizeErrorMessage = "Failed to load data: ${parsedData.message ?: "Unknown error"}"
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            finalizeErrorMessage = "Network or parsing error loading courses: ${e.localizedMessage ?: "Unknown error"}. " +
                                    "Raw response from get_studentFinalization.php: '${rawResponseContent ?: "N/A"}'"
                        } finally {
                            finalizeIsLoading = false
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Finalize Enrollment",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (finalizeIsLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            Text("Loading enrollment details...")
                        } else if (finalizeErrorMessage != null) {
                            Text(
                                text = finalizeErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Text(
                                text = "Enlisted Courses:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (finalizeEnlistedCourses.value.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .heightIn(max = 250.dp)
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    items(finalizeEnlistedCourses.value) { course ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            elevation = CardDefaults.cardElevation(2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(text = "${course.CourseName} (${course.CourseCode})", style = MaterialTheme.typography.bodyLarge)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Units: ${course.CourseUnits}", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "Section: ${course.SectionCODE ?: "N/A"} (Room: ${course.Room ?: "N/A"})", style = MaterialTheme.typography.bodySmall)
                                                Text(text = "Instructor: ${course.InstructorFirstName ?: ""} ${course.InstructorLastName ?: ""}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No courses found for finalization. Please ensure you have enlisted and sectioned your courses.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(text = "Payment Type:", style = MaterialTheme.typography.titleSmall)
                                Row(modifier = Modifier.selectableGroup()) {
                                    finalizePaymentTypes.forEach { type ->
                                        Row(
                                            Modifier
                                                .height(40.dp)
                                                .selectable(
                                                    selected = (finalizeSelectedPaymentType == type),
                                                    onClick = { finalizeSelectedPaymentType = type }
                                                )
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (finalizeSelectedPaymentType == type),
                                                onClick = null
                                            )
                                            Text(
                                                text = type,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Term: $finalizeStudentTerm",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(text = "Enrollment Status:", style = MaterialTheme.typography.titleSmall)
                                Row(modifier = Modifier.selectableGroup()) {
                                    finalizeEnrollmentStatuses.forEach { status ->
                                        Row(
                                            Modifier
                                                .height(40.dp)
                                                .selectable(
                                                    selected = (finalizeSelectedEnrollmentStatus == status),
                                                    onClick = { /* status is fixed */ }
                                                )
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (finalizeSelectedEnrollmentStatus == status),
                                                onClick = null
                                            )
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (finalizeEnlistedCourses.value.isEmpty() || finalizeIsSubmitting) {
                                        context.toast("No courses to finalize or already submitting.")
                                        return@Button
                                    }
                                    if (finalizeStudentTerm == "Loading..." || finalizeStudentTerm == "N/A" || finalizeStudentTerm.isBlank()) {
                                        context.toast("Please wait for term to load or fix term loading issue before finalizing.")
                                        return@Button
                                    }

                                    val unsectionedCourses = finalizeEnlistedCourses.value.filter { it.SectionID == null }
                                    if (unsectionedCourses.isNotEmpty()) {
                                        context.toast("Cannot finalize. Some enlisted courses are not yet sectioned.")
                                        return@Button
                                    }

                                    val enlistmentIdToFinalize = finalizeEnlistedCourses.value.firstOrNull()?.EnlistmentID
                                    if (enlistmentIdToFinalize == null) {
                                        context.toast("Could not find Enlistment ID for finalization.")
                                        return@Button
                                    }

                                    finalizeIsSubmitting = true
                                    coroutineScope.launch {
                                        try {
                                            val finalizeResponse = FinalizeFunctions.finalizeEnrollment(
                                                context = context,
                                                httpClient = httpClient,
                                                studentId = currentStudentId,
                                                enlistmentId = enlistmentIdToFinalize.toString(),
                                                paymentType = finalizeSelectedPaymentType,
                                                term = finalizeStudentTerm,
                                                enrollmentStatus = finalizeSelectedEnrollmentStatus
                                            )
                                            if (finalizeResponse.status == "success") {
                                                context.toast("Enrollment finalized successfully!")
                                                selectedTabIndex = 0
                                            } else {
                                                context.toast("Failed to finalize enrollment: ${finalizeResponse.message ?: "Unknown error"}")
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            context.toast("Error during enrollment finalization: ${e.localizedMessage ?: "Unknown error"}.")
                                        } finally {
                                            finalizeIsSubmitting = false
                                        }
                                    }
                                },
                                enabled = !finalizeIsLoading && !finalizeIsSubmitting &&
                                        finalizeEnlistedCourses.value.isNotEmpty() &&
                                        finalizeEnlistedCourses.value.all { it.SectionID != null } &&
                                        finalizeStudentTerm != "Loading..." && !finalizeStudentTerm.isBlank()
                            ) {
                                Text(if (finalizeIsSubmitting) "Submitting..." else "Submit Enrollment")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Functions (START) ---

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
fun AvailableGroupedSection(section: GroupedSection, isSelected: Boolean, onToggleSelect: (GroupedSection) -> Unit, onSectionConfirmed: (GroupedSection) -> Unit) {
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
