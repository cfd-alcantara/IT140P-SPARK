package com.example.it140p_spark.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.example.it140p_spark.data.models.EnrollmentRecord
import com.example.it140p_spark.data.models.GetEnrollmentsResponse
import com.example.it140p_spark.data.utils.SERVER_URL
import io.ktor.client.call.body
import com.example.it140p_spark.data.models.UpdateStatusResponse
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters

@Composable
fun BillingScreen(studentId: String, padding: PaddingValues) { // Added studentId parameter
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State to hold the list of enrollment records
    var enrollmentRecords by remember { mutableStateOf<List<EnrollmentRecord>>(emptyList()) }
    // State to manage loading status
    var isLoading by remember { mutableStateOf(true) }
    // State to hold any error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // State to hold the currently selected enrollment record
    var selectedEnrollmentRecord by remember { mutableStateOf<EnrollmentRecord?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }


    // Initialize Ktor HTTP client
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

    // Dispose of the HTTP client when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            httpClient.close()
        }
    }

    // LaunchedEffect to fetch data when the composable enters the composition
    // Now depends on studentId to re-fetch if it changes (though typically it won't for this screen)
    LaunchedEffect(studentId, refreshTrigger) {
        isLoading = true
        errorMessage = null
        try {
            // Make the GET request to your PHP script, including studentId
            val response: GetEnrollmentsResponse = httpClient.get("${SERVER_URL}get_enrollments.php?student_id=$studentId").body()

            if (response.status == "success") {
                enrollmentRecords = response.data ?: emptyList()
                if (enrollmentRecords.isEmpty()) {
                    errorMessage = "No enrollment records found."
                }
            } else {
                errorMessage = response.message ?: "Unknown error fetching enrollments."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Network error: ${e.localizedMessage ?: "Unknown error"}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateEnrollmentStatus(enrollmentId: Int, newStatus: String): UpdateStatusResponse {
        return try {
            httpClient.submitForm(
                url = "${SERVER_URL}update_enrollment_status.php",
                formParameters = Parameters.build {
                    append("enrollment_id", enrollmentId.toString())
                    append("new_status", newStatus)
                }
            ).body<UpdateStatusResponse>()
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateStatusResponse(status = "error", message = "Network error during update: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun updateStatusResponse(enrollmentId: Int, newStatus: String): UpdateStatusResponse {
        return try {
            httpClient.submitForm(
                url = "${SERVER_URL}update_enrollment_status.php",
                formParameters = Parameters.build {
                    append("enrollment_id", enrollmentId.toString())
                    append("new_status", newStatus)
                }
            ).body<UpdateStatusResponse>()
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateStatusResponse(status = "error", message = "Network error during update: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enrollment Billing Details",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Loading enrollment data...", color = MaterialTheme.colorScheme.onBackground)
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                if (enrollmentRecords.isEmpty()) {
                    Text(
                        text = "No enrollment records to display.",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f), // Make LazyColumn fill remaining height
                        verticalArrangement = Arrangement.spacedBy(8.dp), // Space between cards
                        contentPadding = PaddingValues(vertical = 8.dp) // Padding at the bottom of the list
                    ) {
                        items(enrollmentRecords) { record ->
                            EnrollmentRecordCard( // Reverted to EnrollmentRecordCard
                                record = record,
                                isSelected = (record == selectedEnrollmentRecord),
                                onCardClick = { clickedRecord ->
                                    selectedEnrollmentRecord = if (selectedEnrollmentRecord == clickedRecord) {
                                        null // Deselect if already selected
                                    } else {
                                        clickedRecord // Select new record
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Space between list and button

                    // New Button
                    Button(
                        onClick = {
                            selectedEnrollmentRecord?.let { recordToUpdate ->
                                coroutineScope.launch {
                                    val newStatus = "Paid" // The status to update to
                                    if (recordToUpdate.status != newStatus) { // Only update if not already "Paid"
                                        val response = updateEnrollmentStatus(recordToUpdate.enrollmentId, newStatus)
                                        if (response.status == "success") {
                                            context.toast("Enrollment ${recordToUpdate.paymentType} (${recordToUpdate.enrollmentDate}) successfully marked as Paid.")
                                            refreshTrigger++ // Increment to trigger LaunchedEffect and refresh data
                                        } else {
                                            context.toast("Failed to update ${recordToUpdate.paymentType} (${recordToUpdate.enrollmentDate}): ${response.message}")
                                        }
                                    } else {
                                        context.toast("${recordToUpdate.paymentType} (${recordToUpdate.enrollmentDate}) is already Paid.")
                                    }
                                }
                            }
                        },
                        enabled = selectedEnrollmentRecord != null, // Enable only if a record is selected
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            text = if (selectedEnrollmentRecord != null) "Process Selected Enrollment" else "Select an Enrollment to Process",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable to display a single enrollment record as a Card.
 *
 * @param record The EnrollmentRecord to display.
 * @param isSelected True if this card is currently selected.
 * @param onCardClick Lambda to be invoked when the card is clicked.
 */
@Composable
fun EnrollmentRecordCard(record: EnrollmentRecord, isSelected: Boolean, onCardClick: (EnrollmentRecord) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable { onCardClick(record) } // Make the card clickable
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Enrollment Date: ${record.enrollmentDate}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Term: ${record.term}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Payment Type: ${record.paymentType}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${record.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cost: ₱${String.format("%.2f", record.cost)}", // Format cost to 2 decimal places
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
