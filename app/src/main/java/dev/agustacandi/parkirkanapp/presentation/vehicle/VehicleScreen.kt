package dev.agustacandi.parkirkanapp.presentation.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import dev.agustacandi.parkirkanapp.data.vehicle.response.VehicleRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    viewModel: VehicleViewModel = hiltViewModel(),
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (String) -> Unit,
    navController: NavHostController
) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val vehicleItems = viewModel.getParkingRecords().collectAsLazyPagingItems()

    // Collect state dari ViewModel menggunakan collectAsState
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Effect untuk mengupdate loading state berdasarkan Paging
    LaunchedEffect(vehicleItems.loadState) {
        when (val refresh = vehicleItems.loadState.refresh) {
            is LoadState.Loading -> viewModel.setLoading(true)
            is LoadState.Error -> {
                viewModel.setLoading(false)
                viewModel.handleError(refresh.error.localizedMessage ?: "Unknown error")
            }
            is LoadState.NotLoading -> viewModel.setLoading(false)
        }
    }

    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<Boolean>("refresh_vehicles")?.observe(navController.currentBackStackEntry!!) { shouldRefresh ->
            if (shouldRefresh) {
                viewModel.refreshData()
                vehicleItems.refresh()
                savedStateHandle["refresh_vehicles"] = false
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicleClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Menggunakan PullToRefreshBox untuk implementasi pull-to-refresh
            PullToRefreshBox(
                onRefresh = {
                    viewModel.refreshData()
                    vehicleItems.refresh()
                },
                isRefreshing = isLoading || vehicleItems.loadState.refresh is LoadState.Loading,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Main content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(vehicleItems.itemCount) { index ->
                        val vehicleRecord = vehicleItems[index]
                        vehicleRecord?.let {
                            VehicleRecordItem(it, onEditVehicleClick)
                        }
                    }

                    // Loading and error states
                    item {
                        when {
                            vehicleItems.loadState.append is LoadState.Loading -> {
                                LoadingItem()
                            }
                            vehicleItems.loadState.refresh is LoadState.Error -> {
                                val error = vehicleItems.loadState.refresh as LoadState.Error
                                ErrorItem(
                                    message = error.error.localizedMessage ?: "An error occurred",
                                    onRetryClick = { vehicleItems.retry() },
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                            vehicleItems.loadState.append is LoadState.Error -> {
                                val error = vehicleItems.loadState.append as LoadState.Error
                                ErrorItem(
                                    message = error.error.localizedMessage ?: "An error occurred",
                                    onRetryClick = { vehicleItems.retry() }
                                )
                            }
                        }
                    }
                }
            }

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
fun VehicleRecordItem(vehicleRecord: VehicleRecord, onEditVehicleClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(vehicleRecord.name)
                IconButton(onClick = { onEditVehicleClick(vehicleRecord.id.toString()) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
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