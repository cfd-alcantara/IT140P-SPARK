package com.example.it140p_spark.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import io.ktor.client.*
import com.example.it140p_spark.data.models.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it140p_spark.ui.components.StudentScheduleTimetable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TabsContent(
    selectedTabIndex: Int,
    // Enlist Tab
    currentStudentId: String,
    enlistmentId: String?,
    coursesList: List<Course>,
    stagedCoursesForAction: MutableList<Course>,
    initialEnrolledCourses: MutableList<Course>,
    searchCourseQuery: String,
    onSelectCourse: (Course) -> Unit,
    onDeselectCourse: (Course) -> Unit,
    onConfirmChanges: () -> Unit,
    // Section Tab
    enlistedCourses: List<EnlistedCourse>,
    selectedSectionIds: MutableList<String>,
    selectedCourseSection: MutableList<EnlistedCourse>,
    suggestedSchedule: List<GroupedSection>,
    showSuggestionDialog: Boolean,
    onSuggestSchedule: () -> Unit,
    onToggleSelect: (GroupedSection) -> Unit,
    onSectionConfirmed: (GroupedSection) -> Unit,
    onApplySuggestedSchedule: () -> Unit,
    onDismissSuggestionDialog: () -> Unit,
    studentId: String,
    httpClient: HttpClient,
    context: Context,
    // Finalize Tab
    finalizeEnlistedCourses: List<FinalizationEnlistedCourse>,
    finalizeStudentTerm: String,
    finalizePaymentTypes: List<String>,
    finalizeSelectedPaymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    finalizeEnrollmentStatuses: List<String>,
    finalizeSelectedEnrollmentStatus: String,
    finalizeIsLoading: Boolean,
    finalizeErrorMessage: String?,
    finalizeIsSubmitting: Boolean,
    onSubmit: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    when (selectedTabIndex) {
        0 -> EnlistTabContent(
            currentStudentId = currentStudentId,
            enlistmentId = enlistmentId,
            coursesList = coursesList,
            stagedCoursesForAction = stagedCoursesForAction,
            initialEnrolledCourses = initialEnrolledCourses,
            searchCourseQuery = searchCourseQuery,
            onSelectCourse = onSelectCourse,
            onDeselectCourse = onDeselectCourse,
            onConfirmChanges = onConfirmChanges
        )
        1 -> SectionTabContent(
            enlistedCourses = enlistedCourses,
            selectedSectionIds = selectedSectionIds,
            selectedCourseSection = selectedCourseSection,
            suggestedSchedule = suggestedSchedule,
            showSuggestionDialog = showSuggestionDialog,
            onSuggestSchedule = onSuggestSchedule,
            onToggleSelect = onToggleSelect,
            onSectionConfirmed = onSectionConfirmed,
            onApplySuggestedSchedule = onApplySuggestedSchedule,
            onDismissSuggestionDialog = onDismissSuggestionDialog,
            studentId = studentId,
            httpClient = httpClient,
            context = context
        )
        2 -> FinalizeTabContent(
            finalizeEnlistedCourses = finalizeEnlistedCourses,
            finalizeStudentTerm = finalizeStudentTerm,
            finalizePaymentTypes = finalizePaymentTypes,
            finalizeSelectedPaymentType = finalizeSelectedPaymentType,
            onPaymentTypeChange = onPaymentTypeChange,
            finalizeEnrollmentStatuses = finalizeEnrollmentStatuses,
            finalizeSelectedEnrollmentStatus = finalizeSelectedEnrollmentStatus,
            finalizeIsLoading = finalizeIsLoading,
            finalizeErrorMessage = finalizeErrorMessage,
            finalizeIsSubmitting = finalizeIsSubmitting,
            onSubmit = onSubmit,
            onStatusChange = onStatusChange
        )
    }
}

