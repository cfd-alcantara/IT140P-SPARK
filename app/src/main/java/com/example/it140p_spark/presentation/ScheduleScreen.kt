package com.example.it140p_spark.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.it140p_spark.ui.components.StudentScheduleTimetable
import io.ktor.client.HttpClient

/* todo: display the finalized list of courses in this Screen
*        else, will display a message that "you are not enrolled" */
@Composable
fun ScheduleScreen(studentId: String, padding: PaddingValues) {
    Box(modifier = Modifier.padding(padding)) {
        StudentScheduleTimetable(
            context = LocalContext.current,
            studentID = studentId, // Use dynamic studentId
            httpClient = HttpClient() // Reuse or inject your client
        )
    }
}