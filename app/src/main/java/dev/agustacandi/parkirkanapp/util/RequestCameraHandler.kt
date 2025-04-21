package dev.agustacandi.parkirkanapp.util

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun RequestCameraPermission(onPermissionResult: (Boolean) -> Unit = {}) {
    var askForPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    LaunchedEffect(askForPermission) {
        if (askForPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            askForPermission = false
        }
    }

    // Fungsi ini memungkinkan kita untuk memicu permintaan izin
    LaunchedEffect(Unit) {
        askForPermission = true
    }
}