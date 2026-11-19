package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AuthManager
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BunkCalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.VioletPrimary
import kotlinx.coroutines.launch

enum class AttendXDestination(
    val title: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    SUBJECTS("My Subjects", "Subjects", Icons.Rounded.Book, Icons.Outlined.Book),
    CALCULATOR("Bunk Calculator", "Bunk Calc", Icons.Rounded.Calculate, Icons.Outlined.Calculate),
    TIMETABLE("Timetable", "Timetable", Icons.Rounded.Schedule, Icons.Outlined.Schedule),
    CALENDAR("Attendance Calendar", "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    ANALYTICS("Analytics & Reports", "Analytics", Icons.Rounded.Analytics, Icons.Outlined.Analytics),
    SETTINGS("Settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendXApp(
    viewModel: AttendXViewModel,
    userEmail: String?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onExitToAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalAttendXColors.current
    val credentialManager = remember { CredentialManager.create(context) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val simulationDeltas by viewModel.simulationDeltas.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDest by rememberSaveable { mutableStateOf(AttendXDestination.DASHBOARD) }
    var calculatorSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = currentDest != AttendXDestination.DASHBOARD) {
        currentDest = AttendXDestination.DASHBOARD
    }

    val navigateTo: (AttendXDestination) -> Unit = { dest ->
        currentDest = dest
        showMoreSheet = false
    }

    val triggerGoogleSignIn: () -> Unit = {
        AuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { /* AuthStateListener switches to Firestore session automatically */ },
            onAuthError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            scope = scope
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.baseBg)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Persistent Sidebar Navigation on Tablet / Desktop
            if (isWideScreen) {
                Surface(
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight(),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = "AttendX",
                                    tint = TextPrimaryDark
                                )
                            }
                            Column {
                                Text(
                                    text = "AttendX",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Smart Bunk Calc",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accentCyan
                                )
                            }
                        }

                        HorizontalDivider(color = colors.borderSubtle)
                        Spacer(modifier = Modifier.height(4.dp))

                        AttendXDestination.entries.forEach { dest ->
                            val selected = currentDest == dest
                            NavigationDrawerItem(
                                selected = selected,
                                onClick = { navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    selectedIconColor = colors.accentCyan,
                                    selectedTextColor = colors.textPrimary,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("nav_sidebar_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Main Content Scaffold
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = colors.baseBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!isWideScreen) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.School,
                                            contentDescription = null,
                                            tint = TextPrimaryDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = currentDest.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            if (!isWideScreen) {
                                IconButton(
                                    onClick = { navigateTo(AttendXDestination.CALENDAR) },
                                    modifier = Modifier.testTag("topbar_calendar_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Attendance Calendar",
                                        tint = if (currentDest == AttendXDestination.CALENDAR) colors.accentCyan else colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { navigateTo(AttendXDestination.ANALYTICS) },
                                    modifier = Modifier.testTag("topbar_analytics_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Analytics,
                                        contentDescription = "Analytics",
                                        tint = if (currentDest == AttendXDestination.ANALYTICS) colors.accentCyan else colors.textSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onToggleDarkTheme(!isDarkTheme) },
                                modifier = Modifier.testTag("topbar_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Navy Mode",
                                    tint = colors.accentCyan
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.cardBg,
                            titleContentColor = colors.textPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = colors.cardBg,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            val primaryTabs = listOf(
                                AttendXDestination.DASHBOARD,
                                AttendXDestination.SUBJECTS,
                                AttendXDestination.CALCULATOR,
                                AttendXDestination.TIMETABLE
                            )
                            primaryTabs.forEach { dest ->
                                val selected = currentDest == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.shortLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.accentCyan,
                                        selectedTextColor = colors.textPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        unselectedIconColor = colors.textMuted,
                                        unselectedTextColor = colors.textMuted
                                    ),
                                    modifier = Modifier.testTag("nav_bottom_${dest.name.lowercase()}")
                                )
                            }
                            val inSecondary = currentDest in listOf(
                                AttendXDestination.CALENDAR,
                                AttendXDestination.ANALYTICS,
                                AttendXDestination.SETTINGS
                            )
                            NavigationBarItem(
                                selected = inSecondary,
                                onClick = { showMoreSheet = true },
                                icon = { Icon(Icons.Rounded.Apps, contentDescription = "More Pages") },
                                label = { Text("More") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.accentCyan,
                                    selectedTextColor = colors.textPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("nav_bottom_more")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                    ) {
                        when (val current = uiState) {
                            is UiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = VioletPrimary)
                                }
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Unable to load attendance data",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CriticalCoral
                                        )
                                        Text(
                                            text = current.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                        Button(onClick = onExitToAuth) {
                                            Text("Back to Sign-In")
                                        }
                                    }
                                }
                            }
                            is UiState.Success -> {
                                val state = current.data
                                when (currentDest) {
                                    AttendXDestination.DASHBOARD -> DashboardScreen(
                                        state = state,
                                        onNavigateToCalculator = { subId ->
                                            if (subId != null) calculatorSubjectId = subId
                                            navigateTo(AttendXDestination.CALCULATOR)
                                        },
                                        onNavigateToSubjects = { navigateTo(AttendXDestination.SUBJECTS) },
                                        onNavigateToTimetable = { navigateTo(AttendXDestination.TIMETABLE) },
                                        onNavigateToSettings = { navigateTo(AttendXDestination.SETTINGS) },
                                        onMarkAttendance = { subId, dateIso, pIdx, pCnt, st ->
                                            viewModel.markAttendance(subId, dateIso, pIdx, pCnt, st)
                                        }
                                    )
                                    AttendXDestination.SUBJECTS -> SubjectsScreen(
                                        state = state,
                                        onUpsertSubject = { id, name, code, cond, att, req, clr ->
                                            viewModel.upsertSubject(id, name, code, cond, att, req, clr)
                                        },
                                        onDeleteSubject = { viewModel.deleteSubject(it) },
                                        onOpenCalculatorForSubject = { subId ->
                                            calculatorSubjectId = subId
                                            navigateTo(AttendXDestination.CALCULATOR)
                                        }
                                    )
                                    AttendXDestination.CALCULATOR -> BunkCalculatorScreen(
                                        state = state,
                                        selectedSubjectId = calculatorSubjectId,
                                        simulationDeltas = simulationDeltas,
                                        onSelectSubject = { calculatorSubjectId = it },
                                        onUpdateSimulation = viewModel::updateSimulation,
                                        onSetAllSimulation = viewModel::setAllSimulationDeltas,
                                        onClearSimulation = viewModel::clearSimulation,
                                        onApplySimulation = viewModel::applySimulationToRecords
                                    )
                                    AttendXDestination.TIMETABLE -> TimetableScreen(
                                        state = state,
                                        onUpsertSlot = viewModel::upsertTimetableSlot,
                                        onDeleteSlot = viewModel::deleteTimetableSlot,
                                        onMarkAttendance = { subId, dateIso, pIdx, pCnt, st ->
                                            viewModel.markAttendance(subId, dateIso, pIdx, pCnt, st)
                                        },
                                        onToggleHoliday = viewModel::toggleHoliday
                                    )
                                    AttendXDestination.CALENDAR -> CalendarScreen(
                                        state = state,
                                        onMarkAttendance = { subId, dateIso, pIdx, pCnt, st ->
                                            viewModel.markAttendance(subId, dateIso, pIdx, pCnt, st)
                                        },
                                        onDeleteRecord = viewModel::deleteAttendanceRecord,
                                        onToggleHoliday = viewModel::toggleHoliday
                                    )
                                    AttendXDestination.ANALYTICS -> AnalyticsScreen(
                                        state = state,
                                        onMarkAttendance = { subId, dateIso, pIdx, pCnt, st ->
                                            viewModel.markAttendance(subId, dateIso, pIdx, pCnt, st)
                                        },
                                        onDeleteRecord = viewModel::deleteAttendanceRecord,
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                    AttendXDestination.SETTINGS -> SettingsScreen(
                                        state = state,
                                        userEmail = userEmail,
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = onToggleDarkTheme,
                                        onUpdateDefaultPercent = viewModel::updateDefaultRequiredPercent,
                                        onExportBackupJson = viewModel::exportBackupJson,
                                        onImportBackupJson = viewModel::importBackupJson,
                                        onResetDemoData = viewModel::resetToDemoData,
                                        onSignOutOrExitLocal = onExitToAuth,
                                        onGoogleSignInFromLocal = triggerGoogleSignIn,
                                        onShowMessage = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = colors.cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "More AttendX Pages",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                listOf(
                    AttendXDestination.CALENDAR,
                    AttendXDestination.ANALYTICS,
                    AttendXDestination.SETTINGS
                ).forEach { dest ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { navigateTo(dest) }
                            .testTag("more_sheet_${dest.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (currentDest == dest) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else colors.cardElevated,
                        border = BorderStroke(1.dp, colors.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = dest.selectedIcon,
                                contentDescription = dest.title,
                                tint = colors.accentCyan
                            )
                            Text(
                                text = dest.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
