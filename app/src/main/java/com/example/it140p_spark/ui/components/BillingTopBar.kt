package com.example.it140p_spark.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingTopBar(
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(text = "Billing") },
        navigationIcon = {
            IconButton(onClick = { /* do something*/ }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Open Menu")
            }
        },
        actions = {
            IconButton(onClick = { /* do something*/ }) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Open Likes")
            }
            IconButton(onClick = { /* do something*/ }) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Open Settings")
            }
        }
    )
}
