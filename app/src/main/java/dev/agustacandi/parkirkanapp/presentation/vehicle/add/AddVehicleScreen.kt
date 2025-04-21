package dev.agustacandi.parkirkanapp.presentation.vehicle.add

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme
import dev.agustacandi.parkirkanapp.util.RequestCameraPermission
import dev.agustacandi.parkirkanapp.util.ext.compressAndCreateImageFile
import java.io.File

@Composable
fun AddVehicleScreen(
    viewModel: AddVehicleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onVehicleAdded: () -> Unit
) {
    val context = LocalContext.current
    val addVehicleState by viewModel.addVehicleState.collectAsState()

    var name by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // File untuk menyimpan foto kamera
    val photoFile = remember { File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // Minta izin kamera
    RequestCameraPermission { isGranted ->
        cameraPermissionGranted = isGranted
    }

    // Efek untuk menangani state
    LaunchedEffect(addVehicleState) {
        when (addVehicleState) {
            is AddVehicleState.Success -> {
                onVehicleAdded()
            }
            else -> {} // Tidak ada aksi khusus untuk state lain
        }
    }

    // Image picker dari galeri
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
        } else {
            Toast.makeText(context, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

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
                    // Set URI ke null terlebih dahulu untuk memicu recomposition
                    selectedImageUri = null
                    cameraLauncher.launch(photoUri)
                } else {
                    // Tampilkan toast atau dialog bahwa izin kamera diperlukan
                    Toast.makeText(context, "Izin kamera diperlukan untuk menggunakan fitur ini", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Content lainnya sama seperti sebelumnya
    AddVehicleContent(
        name = name,
        onNameChange = { name = it },
        licensePlate = licensePlate,
        onLicensePlateChange = { licensePlate = it },
        selectedImageUri = selectedImageUri,
        onSelectImage = { showImageSourceDialog = true },
        isLoading = addVehicleState is AddVehicleState.Loading,
        errorMessage = (addVehicleState as? AddVehicleState.Error)?.message,
        onNavigateBack = onNavigateBack,
        onSaveClick = {
            selectedImageUri?.let { uri ->
                // Konversi Uri ke File dengan kompresi
                val file = uri.compressAndCreateImageFile(context)
                file?.let {
                    viewModel.addVehicle(name, licensePlate, it)
                } ?: run {
                    // Tampilkan pesan error jika kompresi gagal
                    Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleContent(
    name: String,
    onNameChange: (String) -> Unit,
    licensePlate: String,
    onLicensePlateChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Kendaraan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
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
                            text = "Pilih Gambar Kendaraan",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Nama Kendaraan
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Kendaraan") },
                placeholder = { Text("Contoh: Honda Beat") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            // Nomor Plat
            OutlinedTextField(
                value = licensePlate,
                onValueChange = onLicensePlateChange,
                label = { Text("Nomor Plat") },
                placeholder = { Text("Contoh: B 1234 ABC") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )

            // Submit Button
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = name.isNotBlank() && licensePlate.isNotBlank() && selectedImageUri != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text("Simpan")
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
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Sumber Gambar") },
        text = { Text("Silahkan pilih sumber gambar yang ingin digunakan") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onGallerySelected,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Gallery",
                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                    )
                    Text("Galeri")
                }

                Button(
                    onClick = onCameraSelected,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Camera",
                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                    )
                    Text("Kamera")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddVehicleScreenPreview() {
    ParkirkanAppTheme {
        AddVehicleContent(
            name = "Honda Beat",
            onNameChange = {},
            licensePlate = "B 1234 ABC",
            onLicensePlateChange = {},
            selectedImageUri = null,
            onSelectImage = {},
            isLoading = false,
            errorMessage = null,
            onNavigateBack = {},
            onSaveClick = {}
        )
    }
}