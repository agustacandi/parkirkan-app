package dev.agustacandi.parkirkanapp.presentation.broadcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.SubcomposeAsyncImage
import dev.agustacandi.parkirkanapp.presentation.common.ErrorContent
import dev.agustacandi.parkirkanapp.presentation.common.ErrorItem
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
    
    // Create scrolling behavior for collapsing top app bar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Set loading state based on paging state
    LaunchedEffect(broadcastItems.loadState) {
        viewModel.setLoading(broadcastItems.loadState.refresh is LoadState.Loading)

        if (broadcastItems.loadState.refresh is LoadState.Error) {
            val error = (broadcastItems.loadState.refresh as LoadState.Error).error
            viewModel.handleError(error.localizedMessage ?: "Unknown error occurred")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
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
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            strokeWidth = 3.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
        }
    }
}

@Composable
fun BroadcastItem(broadcast: Broadcast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
                        .clip(RoundedCornerShape(12.dp)),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
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
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check back later for updates and announcements",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}