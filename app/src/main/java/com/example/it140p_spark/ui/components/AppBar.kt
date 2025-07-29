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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    route: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null
) {
    when (route) {
        "schedule" -> {
            var showNotificationDialog by remember { mutableStateOf(false) }
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Schedule") },
                actions = {
                    IconButton(onClick = { showNotificationDialog = true }) {
                        Icon(imageVector = Icons.Filled.Notifications, contentDescription = "Open Notifications")
                    }
                }
            )
            if (showNotificationDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationDialog = false },
                    title = { Text("Allow schedule notifications") },
                    text = { Text("By accepting, the app will notify you 15 minutes before your schedule is about to start.") },
                    confirmButton = {
                        TextButton(onClick = { showNotificationDialog = false }) {
                            Text("Accept")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        "enroll" -> {
            var isSearching by rememberSaveable { mutableStateOf(false) }
            var searchQuery by rememberSaveable { mutableStateOf("") }
            val searchFocus = remember { FocusRequester() }

            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
//                    if (isSearching) {
//                        LaunchedEffect(Unit) {
//                            searchFocus.requestFocus()
//                        }
//                        SearchField(
//                            searchQuery = searchQuery,
//                            onChangeSearchQuery = { searchQuery = it },
//                            onSearch = { /* todo: this is where the search logic comes in */ },
//                            modifier = Modifier.focusRequester(searchFocus)
//                        )
//                    } else {
//                        Text(text = "Enroll")
//                    }
                    Text(text = "Enroll")
                },
                navigationIcon = {
//                    if (isSearching) {
//                        IconButton(onClick = {
//                            isSearching = false
//                            searchQuery = ""
//                        }) {
//                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
//                        }
//                    }
                },
                actions = {
//                    if (!isSearching) {
//                        IconButton(onClick = { isSearching = true }) {
//                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Open Search")
//                        }
//                    } else {
//                        if (searchQuery.isNotEmpty()) {
//                            IconButton(onClick = { searchQuery = "" }) {
//                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear Search")
//                            }
//                        }
//                    }
                }
            )
        }
        "billing" -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Billing") }
            )
        }
        "records" -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Records") }
            )
        }
        "appearance" -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Appearance") },
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
        "about" -> {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = "About") },
                navigationIcon = {
                    IconButton(onClick = { onBack?.invoke() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    }
}
