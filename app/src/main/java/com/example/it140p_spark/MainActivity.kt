package com.example.it140p_spark

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme

class MainActivity : ComponentActivity() {

    private var receivedUsername: String? by mutableStateOf(null)
    private var receivedStudentId: String? by mutableStateOf(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        receivedUsername = intent.getStringExtra("USERNAME")
        receivedStudentId = intent.getStringExtra("STUDENT_ID")

        if (receivedUsername != null) {
            Toast.makeText(this, "Welcome, ${receivedUsername}!", Toast.LENGTH_SHORT).show()
        }

        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainMenu(
                        username = receivedUsername,
                        studentId = receivedStudentId
                    )
                }
            }
        }
    }

    @Composable
    fun MainMenu(username: String?, studentId: String?) {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Student Management System",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (username != null) {
                Text(
                    text = "Logged in as: $username",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (studentId != null) {
                Text(
                    text = "Student ID: $studentId",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else {
                Text(
                    text = "Student ID: Not Available",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (studentId != null) {
                        val intent = Intent(context, EnlistmentActivity::class.java).apply {
                            putExtra("STUDENT_ID", studentId)
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Student ID is missing. Cannot proceed to Enlistment.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Enlistment", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Section functionality!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Section Management", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Enroll functionality!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Enrollment", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainMenuPreview() {
        IT140P_SPARKTheme {
            MainMenu(username = "PreviewUser", studentId = "S123")
        }
    }
}