package com.example.it140p_spark.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class EnrollmentRecord(
    @SerialName("enrollmentId") val enrollmentId: Int,
    @SerialName("enrollmentDate") val enrollmentDate: String,
    @SerialName("term") val term: String,
    @SerialName("paymentType") val paymentType: String,
    @SerialName("status") val status: String,
    @SerialName("cost") val cost: Float
)

@Serializable
data class GetEnrollmentsResponse(
    val status: String,
    val message: String? = null,
    val data: List<EnrollmentRecord>? = null
)

// ... other data classes like EnrollmentRecord ...

@Serializable
data class UpdateStatusResponse(
    val status: String,
    val message: String
)