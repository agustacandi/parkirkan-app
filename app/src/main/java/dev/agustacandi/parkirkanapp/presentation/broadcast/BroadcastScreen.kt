package dev.agustacandi.parkirkanapp.presentation.broadcast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.SubcomposeAsyncImage
import dev.agustacandi.parkirkanapp.presentation.home.Broadcast
import dev.agustacandi.parkirkanapp.util.ext.checkHttps
import dev.agustacandi.parkirkanapp.util.ext.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    viewModel: BroadcastViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val broadcastItems = viewModel.getBroadcasts().collectAsLazyPagingItems()

    // Observe loading state
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Set loading state based on paging state
    LaunchedEffect(broadcastItems.loadState) {
        viewModel.setLoading(broadcastItems.loadState.refresh is LoadState.Loading)

        if (broadcastItems.loadState.refresh is LoadState.Error) {
            val error = (broadcastItems.loadState.refresh as LoadState.Error).error
            viewModel.handleError(error.localizedMessage ?: "Unknown error occurred")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcasts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            onRefresh = {
                viewModel.refreshData()
                broadcastItems.refresh()
            },
            isRefreshing = isLoading || broadcastItems.loadState.refresh is LoadState.Loading,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Show loading spinner for initial load
                isLoading && broadcastItems.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Show error for initial load
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

                // Show empty state
                broadcastItems.itemCount == 0 &&
                        broadcastItems.loadState.refresh !is LoadState.Loading -> {
                    EmptyContent(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Show broadcast list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(broadcastItems.itemCount) { index ->
                            val broadcastItem = broadcastItems[index]
                            broadcastItem?.let {
                                BroadcastItem(it)
                            }
                        }

                        // Show loading spinner for pagination
                        when (broadcastItems.loadState.append) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            is LoadState.Error -> {
                                val error = (broadcastItems.loadState.append as LoadState.Error).error
                                item {
                                    ErrorItem(
                                        message = error.localizedMessage ?: "Failed to load more broadcasts",
                                        onRetryClick = { broadcastItems.retry() }
                                    )
                                }
                            }

                            else -> {}
                        }

                        // Add bottom spacing to avoid content being hidden by system bars
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Display snackbar for error messages when we have content
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
fun BroadcastItem(broadcast: Broadcast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Display broadcast image if available
            broadcast.image?.let { imageUrl ->
                SubcomposeAsyncImage(
                    model = imageUrl.checkHttps(),
                    contentDescription = "Broadcast image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
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

            // Title
            Text(
                text = broadcast.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Creation date
            Text(
                text = broadcast.createdAt.formatDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = broadcast.description,
                style = MaterialTheme.typography.bodyLarge
            )
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text("Retry")
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
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Failed to Load Broadcasts",
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
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Try Again")
        }
    }
}

@Composable
fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No broadcasts available",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check back later for updates and announcements",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}