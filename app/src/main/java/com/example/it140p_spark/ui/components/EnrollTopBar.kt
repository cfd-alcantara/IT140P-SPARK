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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollTopBar(
    scrollBehavior: TopAppBarScrollBehavior
) {
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            if (isSearching) {
                LaunchedEffect(Unit) {
                    searchFocus.requestFocus()
                }
                SearchField(
                    searchQuery = searchQuery,
                    onChangeSearchQuery = { searchQuery = it },
                    onSearch = { /* todo: this is where the search logic comes in */ },
                    modifier = Modifier.focusRequester(searchFocus)
                )
            } else {
                Text(text = "Enroll")
            }
        },
        navigationIcon = {
            if (isSearching) {
                IconButton(onClick = {
                    isSearching = false
                    searchQuery = ""
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                }
            }
        },
        actions = {
            if (!isSearching) {
                IconButton(onClick = { isSearching = true }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Open Search")
                }
            } else {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear Search")
                    }
                }
            }
            IconButton(onClick = { /* do something*/ }) {
                Icon(imageVector = Icons.Filled.FilterList, contentDescription = "Open Filter Drawer")
            }
        }
    )
}