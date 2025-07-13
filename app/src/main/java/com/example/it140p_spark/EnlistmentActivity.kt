package com.example.it140p_spark

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName

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

class EnlistmentActivity : ComponentActivity() {

    private val SERVER_URL = "http://192.168.10.1/student_management_system/REST/"

    private var currentStudentId: String by mutableStateOf("")

    private var selectedCourseId: String by mutableStateOf("")
    private var selectedCourseName: String by mutableStateOf("Select Course")
    private var coursesList: List<Course> by mutableStateOf(emptyList())
    private var isCourseDropdownExpanded by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentStudentId = intent.getStringExtra("STUDENT_ID") ?: ""
        if (currentStudentId.isBlank()) {
            Toast.makeText(this, "Error: Student ID not provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                EnlistmentScreen()
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

        androidx.compose.runtime.LaunchedEffect(Unit) {
            coroutineScope.launch {
                fetchCourses(context, httpClient, "")
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
                    text = "Enlisting for Student ID: $currentStudentId",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = isCourseDropdownExpanded,
                    onExpandedChange = { isCourseDropdownExpanded = !isCourseDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCourseName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCourseDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isCourseDropdownExpanded,
                        onDismissRequest = { isCourseDropdownExpanded = false }
                    ) {
                        if (coursesList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Loading courses... or no courses available.") },
                                onClick = { /* Do nothing */ }
                            )
                        } else {
                            coursesList.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.courseName} (${course.courseCode}) - ${course.courseUnits} units") },
                                    onClick = {
                                        selectedCourseName = "${course.courseName} (${course.courseCode})"
                                        selectedCourseId = course.courseId
                                        isCourseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                AddEnlistmentButton(httpClient)
            }
        }
    }

    @Composable
    fun AddEnlistmentButton(httpClient: HttpClient) {
        val mContext = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                coroutineScope.launch {
                    KTORaddEnlistment(mContext, httpClient, currentStudentId, selectedCourseId)
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
                text = "Add Enlistment Record",
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
                    if (coursesList.isNotEmpty()) {
                        if (selectedCourseId.isBlank()) {
                            selectedCourseName = "${coursesList.first().courseName} (${coursesList.first().courseCode})"
                            selectedCourseId = coursesList.first().courseId
                        }
                    } else {
                        context.toast("No courses available to display.")
                        selectedCourseName = "No Courses Available"
                        selectedCourseId = ""
                    }
                } else {
                    context.toast("Server reported error: ${parsedResponse.message ?: "Unknown error"}")
                }
            } else {
                context.toast("HTTP Error fetching courses: ${response.status.value} - ${response.status.description}")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error fetching courses: ${e.localizedMessage}")
            coursesList = emptyList()
            selectedCourseName = "Error Loading Courses"
            selectedCourseId = ""
        }
    }

    private suspend fun KTORaddEnlistment(context: Context, httpClient: HttpClient, studentId: String, courseId: String) {
        try {
            if (studentId.isBlank()) {
                context.toast("Student ID is missing. Cannot enlist.")
                return
            }
            if (courseId.isBlank() || courseId == "Select Course") {
                context.toast("Please select a Course.")
                return
            }

            val fullUrl = "${SERVER_URL}add_enlistment.php?student_id=${studentId}&course_id=${courseId}"
            println("Sending enlistment request to: $fullUrl")

            val response: HttpResponse = httpClient.get(fullUrl)
            val stringBody: String = response.bodyAsText()
            println("Server Response: ${response.status} - $stringBody")
            context.toast("Add Enlistment: ${response.status.value} - $stringBody")

        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error adding enlistment: ${e.localizedMessage}")
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