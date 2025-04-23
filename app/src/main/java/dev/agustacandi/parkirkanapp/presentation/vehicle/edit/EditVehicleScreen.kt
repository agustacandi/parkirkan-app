package dev.agustacandi.parkirkanapp.presentation.vehicle.edit

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme
import dev.agustacandi.parkirkanapp.util.RequestCameraPermission
import dev.agustacandi.parkirkanapp.util.ext.checkHttps
import dev.agustacandi.parkirkanapp.util.ext.compressAndCreateImageFile
import java.io.File

@Composable
fun EditVehicleScreen(
    viewModel: EditVehicleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onVehicleUpdated: () -> Unit,
    onVehicleDeleted: () -> Unit
) {
    val context = LocalContext.current
    val editVehicleState by viewModel.editVehicleState.collectAsState()
    val vehicleData by viewModel.vehicle.collectAsState()

    var name by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalImageUrl by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // Update local state when vehicle data is loaded
    LaunchedEffect(vehicleData) {
        vehicleData?.let {
            name = it.name
            licensePlate = it.licensePlate
            originalImageUrl = it.image
        }
    }

    // File for camera photo
    val photoFile = remember {
        File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
    }
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // Request camera permission
    RequestCameraPermission { isGranted ->
        cameraPermissionGranted = isGranted
    }

    // Handle state changes
    LaunchedEffect(editVehicleState) {
        when (editVehicleState) {
            is EditVehicleState.Success -> {
                Toast.makeText(context, "Vehicle updated successfully", Toast.LENGTH_SHORT).show()
                onVehicleUpdated()
            }
            is EditVehicleState.DeleteSuccess -> {
                Toast.makeText(context, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show()
                onVehicleDeleted()
            }
            is EditVehicleState.Error -> {
                val errorMessage = (editVehicleState as EditVehicleState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
        } else {
            Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Image source dialog
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onGallerySelected = {
                showImageSourceDialog = false
                imagePickerLauncher.launch("image/*")
            },
            onCameraSelected = {
                showImageSourceDialog = false
                if (cameraPermissionGranted) {
                    selectedImageUri = null
                    cameraLauncher.launch(photoUri)
                } else {
                    Toast.makeText(context, "Camera permission required for this feature", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteVehicle()
            }
        )
    }

    // Main content
    EditVehicleContent(
        name = name,
        onNameChange = { name = it },
        licensePlate = licensePlate,
        onLicensePlateChange = { licensePlate = it },
        selectedImageUri = selectedImageUri,
        originalImageUrl = originalImageUrl,
        onSelectImage = { showImageSourceDialog = true },
        isLoading = editVehicleState is EditVehicleState.Loading || editVehicleState is EditVehicleState.Deleting,
        isDeleting = editVehicleState is EditVehicleState.Deleting,
        errorMessage = (editVehicleState as? EditVehicleState.Error)?.message,
        onNavigateBack = onNavigateBack,
        onSaveClick = {
            // Handle update with or without new image
            if (selectedImageUri != null) {
                val file = selectedImageUri?.compressAndCreateImageFile(context)
                file?.let {
                    viewModel.updateVehicle(name, licensePlate, it)
                } ?: run {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.updateVehicle(name, licensePlate, null)
            }
        },
        onDeleteClick = {
            showDeleteConfirmDialog = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVehicleContent(
    name: String,
    onNameChange: (String) -> Unit,
    licensePlate: String,
    onLicensePlateChange: (String) -> Unit,
    selectedImageUri: Uri?,
    originalImageUrl: String?,
    onSelectImage: () -> Unit,
    isLoading: Boolean,
    isDeleting: Boolean,
    errorMessage: String?,
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Vehicle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = !isLoading && !isDeleting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Vehicle",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onSelectImage),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    // Show selected image
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (originalImageUrl != null) {
                    // Show original image from server
                    AsyncImage(
                        model = originalImageUrl.checkHttps(),
                        contentDescription = "Current Vehicle Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Show placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Select Image",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to Change Vehicle Image",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Vehicle Name
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Vehicle Name") },
                placeholder = { Text("e.g., Honda Beat") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                enabled = !isLoading && !isDeleting
            )

            // License Plate
            OutlinedTextField(
                value = licensePlate,
                onValueChange = onLicensePlateChange,
                label = { Text("License Plate") },
                placeholder = { Text("e.g., B 1234 ABC") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                enabled = !isLoading && !isDeleting
            )

            // Save Button
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = name.isNotBlank() && licensePlate.isNotBlank() && !isLoading && !isDeleting
            ) {
                if (isLoading && !isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Saving...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }

            // Delete Button
            Button(
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                enabled = !isLoading && !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp
                    )
                    Text("Deleting...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Vehicle")
                }
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Vehicle") },
        text = {
            Text("Are you sure you want to delete this vehicle? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Image Source",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onGallerySelected,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Gallery",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Gallery")
                    }

                    Button(
                        onClick = onCameraSelected,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Camera")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditVehicleScreenPreview() {
    ParkirkanAppTheme {
        EditVehicleContent(
            name = "Honda Beat",
            onNameChange = {},
            licensePlate = "B 1234 ABC",
            onLicensePlateChange = {},
            selectedImageUri = null,
            originalImageUrl = "https://example.com/image.jpg",
            onSelectImage = {},
            isLoading = false,
            isDeleting = false,
            errorMessage = null,
            onNavigateBack = {},
            onSaveClick = {},
            onDeleteClick = {}
        )
    }
}