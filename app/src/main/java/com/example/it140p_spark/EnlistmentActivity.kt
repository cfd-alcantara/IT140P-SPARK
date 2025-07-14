package com.example.it140p_spark

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme
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
data class EnlistmentResponse(
    val status: String,
    val message: String
)


class EnlistmentActivity : ComponentActivity() {

    private val SERVER_URL = "http://192.168.10.1/student_management_system/REST/"

    private var currentStudentId: String? by mutableStateOf(null)

    private val selectedCourses = mutableStateListOf<Course>()
    private var coursesList: List<Course> by mutableStateOf(emptyList())
    private var searchCourseQuery by mutableStateOf("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val studentIdFromIntent = intent.getStringExtra("STUDENT_ID")
        println("EnlistmentActivity received Student ID: $studentIdFromIntent")

        if (studentIdFromIntent.isNullOrBlank()) {
            Toast.makeText(this, "Error: Student ID not provided. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentStudentId = studentIdFromIntent

        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    currentStudentId?.let {
                        EnlistmentScreen()
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Student ID missing.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EnlistmentScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

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

        DisposableEffect(Unit) {
            onDispose {
                httpClient.close()
            }
        }

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                fetchCourses(context, httpClient, searchCourseQuery)
            }
        }

        LaunchedEffect(selectedCourses.size) {
            coroutineScope.launch {
                fetchCourses(context, httpClient, searchCourseQuery)
            }
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Student Enlistment",
                    fontSize = 28.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Text(
                    text = "Enlisting for Student ID: ${currentStudentId ?: "N/A"}",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchCourseQuery,
                    onValueChange = { newValue ->
                        searchCourseQuery = newValue
                        coroutineScope.launch {
                            fetchCourses(context, httpClient, newValue)
                        }
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
                                processSelectedCoursesForAction(context, httpClient, true)
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
                                processSelectedCoursesForAction(context, httpClient, false)
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

    private suspend fun processSelectedCoursesForAction(context: Context, httpClient: HttpClient, isAddAction: Boolean) {
        if (selectedCourses.isEmpty()) {
            context.toast("Please select at least one course for action.")
            return
        }

        val coursesToProcess = selectedCourses.toList()
        var anyActionFailed = false
        val successfullyProcessedCourses = mutableListOf<Course>()

        for (course in coursesToProcess) {
            println("${if (isAddAction) "Enlisting" else "Removing"}: StudentID = ${currentStudentId}, CourseID = ${course.courseId}")
            val success = if (isAddAction) {
                KTORaddEnlistment(context, httpClient, currentStudentId!!, course.courseId)
            } else {
                KTORremoveEnlistment(context, httpClient, currentStudentId!!, course.courseId)
            }

            if (success) {
                successfullyProcessedCourses.add(course)
            } else {
                anyActionFailed = true
            }
        }

        selectedCourses.removeAll(successfullyProcessedCourses)

        val actionType = if (isAddAction) "enlisted" else "removed"
        val verb = if (isAddAction) "enlisted" else "removed"

        if (!anyActionFailed && successfullyProcessedCourses.isNotEmpty()) {
            context.toast("All selected courses ${actionType} successfully!")
        } else if (anyActionFailed && successfullyProcessedCourses.isNotEmpty()) {
            context.toast("Some courses ${verb}, but others failed.")
        } else if (anyActionFailed && successfullyProcessedCourses.isEmpty()) {
            context.toast("All selected actions failed.")
        } else {
            context.toast("No courses were processed for action.")
        }
    }


    private suspend fun fetchCourses(context: Context, httpClient: HttpClient, query: String) {
        try {
            val fullUrl = "${SERVER_URL}search_courseinfo.php?query=${query}"
            println("Fetching courses from: $fullUrl")

            val response: HttpResponse = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            println("Raw JSON response for courses: $responseBodyString")

            if (response.status.value == 200) {
                val parsedResponse = Json.decodeFromString<CourseSearchResponse>(responseBodyString)

                if (parsedResponse.status == "success" && parsedResponse.data != null) {
                    coursesList = parsedResponse.data
                } else {
                    context.toast("Server reported error: ${parsedResponse.message ?: "Unknown error"}")
                    coursesList = emptyList()
                }
            } else {
                context.toast("HTTP Error fetching courses: ${response.status.value} - ${response.status.description}")
                coursesList = emptyList()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error fetching courses: ${e.localizedMessage}")
            coursesList = emptyList()
        }
    }

    private suspend fun KTORaddEnlistment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
        try {
            if (studentId.isBlank() || courseId.isBlank()) {
                context.toast("Missing student or course ID.")
                return false
            }

            val fullUrl = "${SERVER_URL}add_enlistment.php?student_id=${studentId}&course_id=${courseId}"
            println("Requesting: $fullUrl")

            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            println("Response from server: ${response.status} - $responseBodyString")

            val enlistmentResponse = Json.decodeFromString<EnlistmentResponse>(responseBodyString)

            if (enlistmentResponse.status == "success") {
                return true
            } else {
                context.toast("Failed to enlist $courseId: ${enlistmentResponse.message}")
                return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Enlistment error for $courseId: ${e.localizedMessage ?: "Unknown"}")
            return false
        }
    }

    private suspend fun KTORremoveEnlistment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
        try {
            if (studentId.isBlank() || courseId.isBlank()) {
                context.toast("Missing student or course ID.")
                return false
            }

            val fullUrl = "${SERVER_URL}remove_enlistment.php?student_id=${studentId}&course_id=${courseId}"
            println("Requesting removal: $fullUrl")

            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            println("Response from server for removal: ${response.status} - $responseBodyString")

            val enlistmentResponse = Json.decodeFromString<EnlistmentResponse>(responseBodyString)

            if (enlistmentResponse.status == "success") {
                return true
            } else {
                context.toast("Failed to remove $courseId: ${enlistmentResponse.message}")
                return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Removal error for $courseId: ${e.localizedMessage ?: "Unknown"}")
            return false
        }
    }

    fun Context.toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Preview(showBackground = true)
    @Composable
    fun EnlistmentScreenPreview() {
        IT140P_SPARKTheme {
            EnlistmentActivity().apply { currentStudentId = "PREVIEW_S123" }.EnlistmentScreen()
        }
    }
}