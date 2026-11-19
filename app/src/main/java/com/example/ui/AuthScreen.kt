package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthManager
import com.example.ui.theme.BlueElectric
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.CriticalCoral
import com.example.ui.theme.CriticalCoralBg
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.LocalAttendXColors
import com.example.ui.theme.NavyBase
import com.example.ui.theme.TextPrimaryDark

@Composable
fun AuthScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAuthSuccess: () -> Unit,
    onContinueLocalMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val colors = LocalAttendXColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.baseBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("auth_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Navy Mode",
                        tint = colors.accentCyan
                    )
                }
            }

            // Hero artwork card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_hero),
                    contentDescription = "AttendX analytics illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, NavyBase.copy(alpha = 0.88f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "AttendX Logo",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AttendX",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Attendance & Bunk Calculator",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanGlow
                        )
                    }
                }
            }

            // Feature highlights
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Rounded.Calculate,
                        color = colors.accentViolet,
                        title = "Exact Subject-Wise Bunk Math",
                        subtitle = "Know precisely how many classes you can miss or must attend."
                    )
                    FeatureRow(
                        icon = Icons.Rounded.AutoGraph,
                        color = colors.accentCyan,
                        title = "Future Attendance Simulator",
                        subtitle = "Preview missing tomorrow, a whole week, or consecutive classes."
                    )
                    FeatureRow(
                        icon = Icons.Rounded.CloudSync,
                        color = BlueElectric,
                        title = "Private Cloud Sync & Backups",
                        subtitle = "Your subjects, timetable & logs stay synced securely to your account."
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CriticalCoralBg,
                    border = BorderStroke(1.dp, CriticalCoral.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = CriticalCoral,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary Google Sign-In button
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    AuthManager.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextPrimaryDark
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandGradient, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextPrimaryDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Local Device-Only fallback option
            OutlinedButton(
                onClick = onContinueLocalMode,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("local_mode_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.cardElevated,
                    contentColor = colors.textSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Continue in Local Device Mode",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "Local Device Mode stores your attendance strictly on this device without cloud sync. Sign in with Google anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String
) {
    val colors = LocalAttendXColors.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}
