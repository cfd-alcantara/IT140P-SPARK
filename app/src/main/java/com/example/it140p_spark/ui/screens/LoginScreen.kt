package com.example.it140p_spark.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.it140p_spark.MainActivity
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.formUrlEncode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LoginScreen : ComponentActivity() {

    private val serverURL = "http://192.168.56.1/student_management_system/REST/"
    private val loginPHPScript = "student_login_json.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IT140P_SPARKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    this.LoginScreenContent()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreenContent() {
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
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Login", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            performLoginAttempt(
                                httpClient,
                                username,
                                password,
                                context
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
        }
    }

    private suspend fun performLoginAttempt(
        httpClient: HttpClient,
        username: String,
        passwordPlain: String,
        context: Context
    ) {
        if (username.isBlank() || passwordPlain.isBlank()) {
            context.toast("Please enter both username and password")
            return
        }

        val (status, studentIdOrMessage) = loginUser(httpClient, username, passwordPlain)

        when (status) {
            "success" -> {
                context.toast("Login successful")
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("USERNAME", username)
                    putExtra("STUDENT_ID", studentIdOrMessage)
                }
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
            }
            "error" -> {
                context.toast(studentIdOrMessage ?: "Login failed")
            }
            else -> {
                context.toast("An unexpected error occurred")
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

            val jsonResponse = Json.parseToJsonElement(responseBodyString).jsonObject
            val status = jsonResponse["status"]?.jsonPrimitive?.content ?: "error"
            val message = jsonResponse["message"]?.jsonPrimitive?.content ?: "Unknown error"
            val studentId = jsonResponse["student_id"]?.jsonPrimitive?.content

            if (status == "success") {
                Pair(status, studentId)
            } else {
                Pair(status, message)
            }
        } catch (_: Exception) {
            Pair("error", "An unexpected error occurred")
        }
    }

    fun Context.toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}