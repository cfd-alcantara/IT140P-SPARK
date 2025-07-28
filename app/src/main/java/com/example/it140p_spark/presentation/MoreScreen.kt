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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.it140p_spark.R
import com.example.it140p_spark.LoginActivity
import com.example.it140p_spark.data.utils.ThemeMode
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.it140p_spark.data.utils.ColorMode
import com.example.it140p_spark.ui.components.ListItem
import com.example.it140p_spark.ui.theme.LightColorScheme
import com.example.it140p_spark.ui.theme.DarkColorScheme

private data class MoreItem(val label: String, val icon: ImageVector)
private data class AboutItem(val main: String, val sub: String)

private val moreItems = listOf(
    MoreItem("Appearance", Icons.Outlined.ColorLens),
    MoreItem("About", Icons.Outlined.Info),
    MoreItem("Help", Icons.AutoMirrored.Outlined.HelpOutline),
    MoreItem("Sign out", Icons.AutoMirrored.Outlined.ExitToApp)
)

private val aboutItems = listOf(
    AboutItem("About", "SPARK (Student Portal for Academic Resources and Knowledge) is a mobile student portal app based on OneMCL. It is designed by MMCL students and is made for MMCL students."),
    AboutItem("Version", "1.0.0"),
    AboutItem("Developers", "Carl Francis Alcantara, Jan Gabriel Rea, Julian Peter Gerona, Luis Gerard Tiongco"),
    AboutItem("Disclaimer", "This app is made in partial fulfillment of IT140P. All resources and data are for educational purposes only.")
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
    onThemeChange: (ThemeMode) -> Unit,
    onColorModeChange: (ColorMode) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
) {
    val themeOptions = listOf("System", "Light", "Dark")
    val selectedTheme = when (themeMode) {
        ThemeMode.SYSTEM -> 0
        ThemeMode.LIGHT -> 1
        ThemeMode.DARK -> 2
    }
    val colorOptions = listOf(ColorMode.DEFAULT, ColorMode.DYNAMIC)
    // Determine selected color based on dynamicColor
    val selectedColor = if (dynamicColor) ColorMode.DYNAMIC else ColorMode.DEFAULT
    LazyColumn(
        modifier = modifier.padding(padding).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Theme Mode",
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
        item {
            Text(
                text = "Theme Color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = colorOptions,
                    key = { it.name }
                ) { colorModeOption ->
                    Column(
                        modifier = Modifier
                            .width(114.dp)
                            .padding(top = 8.dp)
                    ) {
                        AppColorThemePreviewItem(
                            selected = selectedColor == colorModeOption,
                            onClick = {
                                onColorModeChange(colorModeOption)
                                onDynamicColorChange(colorModeOption == ColorMode.DYNAMIC)
                            },
                            colorMode = colorModeOption,
                            themeMode = themeMode
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = colorModeOption.display,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AppColorThemePreviewItem(
    selected: Boolean,
    onClick: () -> Unit,
    colorMode: ColorMode = ColorMode.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val context = LocalContext.current
    val dynamicLightColorScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.compose.material3.dynamicLightColorScheme(context)
    } else {
        MaterialTheme.colorScheme
    }
    val dynamicDarkColorScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.compose.material3.dynamicDarkColorScheme(context)
    } else {
        MaterialTheme.colorScheme
    }
    val colorScheme = when (colorMode) {
        ColorMode.DEFAULT -> when (themeMode) {
            ThemeMode.LIGHT -> LightColorScheme
            ThemeMode.DARK -> DarkColorScheme
            ThemeMode.SYSTEM -> if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        }
        ColorMode.DYNAMIC -> when (themeMode) {
            ThemeMode.LIGHT -> dynamicLightColorScheme
            ThemeMode.DARK -> dynamicDarkColorScheme
            ThemeMode.SYSTEM -> if (androidx.compose.foundation.isSystemInDarkTheme()) dynamicDarkColorScheme else dynamicLightColorScheme
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .border(
                width = 4.dp,
                color = if (selected) {
                    colorScheme.primary
                } else {
                    DividerDefaults.color
                },
                shape = RoundedCornerShape(17.dp),
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colorScheme.background)
            .clickable(onClick = onClick),
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .weight(0.7f)
                    .padding(end = 4.dp)
                    .background(
                        color = colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                    ),
            )

            Box(
                modifier = Modifier.weight(0.3f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected Color Theme",
                        tint = colorScheme.primary,
                    )
                }
            }
        }

        // Cover
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .background(
                    color = DividerDefaults.color,
                    shape = MaterialTheme.shapes.small,
                )
                .fillMaxWidth(0.5f)
        ) {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .size(width = 24.dp, height = 16.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(colorScheme.tertiary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(colorScheme.secondary),
                )
            }
        }

        // Bottom bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                color = colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .background(
                                color = colorScheme.primary,
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .alpha(0.6f)
                            .height(17.dp)
                            .weight(1f)
                            .background(
                                color = colorScheme.onSurface,
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues()) {
    LazyColumn(
        modifier = modifier.padding(padding),
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
            ListItem(
                main = main,
                sub = sub,
                onClick = {},
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun MainScreen(onNavigate: (MoreSubScreen) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
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
            ListItem(
                main = item.label,
                sub = when (item.label) {
                    "Appearance" -> "Theme mode and color"
                    "Help" -> "Go to MMCL website"
                    else -> null
                },
                icon = item.icon,
                onClick = {
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
            )
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
    onThemeChange: (ThemeMode) -> Unit,
    onColorModeChange: (ColorMode) -> Unit = {},
    dynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
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
                onThemeChange = onThemeChange,
                onColorModeChange = onColorModeChange,
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange
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
