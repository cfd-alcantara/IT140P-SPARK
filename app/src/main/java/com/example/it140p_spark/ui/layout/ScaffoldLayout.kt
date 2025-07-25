package com.example.it140p_spark.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScaffoldLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {}
) {
    Scaffold (
        modifier = modifier,
        topBar = { topBar() },
        bottomBar = { bottomBar() },
        content = { innerPadding ->
            content(innerPadding)
        }
    )
}
