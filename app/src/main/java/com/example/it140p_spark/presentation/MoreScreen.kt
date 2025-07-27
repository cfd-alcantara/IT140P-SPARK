package com.example.it140p_spark.presentation

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.it140p_spark.R
import com.example.it140p_spark.LoginActivity
import com.example.it140p_spark.data.utils.ThemeMode
import androidx.core.net.toUri

private data class MoreItem(val label: String, val icon: ImageVector)

private val moreItems = listOf(
    MoreItem("Appearance", Icons.Outlined.ColorLens),
    MoreItem("About", Icons.Outlined.Info),
    MoreItem("Help", Icons.AutoMirrored.Outlined.HelpOutline),
    MoreItem("Sign out", Icons.AutoMirrored.Outlined.ExitToApp)
)

sealed class MoreSubScreen {
    data object Main : MoreSubScreen()
    data object Appearance : MoreSubScreen()
    data object About : MoreSubScreen()
}

@Composable
fun AppearanceScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val themeOptions = listOf("System", "Light", "Dark")
    val selectedTheme = when (themeMode) {
        ThemeMode.SYSTEM -> 0
        ThemeMode.LIGHT -> 1
        ThemeMode.DARK -> 2
    }
    LazyColumn(
        modifier = modifier.padding(padding).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { idx, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = themeOptions.size),
                        onClick = {
                            onThemeChange(
                                when (idx) {
                                    0 -> ThemeMode.SYSTEM
                                    1 -> ThemeMode.LIGHT
                                    2 -> ThemeMode.DARK
                                    else -> ThemeMode.SYSTEM
                                }
                            )
                        },
                        selected = idx == selectedTheme,
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues()) {
    val aboutItems = listOf(
        "About" to "SPARK (Student Portal for Academic Resources and Knowledge) is a mobile student portal app based on OneMCL. It is designed by MMCL students and is made for MMCL students.",
        "Version" to "1.0.0",
        "Developers" to "Carl Francis Alcantara, Jan Gabriel Rea, Julian Peter Gerona, Luis Gerard Tiongco",
        "Disclaimer" to "This app is made in partial fulfillment of IT140P. All resources and data are for educational purposes only.",
    )
    LazyColumn(
        modifier = modifier.padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.basic_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(144.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "MMCL Spark",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item {
            HorizontalDivider()
        }
        items(aboutItems) { (main, sub) ->
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(main, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MainScreen(onNavigate: (MoreSubScreen) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.basic_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(144.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "MMCL Spark",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item {
            HorizontalDivider()
        }
        items(moreItems) { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        when (item.label) {
                            "Appearance" -> onNavigate(MoreSubScreen.Appearance)
                            "About" -> onNavigate(MoreSubScreen.About)
                            "Help" -> {
                                val intent = Intent(Intent.ACTION_VIEW, "https://mmcl.edu.ph".toUri())
                                context.startActivity(intent)
                            }
                            "Sign out" -> {
                                signOutUser(context)
                            }
                        }
                    }
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(item.label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun MoreScreen(
    currentScreen: MoreSubScreen,
    onNavigate: (MoreSubScreen) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    when (currentScreen) {
        MoreSubScreen.Main -> {
            MainScreen(onNavigate = onNavigate, modifier = modifier)
        }
        MoreSubScreen.Appearance -> {
            AppearanceScreen(
                modifier = modifier,
                padding = padding,
                themeMode = themeMode,
                onThemeChange = onThemeChange
            )
        }
        MoreSubScreen.About -> {
            AboutScreen(modifier = modifier, padding = padding)
        }
    }
}

fun signOutUser(context: android.content.Context) {
    val intent = Intent(context, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    context.startActivity(intent)
}