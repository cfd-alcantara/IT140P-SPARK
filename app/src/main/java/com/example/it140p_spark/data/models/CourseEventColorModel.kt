package com.example.it140p_spark.data.models

import androidx.compose.ui.graphics.Color

object CourseEventColors {
    val containerColors = listOf(
        Color(0xFFF8BBD0), // Material 100 Pink
        Color(0xFFBBDEFB), // Material 100 Blue
        Color(0xFFFFE0B2), // Material 100 Orange
        Color(0xFFB2DFDB), // Material 100 Teal
        Color(0xFFB3E5FC), // Material 100 Light Blue
        Color(0xFFFFECB3), // Material 100 Amber
        Color(0xFFFFF9C4), // Material 100 Yellow
        Color(0xFFDCEDC8), // Material 100 Light Green
    )
    val onContainerColors = listOf(
        Color(0xFF880E4F), // Material 900 Pink
        Color(0xFF0D47A1), // Material 900 Blue
        Color(0xFFE65100), // Material 900 Orange
        Color(0xFF004D40), // Material 900 Teal
        Color(0xFF01579B), // Material 900 Light Blue
        Color(0xFFFF6F00), // Material 900 Amber
        Color(0xFFF57F17), // Material 900 Light Yellow
        Color(0xFF33691E), // Material 900 Green
    )
    fun getContainerColor(index: Int): Color = containerColors[index % containerColors.size]
    fun getOnContainerColor(index: Int): Color = onContainerColors[index % onContainerColors.size]
}
