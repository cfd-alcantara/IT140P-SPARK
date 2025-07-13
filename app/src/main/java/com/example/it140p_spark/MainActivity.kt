package com.example.it140p_spark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*

data class Course(val subjName: String, val day: String, val section: String, val time: String)

data class CoursesTaken(val id: Int, val course: Course)

// ✅ Your course data
val courses = listOf(
    Course("CS198L", "Monday", "CIS341", "7AM - 8:15AM"),
    Course("IT190-2P", "Monday", "CIS341", "7AM - 8:15AM"),
    Course("IT190-2P", "Tuesday", "CIS342", "12:00PM - 1:15PM"),
    Course("IT140P", "Monday", "CIS341", "8:15AM - 9:30AM"),
    Course("IT190-3P", "Tuesday", "CIS341", "12:00PM - 1:15PM"),
    Course("IT190-3P", "Monday", "CIS342", "7AM - 8:15AM"),
    Course("IT190-3P", "Wednesday", "CIS343", "7AM - 8:15AM")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                ScheduleListScreen()
            }
        }
    }
}

fun generateSchedule(courses: List<Course>): List<CoursesTaken> {
    val schedule = mutableListOf<CoursesTaken>()
    var classNumber = 0
    for (course in courses) {
        schedule.add(CoursesTaken(classNumber++, course))
    }
    return schedule
}

@Preview(showBackground = true)
@Composable
fun ScheduleListScreen() {
    val schedule = remember { generateSchedule(courses) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(schedule) { coursesTaken ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Class #${coursesTaken.id}")
                    Text(text = "Subject: ${coursesTaken.course.subjName}")
                    Text(text = "Day: ${coursesTaken.course.day}")
                    Text(text = "Room: ${coursesTaken.course.section}")
                    Text(text = "Time: ${coursesTaken.course.time}")
                }
            }
        }
    }
}

//@Preview(showBackground = true, widthDp = 411, heightDp = 891)

// class Population

// class GeneticAlgorithm

// class Course (To schedule)

// class instructor

// class room

// class MeetingTime

// class Department

// class Class