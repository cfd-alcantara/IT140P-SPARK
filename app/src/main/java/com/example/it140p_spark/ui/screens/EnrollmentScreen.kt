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
import com.example.it140p_spark.ui.components.CourseCard


const val serverURL = "http://192.168.10.1/student_management_system/REST/"

@Serializable
data class Course(
    @SerialName("CourseID") val courseId: String,
    @SerialName("CourseName") val courseName: String,
    @SerialName("CourseCode") val courseCode: String,
    @SerialName("CourseUnits") val courseUnits: String
)

@Serializable
data class CourseSearchResponse(
    val status: String,
    val message: String? = null,
    val data: List<Course>? = null
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
    val selectedCourses = remember { mutableStateListOf<Course>() }
    var coursesList: List<Course> by remember { mutableStateOf(emptyList()) }
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
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Section Content - Placeholder",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        item {
                            CourseCard(
                                courseCode = "IT200-1D",
                                description = "IT Capstone Project 1",
                                units = "3 Units",
                                yearAndTerm = "Y4T1",
                                available = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            CourseCard(
                                courseCode = "IT201-2A",
                                description = "Web Development",
                                units = "3 Units",
                                yearAndTerm = "Y4T1",
                                available = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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