package com.example.it140p_spark

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
    private var isCourseDropdownExpanded by mutableStateOf(false)
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

                ExposedDropdownMenuBox(
                    expanded = isCourseDropdownExpanded,
                    onExpandedChange = { isCourseDropdownExpanded = !isCourseDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedCourses.isEmpty()) "Select Courses" else "${selectedCourses.size} courses selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Add Courses") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCourseDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isCourseDropdownExpanded,
                        onDismissRequest = { isCourseDropdownExpanded = false }
                    ) {
                        if (coursesList.isEmpty() && searchCourseQuery.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Loading courses...") },
                                onClick = { /* Do nothing */ }
                            )
                        } else if (coursesList.isEmpty() && searchCourseQuery.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No courses found for '${searchCourseQuery}'") },
                                onClick = { /* Do nothing */ }
                            )
                        } else {
                            coursesList.forEach { course ->
                                if (!selectedCourses.contains(course)) {
                                    DropdownMenuItem(
                                        text = { Text("${course.courseCode} - ${course.courseName} (${course.courseUnits} units)") },
                                        onClick = {
                                            selectedCourses.add(course)
                                            isCourseDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedCourses.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Selected Courses:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(selectedCourses) { course ->
                                SelectedCourseItem(course = course) {
                                    selectedCourses.remove(course)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AddEnlistmentButton(httpClient)
            }
        }
    }

    @Composable
    fun SelectedCourseItem(course: Course, onRemove: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${course.courseCode} - ${course.courseName}", style = MaterialTheme.typography.bodySmall)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove course",
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onRemove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun AddEnlistmentButton(httpClient: HttpClient) {
        val mContext = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                coroutineScope.launch {
                    if (selectedCourses.isEmpty()) {
                        mContext.toast("Please select at least one course to enlist.")
                        return@launch
                    }

                    var allEnlistmentsSuccessful = true
                    for (course in selectedCourses) {
                        println("Enlisting: StudentID = ${currentStudentId}, CourseID = ${course.courseId}")
                        val success = KTORaddEnlistment(mContext, httpClient, currentStudentId!!, course.courseId)
                        if (!success) {
                            allEnlistmentsSuccessful = false
                        }
                    }

                    if (allEnlistmentsSuccessful) {
                        mContext.toast("Enlisted successfully!")
                        selectedCourses.clear()
                    } else {
                        mContext.toast("Some enlistments failed.")
                    }
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
                text = "Add Selected Enlistments",
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
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
                context.toast(enlistmentResponse.message)
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