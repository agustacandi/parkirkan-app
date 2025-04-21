package dev.agustacandi.parkirkanapp.presentation.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import dev.agustacandi.parkirkanapp.data.parking.response.ParkingRecord
import dev.agustacandi.parkirkanapp.util.ext.checkHttps
import dev.agustacandi.parkirkanapp.util.ext.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel = hiltViewModel()
) {
    val parkingItems = viewModel.getParkingRecords().collectAsLazyPagingItems()

    // Collect state dari ViewModel menggunakan collectAsState
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Effect untuk mengupdate loading state berdasarkan Paging
    LaunchedEffect(parkingItems.loadState) {
        when (val refresh = parkingItems.loadState.refresh) {
            is LoadState.Loading -> viewModel.setLoading(true)
            is LoadState.Error -> {
                viewModel.setLoading(false)
                viewModel.handleError(refresh.error.localizedMessage ?: "Unknown error")
            }

            is LoadState.NotLoading -> viewModel.setLoading(false)
        }
    }

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Parking Records") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                ),
//                actions = {
//                    IconButton(onClick = {
//                        viewModel.refreshData()
//                        parkingItems.refresh()
//                    }) {
//                        Icon(
//                            imageVector = Icons.Default.Refresh,
//                            contentDescription = "Refresh",
//                            tint = MaterialTheme.colorScheme.onPrimaryContainer
//                        )
//                    }
//                }
//            )
//        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(parkingItems.itemCount) { index ->
                    val parkingRecord = parkingItems[index]
                    parkingRecord?.let {
                        ParkingRecordItem(it)
                    }
                }

                // Loading and error states
                item {
                    when {
                        parkingItems.loadState.refresh is LoadState.Loading -> {
                            LoadingItem(modifier = Modifier.fillParentMaxSize())
                        }

                        parkingItems.loadState.append is LoadState.Loading -> {
                            LoadingItem()
                        }

                        parkingItems.loadState.refresh is LoadState.Error -> {
                            val error = parkingItems.loadState.refresh as LoadState.Error
                            ErrorItem(
                                message = error.error.localizedMessage ?: "An error occurred",
                                onRetryClick = { parkingItems.retry() },
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }

                        parkingItems.loadState.append is LoadState.Error -> {
                            val error = parkingItems.loadState.append as LoadState.Error
                            ErrorItem(
                                message = error.error.localizedMessage ?: "An error occurred",
                                onRetryClick = { parkingItems.retry() }
                            )
                        }
                    }
                }
            }

            // Tampilkan indikator loading jika sedang refresh
//            if (isLoading) {
//                CircularProgressIndicator(
//                    modifier = Modifier
//                        .align(Alignment.TopCenter)
//                        .padding(top = 8.dp)
//                )
//            }

            // Tampilkan error message jika ada
            errorMessage?.let {
                if (it.isNotEmpty()) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.handleError("") }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(it)
                    }
                }
            }
        }
    }
}


@Composable
fun ParkingRecordItem(parkingRecord: ParkingRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${parkingRecord.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                StatusBadge(status = parkingRecord.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Check-in Image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        parkingRecord.checkInImage.checkHttps(),
                        contentDescription = "Check-in image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        text = "Check In",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                            )
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Check-out Image
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        parkingRecord.checkOutImage?.checkHttps(),
                        contentDescription = "Check-out image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Text(
                        text = "Check Out",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                            )
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Check In",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parkingRecord.checkInTime.formatDateTime(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Check Out",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parkingRecord.checkOutTime.let {
                            if (it.isNullOrEmpty()) "Not Checked Out" else it.formatDateTime()
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vehicle and User Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vehicle ID: ${parkingRecord.vehicleId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "User ID: ${parkingRecord.userId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "done" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "pending" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "cancelled" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun LoadingItem(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorItem(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Retry")
        }
    }
}

// Helper function to format date time