package dev.agustacandi.parkirkanapp.presentation.security

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.agustacandi.parkirkanapp.NavDestination
import dev.agustacandi.parkirkanapp.presentation.auth.AuthViewModel
import dev.agustacandi.parkirkanapp.presentation.auth.LogoutState
import dev.agustacandi.parkirkanapp.presentation.profile.ProfileScreen
import dev.agustacandi.parkirkanapp.presentation.security.broadcast.SecurityBroadcastScreen
import dev.agustacandi.parkirkanapp.presentation.security.home.SecurityHomeScreen

private const val TAG = "SecurityMainScreen"

@Composable
fun SecurityMainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val bottomNavItems = listOf(
        NavDestination.SecurityNav.Home,
        NavDestination.SecurityNav.Broadcast,
        NavDestination.SecurityNav.Profile
    )

    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    val logoutState by authViewModel.logoutState.collectAsState()
    
    // Add a flag to track if we're actively logging out
    var isLoggingOut by remember { mutableStateOf(false) }
    
    // Effect to navigate when logout is successful
    LaunchedEffect(logoutState) {
        if (logoutState is LogoutState.Success && isLoggingOut) {
            Log.d(TAG, "Logout successful, navigating to Login screen")
            navController.navigate(NavDestination.Login.route) {
                popUpTo(NavDestination.SecurityNav.Home.route) { inclusive = true }
            }
            // Reset the flag after navigation
            isLoggingOut = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 3.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            when (item) {
                                NavDestination.SecurityNav.Home -> Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                                NavDestination.SecurityNav.Broadcast -> Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = "Broadcasts"
                                )
                                NavDestination.SecurityNav.Profile -> Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile"
                                )
                            }
                        },
                        label = { 
                            Text(
                                text = item.route.replaceFirstChar { e -> e.uppercaseChar() },
                                fontWeight = if (selectedItem == index) FontWeight.Medium else FontWeight.Normal
                            ) 
                        },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (bottomNavItems[selectedItem]) {
                NavDestination.SecurityNav.Home -> {
                    SecurityHomeScreen(
                        onNavigateToBroadcast = {
                            selectedItem = 1 // Switch to broadcast tab
                        }
                    )
                }
                NavDestination.SecurityNav.Broadcast -> {
                    SecurityBroadcastScreen(
                        onAddBroadcastClick = {
                            navController.navigate(NavDestination.AddBroadcast.route)
                        },
                        onEditBroadcastClick = { broadcastId ->
                            navController.navigate(NavDestination.EditBroadcast.createRoute(broadcastId))
                        },
                        navController = navController
                    )
                }
                NavDestination.SecurityNav.Profile -> {
                    ProfileScreen(
                        onChangePasswordClick = {
                            navController.navigate(NavDestination.ChangePassword.route)
                        },
                        onAboutClick = {
                            navController.navigate(NavDestination.About.route)
                        },
                        onLogoutClick = {
                            Log.d(TAG, "Logout clicked, calling authViewModel.logout()")
                            // Set the flag before calling logout
                            isLoggingOut = true
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
    }
}