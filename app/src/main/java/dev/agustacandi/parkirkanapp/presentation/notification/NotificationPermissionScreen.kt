package dev.agustacandi.parkirkanapp.presentation.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.util.BatteryOptimizationChecker
import dev.agustacandi.parkirkanapp.util.PreferenceManager

@Composable
fun NotificationPermissionScreen(
    onContinue: (hasPermission: Boolean) -> Unit,
    preferenceManager: PreferenceManager
) {
    val context = LocalContext.current
    
    // Check if we already have notification permission for API 33+
    val hasNotificationPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // On older Android versions, permission is granted at install time
        }
    }
    
    // Check for battery optimization status
    val isBatteryOptimized = remember {
        !BatteryOptimizationChecker.isIgnoringBatteryOptimizations(context)
    }
    
    var permissionRequested by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequested = true
        // Do not continue automatically - let user see all options
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Image(
                painter = painterResource(id = R.drawable.ic_splash),
                contentDescription = "App Icon",
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Welcome title
            Text(
                text = "Welcome to Parkirkan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Set up required permissions for the best experience",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Permissions card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Notifications section
                    PermissionSection(
                        title = "Notifications",
                        description = "Enable notifications to receive important alerts about your vehicle",
                        isEnabled = hasNotificationPermission,
                        buttonText = if (hasNotificationPermission) "Enabled" else "Enable",
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Open notification settings
                                try {
                                    context.startActivity(
                                        BatteryOptimizationChecker.getNotificationSettingsIntent(context)
                                    )
                                } catch (e: Exception) {
                                    // Fallback
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore if we can't open settings
                                    }
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Battery optimization section
                    PermissionSection(
                        title = "Battery Optimization",
                        description = "Disable battery optimization for reliable notifications",
                        isEnabled = !isBatteryOptimized,
                        buttonText = if (!isBatteryOptimized) "Disabled" else "Disable",
                        onClick = {
                            try {
                                context.startActivity(
                                    BatteryOptimizationChecker.getBatteryOptimizationSettingsIntent(context)
                                )
                            } catch (e: Exception) {
                                // Fallback
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignore if we can't open settings
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Continue button
            Button(
                onClick = { 
                    onContinue(hasNotificationPermission)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionSection(
    title: String,
    description: String,
    isEnabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon - use different icons depending on permission type
            Icon(
                painter = painterResource(
                    id = if (title.contains("Notifications")) {
                        R.drawable.ic_notifications
                    } else {
                        R.drawable.alert_illustration // Use battery icon if available
                    }
                ),
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Button
            OutlinedButton(
                onClick = onClick,
                shape = MaterialTheme.shapes.small
            ) {
                Text(text = buttonText)
            }
        }
        
        if (title != "Battery Optimization") {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
} 