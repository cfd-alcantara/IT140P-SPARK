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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class StudentLoginActivity : ComponentActivity() {

    private val serverURL = "http://192.168.137.1/student_management_system/REST/"
    private val loginPHPScript = "student_login_json.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                StudentLoginScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudentLoginScreen() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

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
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Student Login",
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (username.isBlank() || password.isBlank()) {
                                context.toast("Please enter both username and password.")
                                return@launch
                            }

                            val (status, studentId) = loginUser(httpClient, username, password)

                            when (status) {
                                "success" -> {
                                    context.toast("Login successful!")
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        putExtra("USERNAME", username)
                                        putExtra("STUDENT_ID", studentId)
                                    }
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                }
                                "error" -> {
                                    context.toast(studentId ?: "Login failed. Please try again.")
                                }
                                else -> {
                                    context.toast("An unexpected error occurred.")
                                }
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
                        text = "Login",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    private suspend fun loginUser(
        httpClient: HttpClient,
        username: String,
        passwordPlain: String
    ): Pair<String, String?> {
        return try {
            val fullUrl = "${serverURL}$loginPHPScript"
            println("Attempting login to: $fullUrl")

            val response: HttpResponse = httpClient.post(fullUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "StudentUsername" to username,
                        "StudentPassword" to passwordPlain
                    ).formUrlEncode()
                )
            }

            val responseBodyString = response.bodyAsText()
            println("Raw PHP login response: $responseBodyString")

            val jsonResponse = Json.parseToJsonElement(responseBodyString).jsonObject
            val status = jsonResponse["status"]?.jsonPrimitive?.content ?: "error"
            val message = jsonResponse["message"]?.jsonPrimitive?.content ?: "Unknown error"
            val studentId = jsonResponse["student_id"]?.jsonPrimitive?.content

            if (status == "success") {
                Pair(status, studentId)
            } else {
                Pair(status, message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("error", "An unexpected error occurred.")
        }
    }

    fun Context.toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Preview(showBackground = true)
    @Composable
    fun StudentLoginScreenPreview() {
        IT140P_SPARKTheme {
            StudentLoginScreen()
        }
    }
}