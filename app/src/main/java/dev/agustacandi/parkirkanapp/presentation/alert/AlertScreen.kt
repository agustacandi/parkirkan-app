package dev.agustacandi.parkirkanapp.presentation.alert

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.agustacandi.parkirkanapp.R
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme

@Composable
fun AlertScreen(modifier: Modifier = Modifier) {
    Scaffold(bottomBar = {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Bukan Saya")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Ya, itu Saya")
            }
        }
    }) { paddingValues ->
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
                        R.drawable.alert_illustration
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
                            "Apakah benar ini anda?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertScreenPreview() {
    ParkirkanAppTheme {
        AlertScreen()
    }
}