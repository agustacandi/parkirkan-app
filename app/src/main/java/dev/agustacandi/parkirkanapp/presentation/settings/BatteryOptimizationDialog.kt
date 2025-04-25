package dev.agustacandi.parkirkanapp.presentation.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.util.BatteryOptimizationChecker

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enable Reliable Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.alert_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "For reliable notifications with custom sounds and vibrations, you need to disable battery optimization for this app.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                BatteryOptimizationChecker.getBatteryOptimizationSettingsIntent(context)
                            )
                        } catch (e: Exception) {
                            // Fallback if the intent fails
                            val fallbackIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Battery Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                BatteryOptimizationChecker.getNotificationSettingsIntent(context)
                            )
                        } catch (e: Exception) {
                            // Fallback if the intent fails
                            val fallbackIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Notification Settings")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Later")
                }
            }
        }
    }
}