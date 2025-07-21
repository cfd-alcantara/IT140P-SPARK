package com.example.it140p_spark.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.it140p_spark.ui.components.TimeTable
import com.example.it140p_spark.ui.components.TimeTable_Sectioning

/* todo: display the finalized list of courses in this Screen
*        else, will display a message that "you are not enrolled" */
@Composable
fun ScheduleScreen(padding: PaddingValues) {
    Box(modifier = Modifier.padding(padding)) {
        TimeTable()
    }
}