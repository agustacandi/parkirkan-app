package dev.agustacandi.parkirkanapp.presentation.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.SubcomposeAsyncImage
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord
import dev.agustacandi.parkirkanapp.util.ext.checkHttps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    viewModel: VehicleViewModel = hiltViewModel(),
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (String) -> Unit,
    navController: NavHostController
) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val vehicleItems = viewModel.getVehicles().collectAsLazyPagingItems()

    // Collect state from ViewModel using collectAsState
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()


    // Observer for refresh request from other screens
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<Boolean>("refresh_vehicles")?.let { shouldRefresh ->
            if (shouldRefresh) {
                viewModel.refreshData()
                vehicleItems.refresh()
                savedStateHandle["refresh_vehicles"] = false
            }
        }
    }

    // Observe loading state from paging data
    LaunchedEffect(vehicleItems.loadState) {
        viewModel.setLoading(vehicleItems.loadState.refresh is LoadState.Loading)

        if (vehicleItems.loadState.refresh is LoadState.Error) {
            val error = (vehicleItems.loadState.refresh as LoadState.Error).error
            viewModel.handleError(error.localizedMessage ?: "Unknown error occurred")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicleClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PullToRefreshBox(
                onRefresh = {
                    viewModel.refreshData()
                    vehicleItems.refresh()
                },
                isRefreshing = isLoading || vehicleItems.loadState.refresh is LoadState.Loading,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isLoading && vehicleItems.itemCount == 0 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null && vehicleItems.itemCount == 0 -> {
                        ErrorContent(
                            message = errorMessage ?: "Unknown error occurred",
                            onRetryClick = {
                                viewModel.handleError(null)
                                vehicleItems.refresh()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    vehicleItems.itemCount == 0 && vehicleItems.loadState.refresh !is LoadState.Loading -> {
                        EmptyStateContent(
                            onAddClick = onAddVehicleClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(vehicleItems.itemCount) { index ->
                                val vehicle = vehicleItems[index]
                                vehicle?.let {
                                    VehicleListItem(
                                        vehicle = it,
                                        onEditClick = { onEditVehicleClick(it.id.toString()) }
                                    )
                                }
                            }

                            // Show loader for pagination
                            when (vehicleItems.loadState.append) {
                                is LoadState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }
                                }
                                is LoadState.Error -> {
                                    val error = (vehicleItems.loadState.append as LoadState.Error).error
                                    item {
                                        ErrorItem(
                                            message = error.localizedMessage ?: "Unknown error",
                                            onRetryClick = { vehicleItems.retry() }
                                        )
                                    }
                                }
                                else -> {}
                            }

                            // Add bottom padding to ensure last item isn't covered by FAB
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            // Show snackbar for errors
            errorMessage?.let { message ->
                if (message.isNotEmpty() && vehicleItems.itemCount > 0) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.handleError(null) }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleListItem(
    vehicle: VehicleRecord,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onEditClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle image or placeholder
            Card(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (vehicle.image.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = vehicle.image.checkHttps(),
                            contentDescription = "Vehicle image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            },
                            error = {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Vehicle details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vehicle.licensePlate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Edit icon button
            IconButton(
                onClick = onEditClick
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit vehicle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Vehicles Found",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You haven't added any vehicles yet. Add a vehicle to get started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Vehicle")
        }
    }
}

@Composable
fun ErrorItem(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            FilledTonalIconButton(
                onClick = onRetryClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Try again"
                )
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}