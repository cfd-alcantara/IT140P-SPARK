package com.example.it140p_spark.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FinalizationEnlistedCourse(
    val CourseEnlistedID: Int,
    val CourseID: Int,
    val CourseName: String,
    val CourseCode: String,
    val CourseUnits: Int,
    val EnlistmentID: Int,
    val SectionID: Int?,
    val SectionCODE: String?,
    val Room: String?,
    val Capacity: Int?,
    val CurrentEnrolled: Int?,
    val InstructorFirstName: String?,
    val InstructorLastName: String?
)

@Serializable
data class StudentFinalizationDataResponse(
    val status: String,
    val courses: List<FinalizationEnlistedCourse>? = null,
    val term: String? = null,
    val message: String? = null
)

@Serializable
data class FinalizeEnrollmentResponse(
    val status: String,
    val message: String? = null,
    val enrollment_id: Int? = null
)
