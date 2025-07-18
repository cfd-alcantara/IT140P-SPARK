package com.example.it140p_spark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val studentId = intent.getStringExtra("STUDENT_ID")
        val finalStudentId = studentId ?: "UNKNOWN_STUDENT"

        setContent {
            IT140P_SPARKTheme {
                App(
                    studentId = finalStudentId 
                )
            }
        }
    }
}