package dev.agustacandi.parkirkanapp.presentation.alert

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agustacandi.parkirkanapp.R

@Composable
fun AlertScreen(
    modifier: Modifier = Modifier,
    viewModel: AlertViewModel = hiltViewModel(),
    onConfirmClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time actions
    LaunchedEffect(viewModel) {
        viewModel.actionFlow.collect { action ->
            when (action) {
                is AlertAction.NavigateBack -> onRejectClick()
                is AlertAction.ShowSuccess -> onConfirmClick()
                is AlertAction.ShowError -> {
                    // Error is shown via snackbar
                }
            }
        }
    }

    // Show snackbar for errors
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.rejectCheckOut() },
                    enabled = !isLoading
                ) {
                    Text("Bukan Saya")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // In a real app, get license plate from app state or intent extras
                        // For now we'll use a placeholder
                        val licensePlate = "B 1234 ABC" // Example
                        viewModel.confirmCheckOut(licensePlate)
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Ya, itu Saya")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    painter = painterResource(
                        id = R.drawable.alert_illustration
                    ),
                    contentDescription = null
                )
                Text(
                    text = "Perhatian!",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Kami mendeteksi bahwa anda melakukan\n" +
                            "check out sebelum melakukan konfirmasi.\n" +
                            "Apakah benar itu anda?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}