package com.example.it140p_spark.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GradeRecord(
    val CourseCode: String,
    val Grade: Int?
)