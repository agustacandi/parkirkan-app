package dev.agustacandi.parkirkanapp.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch

data class Kendaraan(val id: Int, val nama: String, val nomorPlat: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToBroadcast: () -> Unit) {
    // State untuk menampilkan atau menyembunyikan bottom sheet
    var showBottomSheet by remember { mutableStateOf(false) }

    // State untuk bottom sheet
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Sample data kendaraan
    val daftarKendaraan = remember {
        listOf(
            Kendaraan(1, "Honda Beat", "B 1234 ABC"),
            Kendaraan(2, "Toyota Avanza", "B 5678 DEF"),
            Kendaraan(3, "Yamaha NMAX", "B 9012 GHI")
        )
    }

    // State kendaraan yang dipilih
    var kendaraanTerpilih by remember { mutableStateOf<Kendaraan?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text("Welcome to Parkirkan App", modifier = Modifier.padding(16.dp))
            Row {
                Button(modifier = Modifier.padding(16.dp), onClick = {}) {
                    Text("Confirm Check Out")
                }
                Button(
                    modifier = Modifier.padding(16.dp),
                    onClick = { showBottomSheet = true }
                ) {
                    Text("Choose Vehicle")
                }
            }

            // Menampilkan kendaraan yang terpilih
            kendaraanTerpilih?.let {
                Text(
                    text = "Kendaraan terpilih: ${it.nama} (${it.nomorPlat})",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Home Screen", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToBroadcast) {
            Text("Go to Broadcast")
        }
    }

    // Bottom Sheet untuk memilih kendaraan
    if (showBottomSheet) {
        VehicleBottomSheet(
            bottomSheetState = bottomSheetState,
            daftarKendaraan = daftarKendaraan,
            onDismiss = { showBottomSheet = false },
            onKendaraanSelected = { kendaraan ->
                kendaraanTerpilih = kendaraan
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) {
                        showBottomSheet = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleBottomSheet(
    bottomSheetState: SheetState,
    daftarKendaraan: List<Kendaraan>,
    onDismiss: () -> Unit,
    onKendaraanSelected: (Kendaraan) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pilih Kendaraan",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(daftarKendaraan) { kendaraan ->
                    KendaraanItem(
                        kendaraan = kendaraan,
                        onClick = { onKendaraanSelected(kendaraan) }
                    )
                }

                item {
                    Button(
                        onClick = { /* Implementasi tambah kendaraan */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Kendaraan",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Tambah Kendaraan Baru")
                    }
                }

                // Tambahkan padding di bawah untuk menghindari konten terpotong
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun KendaraanItem(kendaraan: Kendaraan, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = kendaraan.nama,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = kendaraan.nomorPlat,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}