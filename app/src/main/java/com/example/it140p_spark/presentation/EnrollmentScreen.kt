package com.example.it140p_spark.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.it140p_spark.data.utils.SERVER_URL
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

    val searchCourseQuery by remember { mutableStateOf("") }

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

    // --- Finalize Tab States ---
    val finalizeEnlistedCourses = remember { mutableStateOf<List<FinalizationEnlistedCourse>>(emptyList()) }
    var finalizeStudentTerm by remember { mutableStateOf("Loading...") }
    val finalizePaymentTypes = listOf("Installment 1", "Installment 2", "Full Payment")
    var finalizeSelectedPaymentType by remember { mutableStateOf(finalizePaymentTypes[0]) }
    val finalizeEnrollmentStatuses = listOf("Not Paid")
    val finalizeSelectedEnrollmentStatus by remember { mutableStateOf(finalizeEnrollmentStatuses[0]) }
    var finalizeIsLoading by remember { mutableStateOf(true) }
    var finalizeErrorMessage by remember { mutableStateOf<String?>(null) }
    var finalizeIsSubmitting by remember { mutableStateOf(false) }

    // Load finalization data when needed
    LaunchedEffect(studentId, selectedTabIndex) {
        if (selectedTabIndex == 2) {
            finalizeIsLoading = true
            finalizeErrorMessage = null
            var rawResponseContent: String? = null
            try {
                val response =
                    httpClient.get("${SERVER_URL}get_studentFinalization.php?student_id=$studentId")
                rawResponseContent = response.bodyAsText()
                val parsedData =
                    Json.decodeFromString<StudentFinalizationDataResponse>(rawResponseContent)
                if (parsedData.status == "success") {
                    finalizeEnlistedCourses.value =
                        parsedData.courses?.filter { it.SectionID != null } ?: emptyList()
                    finalizeStudentTerm = parsedData.term ?: "N/A"
                    if (finalizeEnlistedCourses.value.isEmpty()) {
                        finalizeErrorMessage =
                            "You have no enlisted and sectioned courses to finalize."
                    }
                    if (parsedData.term == null || parsedData.term.isNullOrBlank()) {
                        val currentMessage = finalizeErrorMessage ?: ""
                        finalizeErrorMessage =
                            currentMessage + (if (currentMessage.isNotEmpty()) ". " else "") + "Could not retrieve student's current term."
                        if (finalizeErrorMessage?.trim() == "") finalizeErrorMessage =
                            "Could not retrieve student's current term."
                    }
                } else {
                    finalizeErrorMessage =
                        "Failed to load data: ${parsedData.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finalizeErrorMessage =
                    "Network or parsing error loading courses: ${e.localizedMessage ?: "Unknown error"}. " +
                            "Raw response from get_studentFinalization.php: '${rawResponseContent ?: "N/A"}'"
            } finally {
                finalizeIsLoading = false
            }
        }
    }

    // LaunchedEffect to manage data fetching based on tab, search query, and refreshTrigger
    LaunchedEffect(selectedTabIndex, searchCourseQuery, refreshTrigger, selectedCourses.size) { // Add refreshTrigger here
        println("LaunchedEffect triggered. selectedTabIndex: $selectedTabIndex, searchCourseQuery: $searchCourseQuery, refreshTrigger: $refreshTrigger")
        EnlistmentFunctions.fetchEnlistmentId(context, httpClient, studentId) { fetchedId ->
            enlistmentIdState.value = fetchedId
            println("Fetched Enlistment ID: $fetchedId")
        }

        if (selectedTabIndex == 0) {
            // Fetch all courses from the server
            val allFetchedCourses = EnlistmentFunctions.fetchCoursesSuspend(context, httpClient, searchCourseQuery)
            println("Fetched all available courses: ${allFetchedCourses.size} courses.")

            // Fetch courses currently enlisted by the student
            val enlistedCoursesFromBackend = EnlistmentFunctions.fetchCourseEnlistedSuspend(
                context,
                httpClient,
                studentId
            )
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
            enlistedCourses = SectioningFunctions.fetchCourseSectionsSuspend(
                context,
                httpClient,
                studentId
            )
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

            // Remove the when block and directly use TabsContent
            TabsContent(
                selectedTabIndex = selectedTabIndex,
                // Enlist Tab
                currentStudentId = studentId,
                enlistmentId = enlistmentId,
                coursesList = coursesList,
                stagedCoursesForAction = stagedCoursesForAction,
                initialEnrolledCourses = initialEnrolledCourses,
                searchCourseQuery = searchCourseQuery,
                onSelectCourse = { course ->
                    stagedCoursesForAction.add(course)
                    coursesList =
                        (coursesList.toMutableList() - course).sortedBy { c -> c.courseCode }
                },
                onDeselectCourse = { course ->
                    stagedCoursesForAction.remove(course)
                    coursesList =
                        (coursesList.toMutableList() + course).sortedBy { c -> c.courseCode }
                },
                onConfirmChanges = {
                    coroutineScope.launch {
                        EnlistmentFunctions.processConfirmAction(
                            context = context,
                            httpClient = httpClient,
                            studentId = studentId,
                            enlistmentId = enlistmentId,
                            initialEnrolledCourses = initialEnrolledCourses,
                            finalSelectedCourses = stagedCoursesForAction,
                            toast = { context.toast(it) }
                        )
                        refreshTrigger += 1
                    }
                },
                // Section Tab
                enlistedCourses = enlistedCourses,
                selectedSectionIds = selectedSectionIds,
                selectedCourseSection = selectedCourseSection,
                suggestedSchedule = suggestedSchedule,
                showSuggestionDialog = showSuggestionDialog,
                onSuggestSchedule = {
                    val suggestion = SectioningFunctions.suggestValidSchedule(enlistedCourses)
                    if (suggestion != null) {
                        suggestedSchedule = suggestion
                        showSuggestionDialog = true
                    } else {
                        context.toast("No valid schedule found.")
                    }
                },
                onToggleSelect = { toggled ->
                    val existingIndex =
                        selectedCourseSection.indexOfFirst { it.courseId == toggled.courseId }
                    val uniqueId = toggled.courseId + (toggled.sectionId)
                    val isSelected = selectedSectionIds.contains(uniqueId)
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
                onSectionConfirmed = { section ->
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
                },
                onApplySuggestedSchedule = {
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
                    coroutineScope.launch {
                        if (enlistmentId != null) {
                            selectedCourseSection.forEach { course ->
                                val courseId = course.courseId
                                val sectionId = course.sectionId ?: ""
                                SectioningFunctions.ktorInsertSection(
                                    context, httpClient, enlistmentId, courseId, sectionId
                                )
                            }
                            context.toast("Suggested schedule applied and submitted.")
                        } else {
                            context.toast("Enlistment ID missing.")
                        }
                    }
                    showSuggestionDialog = false
                },
                onDismissSuggestionDialog = { showSuggestionDialog = false },
                studentId = studentId,
                httpClient = httpClient,
                context = context,
                // Finalize Tab
                finalizeEnlistedCourses = finalizeEnlistedCourses.value,
                finalizeStudentTerm = finalizeStudentTerm,
                finalizePaymentTypes = finalizePaymentTypes,
                finalizeSelectedPaymentType = finalizeSelectedPaymentType,
                onPaymentTypeChange = { newType -> finalizeSelectedPaymentType = newType },
                finalizeEnrollmentStatuses = finalizeEnrollmentStatuses,
                finalizeSelectedEnrollmentStatus = finalizeSelectedEnrollmentStatus,
                finalizeIsLoading = finalizeIsLoading,
                finalizeErrorMessage = finalizeErrorMessage,
                finalizeIsSubmitting = finalizeIsSubmitting,
                onSubmit = {
                    if (finalizeEnlistedCourses.value.isEmpty() || finalizeIsSubmitting) {
                        context.toast("No courses to finalize or already submitting.")
                        return@TabsContent
                    }
                    if (finalizeStudentTerm == "Loading..." || finalizeStudentTerm == "N/A" || finalizeStudentTerm.isBlank()) {
                        context.toast("Please wait for term to load or fix term loading issue before finalizing.")
                        return@TabsContent
                    }
                    val unsectionedCourses =
                        finalizeEnlistedCourses.value.filter { it.SectionID == null }
                    if (unsectionedCourses.isNotEmpty()) {
                        context.toast("Cannot finalize. Some enlisted courses are not yet sectioned.")
                        return@TabsContent
                    }
                    val enlistmentIdToFinalize =
                        finalizeEnlistedCourses.value.firstOrNull()?.EnlistmentID
                    if (enlistmentIdToFinalize == null) {
                        context.toast("Could not find Enlistment ID for finalization.")
                        return@TabsContent
                    }
                    finalizeIsSubmitting = true
                    coroutineScope.launch {
                        try {
                            val finalizeResponse = FinalizeFunctions.finalizeEnrollment(
                                context = context,
                                httpClient = httpClient,
                                studentId = studentId,
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
                onStatusChange = { /* status is fixed */ }
            )
        }
    }
}

// --- Helper Functions (START) ---

@Composable
fun AvailableCourseItem(course: Course, onSelect: (Course) -> Unit) {
    Card(
        modifier = Modifier
            .width(225.dp)
            .height(175.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${course.courseCode} - ${course.courseName}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Units: ${course.courseUnits}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.FilledTonalButton(
                onClick = { onSelect(course) },
                modifier = Modifier
                    .height(28.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDCEDC8),
                    contentColor = Color(0xFF33691E)
                )
            ) {
                Text("Select", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ActionableCourseItem(course: Course, onUnselect: () -> Unit) {
    Card(
        modifier = Modifier
            .width(225.dp)
            .height(175.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${course.courseCode} - ${course.courseName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onUnselect,
                modifier = Modifier
                    .height(28.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF8BBD0),
                    contentColor = Color(0xFF880E4F)
                )
            ) {
                Text("Remove", style = MaterialTheme.typography.labelSmall)
            }
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

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(250.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "${section.courseCode} - ${section.courseName}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Section: ${section.sectionCode}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (isSelected) {
                        onToggleSelect(section)
                    } else {
                        showDialog = true
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(32.dp), // Extra small height
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFFF8BBD0) else Color(0xFFDCEDC8),
                    contentColor = if (isSelected) Color(0xFF880E4F) else Color(0xFF33691E)
                )
            ) {
                Text(
                    if (isSelected) "Remove" else "Select",
                    style = MaterialTheme.typography.labelMedium
                )
            }
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
                TextButton(onClick = {
                    onToggleSelect(section)
                    onSectionConfirmed(section)
                    showDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
// --- Helper Functions (END) ---
