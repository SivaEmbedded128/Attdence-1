package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthManager
import com.example.data.AuthManager.authStateFlow
import com.example.data.FirestoreAttendanceRepository
import com.example.data.LocalAttendanceRepository
import com.example.ui.AttendXApp
import com.example.ui.AttendXViewModel
import com.example.ui.AuthScreen
import com.example.ui.theme.AttendXTheme
import com.example.ui.theme.ThemePreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val isDarkTheme by ThemePreferences.observeDarkMode(context)
                .collectAsStateWithLifecycle(initialValue = true)

            AttendXTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { enabled ->
                        scope.launch { ThemePreferences.setDarkMode(context, enabled) }
                    }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var useLocalDeviceMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null && AuthManager.isFirebaseConfigured(context)) {
            val credentialManager = CredentialManager.create(context)
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { useLocalDeviceMode = false },
                onUnauthenticated = { /* Stay on AuthScreen until user signs in or chooses local mode */ },
                scope = scope
            )
        }
    }

    val user = currentUser
    when {
        user != null -> {
            val uid = user.uid
            key(uid) {
                val vm: AttendXViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            val repo = FirestoreAttendanceRepository(db = db, currentUserId = uid)
                            AttendXViewModel(
                                repository = repo,
                                displayName = user.displayName ?: "Student",
                                email = user.email ?: ""
                            )
                        }
                    }
                )
                AttendXApp(
                    viewModel = vm,
                    userEmail = user.email,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onExitToAuth = { useLocalDeviceMode = false }
                )
            }
        }
        useLocalDeviceMode -> {
            key("local_device_mode") {
                val vm: AttendXViewModel = viewModel(
                    key = "local_device_mode",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val repo = LocalAttendanceRepository(app)
                            AttendXViewModel(
                                repository = repo,
                                displayName = "Local Student",
                                email = ""
                            )
                        }
                    }
                )
                AttendXApp(
                    viewModel = vm,
                    userEmail = null,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onExitToAuth = { useLocalDeviceMode = false }
                )
            }
        }
        else -> {
            AuthScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { onToggleDarkTheme(!isDarkTheme) },
                onAuthSuccess = { useLocalDeviceMode = false },
                onContinueLocalMode = { useLocalDeviceMode = true }
            )
        }
    }
}
