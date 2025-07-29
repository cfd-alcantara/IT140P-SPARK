package com.example.it140p_spark.data.functions

import com.example.it140p_spark.data.utils.SERVER_URL
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.example.it140p_spark.data.models.GradeRecord
import io.ktor.client.request.parameter
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


object RecordFunctions {
    suspend fun fetchGrades(
        httpClient: HttpClient,
        studentId: String,
        onResult: (List<GradeRecord>?) -> Unit
    ) {
        try {
            val response = httpClient.get("${SERVER_URL}get_grades.php") {
                parameter("StudentID", studentId)
            }

            val responseBody = response.bodyAsText()
            val json = Json.parseToJsonElement(responseBody).jsonObject
            val status = json["status"]?.jsonPrimitive?.content

            if (status == "success") {
                val data = json["data"]?.jsonArray ?: return onResult(null)
                val grades = data.map {
                    val obj = it.jsonObject
                    GradeRecord(
                        CourseCode = obj["CourseCode"]?.jsonPrimitive?.content ?: "",
                        Grade = obj["Grade"]?.jsonPrimitive?.intOrNull
                    )
                }
                onResult(grades)
            } else {
                onResult(null)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }
}