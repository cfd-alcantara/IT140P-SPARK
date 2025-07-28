package com.example.it140p_spark.presentation

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import com.example.it140p_spark.data.models.GradeRecord
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.it140p_spark.data.functions.RecordFunctions.fetchGrades
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

@Composable
fun RecordsScreen(
    studentId: String,
    padding: PaddingValues
) {
    var grades by remember { mutableStateOf<List<GradeRecord>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // TODO: Move this HttpClient to a singleton or MainScreen and pass it down
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

    LaunchedEffect(Unit) {
        fetchGrades(httpClient, studentId) {
            grades = it
            isLoading = false
        }
    }

    Box(modifier = Modifier
        .padding(padding)
        .fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            grades == null -> {
                Text("Failed to load grades", modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 🔷 Hardcoded header for School Year and Term
                    Text(
                        text = "School Year: 2024–2025",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Term: 3rd Term",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (grades!!.isEmpty()) {
                        Text("No grades available")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(grades!!) { grade ->
                                GradeItem(grade)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeItem(grade: GradeRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Course: ${grade.CourseCode}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Grade: ${grade.Grade ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}