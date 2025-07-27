package com.example.it140p_spark.data.functions

import android.content.Context
import com.example.it140p_spark.data.SERVER_URL
import com.example.it140p_spark.data.models.FinalizeEnrollmentResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.Parameters
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FinalizeFunctions {
    suspend fun finalizeEnrollment(
        context: Context,
        httpClient: HttpClient,
        studentId: String,
        enlistmentId: String,
        paymentType: String,
        term: String,
        enrollmentStatus: String
    ): FinalizeEnrollmentResponse {
        val enrollmentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val response = httpClient.post("${SERVER_URL}add_finalization.php") {
            contentType(io.ktor.http.ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("student_id", studentId)
                        append("enlistment_id", enlistmentId)
                        append("payment_type", paymentType)
                        append("term", term)
                        append("enrollment_date", enrollmentDate)
                        append("status", enrollmentStatus)
                    }
                )
            )
        }
        val responseBody = response.bodyAsText()
        return Json.decodeFromString(responseBody)
    }
}
