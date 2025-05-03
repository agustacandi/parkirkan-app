package dev.agustacandi.parkirkanapp.presentation.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import dev.agustacandi.parkirkanapp.NavDestination
import dev.agustacandi.parkirkanapp.presentation.profile.ProfileScreen
import dev.agustacandi.parkirkanapp.presentation.security.broadcast.SecurityBroadcastScreen
import dev.agustacandi.parkirkanapp.presentation.security.home.SecurityHomeScreen

@Composable
fun SecurityMainScreen(navController: NavHostController) {
    val bottomNavItems = listOf(
        NavDestination.SecurityNav.Home,
        NavDestination.SecurityNav.Broadcast,
        NavDestination.SecurityNav.Profile
    )

    var selectedItem by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                        label = { Text(item.route.replaceFirstChar { e -> e.uppercaseChar() }) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
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
                            // AuthViewModel will handle the actual logout
                            // This just triggers the process
                        }
                    )
                }
            }
        }
    }
}