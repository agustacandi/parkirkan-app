package dev.agustacandi.parkirkanapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme

@Composable
fun AuthScreenBak(onLoginSuccess: () -> Unit) {
    // Buat Halaman Login yang terdiri dari TextField untuk username dan password dan button login
    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Parkirkan App", style = MaterialTheme.typography.headlineLarge)
                Text("Login to use this app", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(32.dp))
                // TextField untuk username
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                // TextField untuk password
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLoginSuccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
        }
    }
}

@Preview
@Composable
private fun AuthScreenPreview() {
   ParkirkanAppTheme {
       AuthScreenBak {  }
   }
}