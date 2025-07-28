package com.example.it140p_spark.data.functions

import android.content.Context
import com.example.it140p_spark.data.utils.SERVER_URL
import com.example.it140p_spark.data.models.Course
import com.example.it140p_spark.data.models.CourseEnlistedSearchResponse
import com.example.it140p_spark.data.models.CourseSearchResponse
import com.example.it140p_spark.data.models.EnrollmentResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object EnlistmentFunctions {
    suspend fun processConfirmAction(
        context: Context,
        httpClient: HttpClient,
        studentId: String,
        enlistmentId: String?,
        initialEnrolledCourses: List<Course>,
        finalSelectedCourses: List<Course>,
        toast: (String) -> Unit
    ) {
        val coursesToAdd = finalSelectedCourses.filter { it !in initialEnrolledCourses }
        val coursesToRemove = initialEnrolledCourses.filter { it !in finalSelectedCourses }
        var anyFailure = false
        var currentEnlistmentId = enlistmentId
        for (course in coursesToAdd) {
            val (success, newId) = ktorAddEnrollment(
                context = context,
                httpClient = httpClient,
                studentId = studentId,
                courseId = course.courseId,
                currentEnlistmentId = currentEnlistmentId
            )
            if (success) {
                if (currentEnlistmentId == null && newId != null) {
                    currentEnlistmentId = newId
                }
                toast("Added ${course.courseCode}")
            } else {
                toast("Failed to add ${course.courseCode}")
                anyFailure = true
            }
        }
        for (course in coursesToRemove) {
            val success = ktorRemoveEnrollment(
                context = context,
                httpClient = httpClient,
                studentId = studentId,
                courseId = course.courseId
            )
            if (success) {
                toast("Removed ${course.courseCode}")
            } else {
                toast("Failed to remove ${course.courseCode}")
                anyFailure = true
            }
        }
        if (!anyFailure) {
            toast("All changes confirmed successfully!")
        } else {
            toast("Some changes failed to apply. Please check the console for details.")
        }
    }

    suspend fun fetchEnlistmentId(
        context: Context,
        httpClient: HttpClient,
        studentId: String,
        onFetched: (String?) -> Unit
    ) {
        try {
            val response = httpClient.get("${SERVER_URL}get_enlistmentID.php?student_id=$studentId")
            val body = response.bodyAsText()
            val json = Json.parseToJsonElement(body).jsonObject
            val status = json["status"]?.jsonPrimitive?.contentOrNull
            if (status == "success") {
                val enlistmentId = json["enlistment_id"]?.jsonPrimitive?.contentOrNull
                onFetched(enlistmentId)
            } else {
                onFetched(null)
            }
        } catch (e: Exception) {
            onFetched(null)
        }
    }

    suspend fun fetchCoursesSuspend(context: Context, httpClient: HttpClient, query: String): List<Course> {
        return try {
            val fullUrl = "${SERVER_URL}search_courseinfo.php?query=${query}"
            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            if (response.status.value == 200) {
                val parsedResponse = Json.decodeFromString<CourseSearchResponse>(responseBodyString)
                if (parsedResponse.status == "success" && parsedResponse.data != null) {
                    parsedResponse.data
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCourseEnlistedSuspend(context: Context, httpClient: HttpClient, studentID: String): List<Course> {
        return try {
            val fullUrl = "${SERVER_URL}get_courseEnlisted.php?StudentID=${studentID}"
            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            if (response.status.value == 200) {
                val parsedResponse = Json.decodeFromString<CourseEnlistedSearchResponse>(responseBodyString)
                if (parsedResponse.status == "success" && parsedResponse.data != null) {
                    parsedResponse.data
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun ktorAddEnrollment(context: Context, httpClient: HttpClient, studentId: String, courseId: String, currentEnlistmentId: String?): Pair<Boolean, String?> {
        return try {
            val urlBuilder = StringBuilder("${SERVER_URL}add_enlistment.php?student_id=$studentId&course_id=$courseId")
            if (currentEnlistmentId != null) {
                urlBuilder.append("&enlistment_id=$currentEnlistmentId")
            }
            val fullUrl = urlBuilder.toString()
            val response = httpClient.get(fullUrl)
            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText).jsonObject
            val success = json["status"]?.jsonPrimitive?.content == "success"
            val newEnlistmentId = json["enlistment_id"]?.jsonPrimitive?.content
            Pair(success, newEnlistmentId)
        } catch (e: Exception) {
            Pair(false, null)
        }
    }

    suspend fun ktorRemoveEnrollment(context: Context, httpClient: HttpClient, studentId: String, courseId: String): Boolean {
        return try {
            val fullUrl = "${SERVER_URL}remove_enlistment.php?student_id=${studentId}&course_id=${courseId}"
            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            val enrollmentResponse = Json.decodeFromString<EnrollmentResponse>(responseBodyString)
            enrollmentResponse.status == "success"
        } catch (e: Exception) {
            false
        }
    }
}