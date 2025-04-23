package dev.agustacandi.parkirkanapp.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.presentation.AppViewModel
import dev.agustacandi.parkirkanapp.util.ext.checkHttps
import dev.agustacandi.parkirkanapp.util.ext.formatDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
    onNavigateToBroadcast: () -> Unit
) {
    // State untuk menampilkan atau menyembunyikan bottom sheet
    var showBottomSheet by remember { mutableStateOf(false) }

    // State untuk bottom sheet
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Observe vehicle list state
    val vehicleListState by viewModel.vehicleListState.collectAsState()

    // Observe check-in status
    val isCheckedInState by viewModel.isCheckedInState.collectAsState()

    // Observe broadcast data
    val broadcastState by viewModel.broadcastState.collectAsState()

    // Observe selected vehicle from AppViewModel
    val selectedVehicle by appViewModel.selectedVehicle.collectAsState()

    // Fetch vehicles and broadcasts when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchVehicles()
        viewModel.fetchRecentBroadcasts()
    }

    // Selected vehicle state

    // Check if selected vehicle is checked in when vehicle changes
    LaunchedEffect(selectedVehicle) {
        selectedVehicle?.let {
            viewModel.checkVehicleCheckedInStatus(it.licensePlate)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card with Check In/Out
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Welcome to Parkirkan App",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display selected vehicle
                    selectedVehicle?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.size(48.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                SubcomposeAsyncImage(
                                    model = it.image.checkHttps(),
                                    contentDescription = "Vehicle image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = it.licensePlate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { appViewModel.setSelectedVehicle(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear selection"
                                )
                            }
                        }
                    } ?: run {
                        if (vehicleListState is VehicleListState.Success &&
                            (vehicleListState as VehicleListState.Success).vehicles.isEmpty()) {
                            // No vehicles available
                            Text(
                                "You don't have any vehicles yet. Please add a vehicle first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Prompt to select a vehicle
                            Text(
                                "Please select a vehicle to proceed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Confirm Check Out button - only show if a vehicle is selected AND checked in
                        if (selectedVehicle != null && isCheckedInState is CheckedInState.CheckedIn) {
                            Button(
                                onClick = {
                                    selectedVehicle?.let {
                                        viewModel.confirmCheckOut(it.licensePlate)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm Check Out")
                            }
                        }

                        // Choose Vehicle button - always show
                        if (vehicleListState is VehicleListState.Success &&
                            (vehicleListState as VehicleListState.Success).vehicles.isNotEmpty()) {

                            Button(
                                onClick = { showBottomSheet = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Choose Vehicle")
                            }
                        } else if (vehicleListState is VehicleListState.Success) {
                            // No vehicles, show Add Vehicle button
                            Button(
                                onClick = { /* TODO: Navigate to Add Vehicle */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Vehicle")
                            }
                        }
                    }

                    // Status message based on check-in state
                    when (isCheckedInState) {
                        is CheckedInState.Loading -> {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            )
                        }
                        is CheckedInState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (isCheckedInState as CheckedInState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is CheckedInState.CheckedIn -> {
                            Text(
                                "Your vehicle is currently checked in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        is CheckedInState.NotCheckedIn -> {
                            if (selectedVehicle != null) {
                                Text(
                                    "Your vehicle is not checked in",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                        else -> { /* Nothing to show for Idle state */ }
                    }
                }
            }
        }

        // Broadcasts section with See All button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Latest Broadcasts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = onNavigateToBroadcast
                ) {
                    Text("See All")
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "See all broadcasts",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Broadcasts content
        item {
            when (val state = broadcastState) {
                is BroadcastState.Success -> {
                    if (state.broadcasts.isEmpty()) {
                        EmptyBroadcasts()
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.broadcasts.take(3).forEach { broadcast ->
                                BroadcastItem(broadcast = broadcast)
                            }
                        }
                    }
                }
                is BroadcastState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is BroadcastState.Error -> {
                    ErrorBroadcasts(message = state.message) {
                        viewModel.fetchRecentBroadcasts()
                    }
                }
                else -> { /* Nothing to show for Idle state */ }
            }
        }
    }

    // Bottom Sheet for vehicle selection
    if (showBottomSheet && vehicleListState is VehicleListState.Success) {
        val vehicles = (vehicleListState as VehicleListState.Success).vehicles

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
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
                        text = "Select Vehicle",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showBottomSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vehicles) { vehicle ->
                        VehicleItem(
                            vehicle = vehicle,
                            onClick = {
                                appViewModel.setSelectedVehicle(vehicle)
                                scope.launch {
                                    bottomSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!bottomSheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                            }
                        )
                    }

                    // Add padding at the bottom
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleItem(vehicle: VehicleRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle image
            Card(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                SubcomposeAsyncImage(
                    model = vehicle.image.checkHttps(),
                    contentDescription = "Vehicle image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = vehicle.licensePlate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BroadcastItem(broadcast: Broadcast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Broadcast image if available
            broadcast.image?.let { imageUrl ->
                Card(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    SubcomposeAsyncImage(
                        model = imageUrl.checkHttps(),
                        contentDescription = "Broadcast image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = broadcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = broadcast.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = broadcast.createdAt.formatDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyBroadcasts() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No broadcasts available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorBroadcasts(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading broadcasts",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}