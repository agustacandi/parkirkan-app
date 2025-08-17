package dev.agustacandi.parkirkanapp.presentation.alert

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme

@Composable
fun AlertScreen(
    modifier: Modifier = Modifier,
    viewModel: AlertViewModel = hiltViewModel(),
    onConfirmClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val activity = context as? Activity
    val intent = activity?.intent
    val vehiclePlate = intent?.getStringExtra("vehicle_license_plate") ?: "Unknown Plate"

    LaunchedEffect(viewModel) {
        viewModel.actionFlow.collect { action ->
            when (action) {
                is AlertAction.NavigateBack -> onRejectClick()
                is AlertAction.ShowSuccess -> onConfirmClick()
                is AlertAction.ShowError -> {}
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AlertScreenContent(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        vehiclePlate = vehiclePlate,
        isLoading = isLoading,
        onNotMe = { viewModel.reportCheckOut(vehiclePlate) },
        onYesItsMe = { viewModel.confirmCheckOut(vehiclePlate) }
    )
}

@Composable
private fun AlertScreenContent(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    vehiclePlate: String,
    isLoading: Boolean,
    onNotMe: () -> Unit,
    onYesItsMe: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 64.dp),
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    onClick = onNotMe,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    )
                ) {
                    Text(
                        "Not Me",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    modifier = Modifier
                        .weight(1f),
                    onClick = onYesItsMe,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Yes, It's Me",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(180.dp).padding(bottom = 24.dp),
                    painter = painterResource(id = R.drawable.alert_illustration),
                    contentDescription = null
                )
                Text(
                    text = "Attention!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "We detected that your vehicle with the number ${vehiclePlate} left before you confirmed your action. Is that you?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 4.dp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertScreenPreview() {
    ParkirkanAppTheme {
        AlertScreenContent(
            vehiclePlate = "B 1234 XYZ",
            isLoading = false,
            onNotMe = {},
            onYesItsMe = {}
        )
    }
}