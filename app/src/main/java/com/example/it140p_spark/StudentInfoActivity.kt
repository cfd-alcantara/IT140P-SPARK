package com.example.it140p_spark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.text.KeyboardOptions

@Serializable
data class StudentInfo(
    val StudentFirstName: String,
    val StudentLastName: String,
    val StudentCourse: String,
    val StudentYear: Int,
    val StudentSem: String
)

@Serializable
data class AddStudentResponse(
    val status: String,
    val message: String? = null,
    val StudentID: Int? = null
)

class StudentInfoActivity : ComponentActivity() {

    private val SERVER_URL = "http://192.168.10.1/student_management_system/REST/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                StudentInfoScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudentInfoScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var studentCourse by remember { mutableStateOf("") }
        var studentYear by remember { mutableStateOf("") }
        var studentSem by remember { mutableStateOf("") }

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
                    text = "Student Information",
                    fontSize = 28.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = studentCourse,
                    onValueChange = { studentCourse = it },
                    label = { Text("Course") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = studentYear,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            studentYear = newValue
                        }
                    },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = studentSem,
                    onValueChange = { studentSem = it },
                    label = { Text("Semester") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val yearInt = studentYear.toIntOrNull()
                            if (firstName.isBlank() || lastName.isBlank() || yearInt == null) {
                                context.toast("Please fill in all required fields (First Name, Last Name, Year).")
                                return@launch
                            }

                            val studentData = StudentInfo(
                                StudentFirstName = firstName,
                                StudentLastName = lastName,
                                StudentCourse = studentCourse,
                                StudentYear = yearInt,
                                StudentSem = studentSem
                            )
                            val response = addStudentInfo(context, httpClient, studentData)

                            if (response?.status == "success" && response.StudentID != null) {
                                context.toast("Student added! ID: ${response.StudentID}. Proceeding to Enlistment.")
                                val intent = Intent(context, EnlistmentActivity::class.java).apply {
                                    putExtra("STUDENT_ID", response.StudentID.toString())
                                }
                                context.startActivity(intent)
                                (context as? ComponentActivity)?.finish()
                            } else {
                                context.toast("Failed to add student: ${response?.message ?: "Unknown error"}")
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
                        text = "Save Student Info & Enlist",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    private suspend fun addStudentInfo(context: Context, httpClient: HttpClient, student: StudentInfo): AddStudentResponse? {
        return try {
            val fullUrl = "${SERVER_URL}add_studentinfo.php"
            println("Sending student info to: $fullUrl")

            val response: HttpResponse = httpClient.post(fullUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "StudentFirstName" to student.StudentFirstName,
                        "StudentLastName" to student.StudentLastName,
                        "StudentCourse" to student.StudentCourse,
                        "StudentYear" to student.StudentYear.toString(),
                        "StudentSem" to student.StudentSem
                    ).formUrlEncode()
                )
            }

            val responseBodyString = response.bodyAsText()
            println("Raw JSON response for add student: $responseBodyString")

            if (response.status.value == 200) {
                Json.decodeFromString<AddStudentResponse>(responseBodyString)
            } else {
                context.toast("HTTP Error adding student: ${response.status.value} - ${response.status.description}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error adding student: ${e.localizedMessage}")
            null
        }
    }

    fun Context.toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Preview(showBackground = true)
    @Composable
    fun StudentInfoScreenPreview() {
        IT140P_SPARKTheme {
            StudentInfoScreen()
        }
    }
}