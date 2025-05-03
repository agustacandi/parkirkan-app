package dev.agustacandi.parkirkanapp.presentation.security.broadcast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.SubcomposeAsyncImage
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import dev.agustacandi.parkirkanapp.util.ext.checkHttps
import dev.agustacandi.parkirkanapp.util.ext.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityBroadcastScreen(
    viewModel: SecurityBroadcastViewModel = hiltViewModel(),
    onAddBroadcastClick: () -> Unit,
    onEditBroadcastClick: (String) -> Unit,
    navController: NavHostController
) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val broadcastItems = viewModel.getBroadcasts().collectAsLazyPagingItems()

    // Collect state from ViewModel using collectAsState
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Observer for refresh request from other screens
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<Boolean>("refresh_broadcasts")?.let { shouldRefresh ->
            if (shouldRefresh) {
                viewModel.refreshData()
                broadcastItems.refresh()
                savedStateHandle["refresh_broadcasts"] = false
            }
        }
    }

    // Observe loading state from paging data
    LaunchedEffect(broadcastItems.loadState) {
        viewModel.setLoading(broadcastItems.loadState.refresh is LoadState.Loading)

        if (broadcastItems.loadState.refresh is LoadState.Error) {
            val error = (broadcastItems.loadState.refresh as LoadState.Error).error
            viewModel.handleError(error.localizedMessage ?: "Unknown error occurred")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBroadcastClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Broadcast")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PullToRefreshBox(
                onRefresh = {
                    viewModel.refreshData()
                    broadcastItems.refresh()
                },
                isRefreshing = isLoading || broadcastItems.loadState.refresh is LoadState.Loading,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isLoading && broadcastItems.itemCount == 0 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null && broadcastItems.itemCount == 0 -> {
                        ErrorContent(
                            message = errorMessage ?: "Unknown error occurred",
                            onRetryClick = {
                                viewModel.handleError(null)
                                broadcastItems.refresh()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    broadcastItems.itemCount == 0 && broadcastItems.loadState.refresh !is LoadState.Loading -> {
                        EmptyStateContent(
                            onAddClick = onAddBroadcastClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(broadcastItems.itemCount) { index ->
                                val broadcast = broadcastItems[index]
                                broadcast?.let {
                                    SecurityBroadcastItem(
                                        broadcast = it,
                                        onEditClick = { onEditBroadcastClick(it.id.toString()) }
                                    )
                                }
                            }

                            // Show loader for pagination
                            when (broadcastItems.loadState.append) {
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
                                    val error = (broadcastItems.loadState.append as LoadState.Error).error
                                    item {
                                        ErrorItem(
                                            message = error.localizedMessage ?: "Unknown error",
                                            onRetryClick = { broadcastItems.retry() }
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
                if (message.isNotEmpty() && broadcastItems.itemCount > 0) {
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
fun SecurityBroadcastItem(
    broadcast: Broadcast,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onEditClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Broadcast image if available
            broadcast.image?.let { imageUrl ->
                SubcomposeAsyncImage(
                    model = imageUrl.checkHttps(),
                    contentDescription = "Broadcast image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = broadcast.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = broadcast.createdAt.formatDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit broadcast",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = broadcast.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
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
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tidak Ada Broadcast",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Anda belum membuat broadcast apapun. Tambahkan broadcast untuk memberikan informasi kepada pengguna.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
            Text("Tambah Broadcast")
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
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(
                onClick = onRetryClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Coba Lagi")
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
        Text(
            text = "Gagal Memuat Broadcast",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetryClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Coba Lagi")
        }
    }
}