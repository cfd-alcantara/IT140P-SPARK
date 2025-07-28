package com.example.it140p_spark.data.functions

import android.content.Context
import com.example.it140p_spark.data.utils.SERVER_URL
import com.example.it140p_spark.data.models.EnlistedCourse
import com.example.it140p_spark.data.models.CourseSectionSearchResponse
import com.example.it140p_spark.data.models.EnrollmentResponse
import com.example.it140p_spark.data.models.GroupedSection
import com.example.it140p_spark.data.models.ScheduleEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

object SectioningFunctions {
    suspend fun fetchCourseSectionsSuspend(context: Context, httpClient: HttpClient, studentID: String): List<EnlistedCourse> {
        return try {
            val fullUrl = "${SERVER_URL}get_courseEnlisted_Schedule.php?StudentID=${studentID}"
            val response = httpClient.get(fullUrl)
            val responseBodyString = response.bodyAsText()
            if (response.status.value == 200) {
                val parsedResponse = Json.decodeFromString<CourseSectionSearchResponse>(responseBodyString)
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

    suspend fun ktorInsertSection(context: Context, httpClient: HttpClient, enlistmentId: String, courseId: String, sectionId: String): Boolean {
        return try {
            val fullUrl = "${SERVER_URL}insert_section.php?EnlistmentID=$enlistmentId&CourseID=$courseId&SectionID=$sectionId"
            val response = httpClient.get(fullUrl)
            val body = response.bodyAsText()
            val result = Json.decodeFromString<EnrollmentResponse>(body)
            result.status == "success"
        } catch (e: Exception) {
            false
        }
    }

    fun suggestValidSchedule(sections: List<EnlistedCourse>): List<GroupedSection>? {
        val grouped = sections.groupBy { it.courseId + (it.sectionCode ?: "") }
        val groupedSections = grouped.map { (_, entries) ->
            val first = entries.first()
            GroupedSection(
                courseId = first.courseId,
                courseName = first.courseName,
                courseCode = first.courseCode,
                sectionId = first.sectionId ?: "",
                sectionCode = first.sectionCode ?: "",
                courseUnits = first.courseUnits,
                schedules = entries.mapNotNull { enlistedCourse ->
                    if (enlistedCourse.day != null && enlistedCourse.startTime != null && enlistedCourse.endTime != null) {
                        ScheduleEntry(enlistedCourse.day, enlistedCourse.startTime, enlistedCourse.endTime)
                    } else {
                        null
                    }
                }
            )
        }.groupBy { it.courseId }
        fun hasConflict(existing: List<ScheduleEntry>, new: List<ScheduleEntry>): Boolean {
            for (e in existing) {
                for (n in new) {
                    if (e.day == n.day && !(e.endTime <= n.startTime || n.endTime <= e.startTime)) {
                        return true
                    }
                }
            }
            return false
        }
        fun backtrack(
            groupedList: List<List<GroupedSection>>,
            index: Int,
            currentSchedule: MutableList<GroupedSection>,
            accumulatedSchedules: MutableList<ScheduleEntry>
        ): List<GroupedSection>? {
            if (index == groupedList.size) return currentSchedule.toList()
            for (section in groupedList[index]) {
                if (!hasConflict(accumulatedSchedules, section.schedules)) {
                    currentSchedule.add(section)
                    accumulatedSchedules.addAll(section.schedules)
                    val result = backtrack(groupedList, index + 1, currentSchedule, accumulatedSchedules)
                    if (result != null) return result
                    currentSchedule.removeAt(currentSchedule.size - 1)
                    accumulatedSchedules.removeAll(section.schedules)
                }
            }
            return null
        }
        return backtrack(groupedSections.values.toList(), 0, mutableListOf(), mutableListOf())
    }
}