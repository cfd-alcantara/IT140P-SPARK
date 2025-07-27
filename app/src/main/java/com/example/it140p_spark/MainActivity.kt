package com.example.it140p_spark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.it140p_spark.ui.theme.IT140P_SPARKTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import com.example.it140p_spark.ui.components.AppBar
import com.example.it140p_spark.ui.components.NavBar
import com.example.it140p_spark.ui.components.navItems
import com.example.it140p_spark.ui.layout.ScaffoldLayout
import com.example.it140p_spark.presentation.BillingScreen
import com.example.it140p_spark.presentation.EnrollmentScreen
import com.example.it140p_spark.presentation.RecordsScreen
import com.example.it140p_spark.presentation.ScheduleScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.it140p_spark.presentation.MoreScreen
import com.example.it140p_spark.presentation.MoreSubScreen
import com.example.it140p_spark.data.utils.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.it140p_spark.data.utils.ThemePreferenceManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val studentId = intent.getStringExtra("STUDENT_ID")
        val finalStudentId = studentId ?: "UNKNOWN_STUDENT"
        enableEdgeToEdge()
        setContent {
            IT140P_SPARKTheme {
                App(finalStudentId)
            }
        }
    }
}

/* todo: save current screen state when phone is rotated to landscape/portrait */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(studentId: String) {
    val context = LocalContext.current
    val themeFlow = remember { ThemePreferenceManager.themeModeFlow(context) }
    val persistedTheme by themeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    var themeMode by remember { mutableStateOf(persistedTheme) }

    // Observe ColorMode from DataStore
    val colorModeFlow = remember { ThemePreferenceManager.colorModeFlow(context) }
    val persistedColorMode by colorModeFlow.collectAsState(initial = com.example.it140p_spark.data.utils.ColorMode.DEFAULT)
    var colorMode by remember { mutableStateOf(persistedColorMode) }

    // Add dynamicColor state (sync with colorMode)
    var dynamicColor by remember { mutableStateOf(colorMode == com.example.it140p_spark.data.utils.ColorMode.DYNAMIC) }

    // Save theme to DataStore when changed, but only if it is different from persistedTheme
    LaunchedEffect(themeMode) {
        if (themeMode != persistedTheme) {
            ThemePreferenceManager.setThemeMode(context, themeMode)
        }
    }

    // Save colorMode to DataStore when changed
    LaunchedEffect(colorMode) {
        if (colorMode != persistedColorMode) {
            ThemePreferenceManager.setColorMode(context, colorMode)
        }
        dynamicColor = colorMode == com.example.it140p_spark.data.utils.ColorMode.DYNAMIC
    }

    // Update themeMode when persistedTheme changes (e.g., after app restart)
    LaunchedEffect(persistedTheme) {
        if (themeMode != persistedTheme) {
            themeMode = persistedTheme
        }
    }

    // Update colorMode when persistedColorMode changes
    LaunchedEffect(persistedColorMode) {
        if (colorMode != persistedColorMode) {
            colorMode = persistedColorMode
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedNavItem = navItems[selectedIndex]
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // MoreScreen navigation state
    var moreScreenState by remember { mutableStateOf<MoreSubScreen>(MoreSubScreen.Main) }

    IT140P_SPARKTheme(
        darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        dynamicColor = dynamicColor
    ) {
        ScaffoldLayout(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                when {
                    selectedNavItem.route == "more" && moreScreenState == MoreSubScreen.Appearance ->
                        AppBar(route = "appearance", scrollBehavior = scrollBehavior, onBack = { moreScreenState = MoreSubScreen.Main })
                    selectedNavItem.route == "more" && moreScreenState == MoreSubScreen.About ->
                        AppBar(route = "about", scrollBehavior = scrollBehavior, onBack = { moreScreenState = MoreSubScreen.Main })
                    selectedNavItem.route != "more" ->
                        AppBar(route = selectedNavItem.route, scrollBehavior = scrollBehavior)
                }
            },
            bottomBar = {
                NavBar(
                    selectedIndex = selectedIndex,
                    onItemSelected = {
                        selectedIndex = it
                        if (navItems[it].route == "more") {
                            moreScreenState = MoreSubScreen.Main
                        }
                    }
                )
            },
        ) { padding ->
            when (selectedNavItem.route) {
                "schedule" -> ScheduleScreen(studentId, padding)
                "enroll" -> EnrollmentScreen(studentId, padding)
                "billing" -> BillingScreen(padding)
                "records" -> RecordsScreen(padding)
                "more" -> MoreScreen(
                    currentScreen = moreScreenState,
                    onNavigate = { moreScreenState = it },
                    padding = padding,
                    themeMode = themeMode,
                    onThemeChange = { themeMode = it },
                    colorMode = colorMode,
                    onColorModeChange = { colorMode = it },
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = { dynamicColor = it; colorMode = if (it) com.example.it140p_spark.data.utils.ColorMode.DYNAMIC else com.example.it140p_spark.data.utils.ColorMode.DEFAULT }
                )
            }
        }
    }
}
