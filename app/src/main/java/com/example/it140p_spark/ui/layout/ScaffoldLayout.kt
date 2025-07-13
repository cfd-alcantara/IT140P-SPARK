package com.example.it140p_spark.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@Composable
fun ScaffoldLayout(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {}
) {
    Scaffold (
        topBar = { topBar() },
        bottomBar = { bottomBar() },
        content = { innerPadding ->
            content(innerPadding)
        }
    )
}
