package dev.agustacandi.parkirkanapp.presentation.security.broadcast.edit

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
fun EditBroadcastScreen(
    viewModel: EditBroadcastViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onBroadcastUpdated: () -> Unit,
    onBroadcastDeleted: () -> Unit
) {
    val context = LocalContext.current
    val editBroadcastState by viewModel.editBroadcastState.collectAsState()
    val broadcastData by viewModel.broadcast.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalImageUrl by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // Update local state when broadcast data is loaded
    LaunchedEffect(broadcastData) {
        broadcastData?.let {
            title = it.title
            description = it.description
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
    LaunchedEffect(editBroadcastState) {
        when (editBroadcastState) {
            is EditBroadcastState.Success -> {
                Toast.makeText(context, "Broadcast berhasil diperbarui", Toast.LENGTH_SHORT).show()
                onBroadcastUpdated()
            }
            is EditBroadcastState.DeleteSuccess -> {
                Toast.makeText(context, "Broadcast berhasil dihapus", Toast.LENGTH_SHORT).show()
                onBroadcastDeleted()
            }
            is EditBroadcastState.Error -> {
                val errorMessage = (editBroadcastState as EditBroadcastState.Error).message
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
                viewModel.deleteBroadcast()
            }
        )
    }

    // Main content
    EditBroadcastContent(
        title = title,
        onTitleChange = { title = it },
        description = description,
        onDescriptionChange = { description = it },
        selectedImageUri = selectedImageUri,
        originalImageUrl = originalImageUrl,
        onSelectImage = { showImageSourceDialog = true },
        isLoading = editBroadcastState is EditBroadcastState.Loading || editBroadcastState is EditBroadcastState.Deleting,
        isDeleting = editBroadcastState is EditBroadcastState.Deleting,
        errorMessage = (editBroadcastState as? EditBroadcastState.Error)?.message,
        onNavigateBack = onNavigateBack,
        onSaveClick = {
            // Handle update with or without new image
            if (selectedImageUri != null) {
                val file = selectedImageUri?.compressAndCreateImageFile(context)
                file?.let {
                    viewModel.updateBroadcast(title, description, it)
                } ?: run {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.updateBroadcastWithoutImage(title, description)
            }
        },
        onDeleteClick = {
            showDeleteConfirmDialog = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBroadcastContent(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
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
                title = { Text("Edit Broadcast") },
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
                            contentDescription = "Delete Broadcast",
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
                        contentDescription = "Current Broadcast Image",
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
                            text = "Tap to Change Broadcast Image",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Broadcast Title
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Judul Broadcast") },
                placeholder = { Text("Contoh: Parkir Tutup Sementara") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                enabled = !isLoading && !isDeleting
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Deskripsi") },
                placeholder = { Text("Contoh: Area parkir akan ditutup untuk renovasi...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                singleLine = false,
                enabled = !isLoading && !isDeleting
            )

            // Save Button
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = title.isNotBlank() && description.isNotBlank() && !isLoading && !isDeleting
            ) {
                if (isLoading && !isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Menyimpan...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Perubahan")
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
                    Text("Menghapus...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Broadcast")
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
        title = { Text("Hapus Broadcast") },
        text = {
            Text("Apakah Anda yakin ingin menghapus broadcast ini? Tindakan ini tidak dapat dibatalkan.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
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
                    text = "Pilih Sumber Gambar",
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
                        Text("Galeri")
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
                        Text("Kamera")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal")
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditBroadcastScreenPreview() {
    ParkirkanAppTheme {
        EditBroadcastContent(
            title = "Parkir Tutup Sementara",
            onTitleChange = {},
            description = "Area parkir akan ditutup untuk renovasi mulai tanggal 10 Mei 2025.",
            onDescriptionChange = {},
            selectedImageUri = null,
            originalImageUrl = null,
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