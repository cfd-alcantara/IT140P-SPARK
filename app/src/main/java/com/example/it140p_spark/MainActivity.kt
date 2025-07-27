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
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedNavItem = navItems[selectedIndex]
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // MoreScreen navigation state
    var moreScreenState by remember { mutableStateOf<MoreSubScreen>(MoreSubScreen.Main) }

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
                padding = padding
            )
        }
    }
}
