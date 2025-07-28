package com.example.it140p_spark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.it140p_spark.data.utils.SERVER_URL
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

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Login()
                }
            }
        }
    }
}

@Composable
fun Login() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var usernameSupportingText by remember { mutableStateOf("*enter your MCL live email address") }
    var passwordSupportingText by remember { mutableStateOf("*enter your password") }

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
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MMCL Spark", style = MaterialTheme.typography.headlineLarge)
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (usernameError) {
                        usernameError = false
                        usernameSupportingText = "*enter your MCL live email address"
                    }
                },
                label = { Text("Email*") },
                isError = usernameError,
                supportingText = {
                    Text(
                        usernameSupportingText,
                        color = if (usernameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    if (usernameError) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() }
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError) {
                        passwordError = false
                        passwordSupportingText = "*enter your password"
                    }
                },
                label = { Text("Password*") },
                isError = passwordError,
                supportingText = {
                    Text(
                        passwordSupportingText,
                        color = if (passwordError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    if (passwordError) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val hasError = validateAndSetErrors(
                            username,
                            password,
                            onUsernameError = { msg ->
                                usernameError = true
                                usernameSupportingText = msg
                            },
                            onPasswordError = {
                                passwordError = true
                                passwordSupportingText = "Password is required"
                            }
                        )
                        if (!hasError) {
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                performLoginAttempt(
                                    httpClient,
                                    username,
                                    password,
                                    context
                                )
                            }
                        }
                    }
                )
            )
            Button(
                onClick = {
                    val hasError = validateAndSetErrors(
                        username,
                        password,
                        onUsernameError = { msg ->
                            usernameError = true
                            usernameSupportingText = msg
                        },
                        onPasswordError = {
                            passwordError = true
                            passwordSupportingText = "Password is required"
                        }
                    )
                    if (!hasError) {
                        coroutineScope.launch {
                            performLoginAttempt(
                                httpClient,
                                username,
                                password,
                                context
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In", Modifier.padding(vertical = 8.dp))
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
        showToast(context, "Please enter both username and password")
        return
    }

    val (status, studentIdOrMessage) = loginUser(httpClient, username, passwordPlain)

    when (status) {
        "success" -> {
            showToast(context, "Login successful")
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("USERNAME", username)
                putExtra("STUDENT_ID", studentIdOrMessage)
            }
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
        }
        "error" -> {
            showToast(context, studentIdOrMessage ?: "Login failed")
        }
        else -> {
            showToast(context, "An unexpected error occurred")
        }
    }
}

private suspend fun loginUser(
    httpClient: HttpClient,
    username: String,
    passwordPlain: String
): Pair<String, String?> {
    return try {
        val fullUrl = "${SERVER_URL}student_login_json.php"

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

fun showToast(context: Context, message: CharSequence) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

// Add helper function for validation
private fun validateAndSetErrors(
    username: String,
    password: String,
    onUsernameError: (String) -> Unit,
    onPasswordError: () -> Unit
): Boolean {
    var hasError = false
    if (username.isBlank()) {
        onUsernameError("Email is required")
        hasError = true
    } else if (!username.endsWith("@live.mcl.edu.ph")) {
        onUsernameError("Email must be a valid MCL live account")
        hasError = true
    }
    if (password.isBlank()) {
        onPasswordError()
        hasError = true
    }
    return hasError
}
