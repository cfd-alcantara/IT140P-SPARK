package com.example.it140p_spark.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class EnlistedCourse(
    @SerialName("CourseID") val courseId: String,
    @SerialName("CourseName") val courseName: String,
    @SerialName("CourseCode") val courseCode: String,
    @SerialName("SectionID") val sectionId: String?,
    @SerialName("SectionCode") val sectionCode: String?,
    @SerialName("CourseUnits") val courseUnits: String,
    @SerialName("Day") val day: String?,
    @SerialName("StartTime") val startTime: String?,
    @SerialName("EndTime") val endTime: String?
)

data class GroupedSection(
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val sectionId: String,
    val sectionCode: String,
    val courseUnits: String,
    val schedules: List<ScheduleEntry>
)

data class ScheduleEntry(
    val day: String,
    val startTime: String,
    val endTime: String
)

@Serializable
data class CourseSectionSearchResponse(
    val status: String,
    val message: String? = null,
    val data: List<EnlistedCourse>? = null
)