// --- Tab Content Composables (migrated from EnrollmentScreen) ---
@Composable
fun EnlistTabContent(
    currentStudentId: String,
    enlistmentId: String?,
    coursesList: List<Course>,
    stagedCoursesForAction: MutableList<Course>,
    initialEnrolledCourses: MutableList<Course>,
    searchCourseQuery: String,
    onSelectCourse: (Course) -> Unit,
    onDeselectCourse: (Course) -> Unit,
    onConfirmChanges: () -> Unit
) {
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
                    AvailableCourseItem(course = course) {
                        onSelectCourse(it)
                    }
                }
                if (coursesList.isEmpty() && searchCourseQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "No courses found for '$searchCourseQuery'",
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
            text = "Selected Courses (Current Record)",
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
                items(stagedCoursesForAction) { course ->
                    ActionableCourseItem(course = course) {
                        onDeselectCourse(course)
                    }
                }
                if (stagedCoursesForAction.isEmpty()) {
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
        Button(
            onClick = onConfirmChanges,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Confirm Changes",
            )
        }
    }
}
@Composable
fun SectionTabContent(
    enlistedCourses: List<EnlistedCourse>,
    selectedSectionIds: MutableList<String>,
    selectedCourseSection: MutableList<EnlistedCourse>,
    suggestedSchedule: List<GroupedSection>,
    showSuggestionDialog: Boolean,
    onSuggestSchedule: () -> Unit,
    onToggleSelect: (GroupedSection) -> Unit,
    onSectionConfirmed: (GroupedSection) -> Unit,
    onApplySuggestedSchedule: () -> Unit,
    onDismissSuggestionDialog: () -> Unit,
    studentId: String,
    httpClient: HttpClient,
    context: Context
) {
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
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSuggestSchedule,
                    modifier = Modifier.fillMaxWidth(),
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
                onToggleSelect = { onToggleSelect(it) },
                onSectionConfirmed = { onSectionConfirmed(it) }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                val screen = "Section"
                StudentScheduleTimetable(context, studentId, httpClient, screen)
            }
        }
    }

    if (showSuggestionDialog) {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        AlertDialog(
            onDismissRequest = onDismissSuggestionDialog,
            title = { Text("Suggested Schedule") },
            text = {
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
                TextButton(onClick = onApplySuggestedSchedule) {
                    Text("Apply Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSuggestionDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun FinalizeTabContent(
    finalizeEnlistedCourses: List<FinalizationEnlistedCourse>,
    finalizeStudentTerm: String,
    finalizePaymentTypes: List<String>,
    finalizeSelectedPaymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    finalizeEnrollmentStatuses: List<String>,
    finalizeSelectedEnrollmentStatus: String,
    finalizeIsLoading: Boolean,
    finalizeErrorMessage: String?,
    finalizeIsSubmitting: Boolean,
    onSubmit: () -> Unit,
    onStatusChange: (String) -> Unit
) {
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
                text = finalizeErrorMessage,
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
            if (finalizeEnlistedCourses.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 250.dp)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(finalizeEnlistedCourses) { course ->
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
                val selectedIndex = finalizePaymentTypes.indexOf(finalizeSelectedPaymentType)
                SingleChoiceSegmentedButtonRow {
                    finalizePaymentTypes.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = finalizePaymentTypes.size
                            ),
                            onClick = { onPaymentTypeChange(type) },
                            selected = index == selectedIndex,
                            label = { Text(type, fontSize = 12.sp) }
                        )
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
                                    onClick = { onStatusChange(status) }
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
                onClick = onSubmit,
                enabled = !finalizeIsLoading && !finalizeIsSubmitting &&
                        finalizeEnlistedCourses.isNotEmpty() &&
                        finalizeEnlistedCourses.all { it.SectionID != null } &&
                        finalizeStudentTerm != "Loading..." && finalizeStudentTerm.isNotBlank()
            ) {
                Text(if (finalizeIsSubmitting) "Submitting..." else "Submit Enrollment", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}
