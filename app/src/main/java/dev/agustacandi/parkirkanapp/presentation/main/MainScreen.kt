package dev.agustacandi.parkirkanapp.presentation.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.agustacandi.parkirkanapp.NavDestination
import dev.agustacandi.parkirkanapp.presentation.auth.AuthViewModel
import dev.agustacandi.parkirkanapp.presentation.auth.LogoutState
import dev.agustacandi.parkirkanapp.presentation.home.HomeScreen
import dev.agustacandi.parkirkanapp.presentation.parking.ParkingScreen
import dev.agustacandi.parkirkanapp.presentation.profile.ProfileScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.VehicleScreen
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val bottomNavItems = listOf(
        NavDestination.BottomNav.Home,
        NavDestination.BottomNav.Parking,
        NavDestination.BottomNav.Vehicle,
        NavDestination.BottomNav.Profile
    )

    var selectedItem by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            // Get destination from arguments to determine which tab to select when returning
            val returnToTab = entry.arguments?.getString("return_to_tab")
            if (returnToTab != null) {
                // Find the index of the tab to return to
                val tabIndex = bottomNavItems.indexOfFirst { it.route == returnToTab }
                if (tabIndex >= 0) {
                    selectedItem = tabIndex
                }
            }
        }
    }


    Scaffold(
        topBar = {
            if (selectedItem != 0) {
                TopAppBar(
                    title = { Text(bottomNavItems[selectedItem].route.replaceFirstChar { it.uppercase() }) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                )
            }
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            when (item) {
                                NavDestination.BottomNav.Home -> Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Home"
                                )

                                NavDestination.BottomNav.Parking -> Icon(
                                    Icons.Default.History,
                                    contentDescription = "Parking"
                                )

                                NavDestination.BottomNav.Vehicle -> Icon(
                                    Icons.Filled.Motorcycle,
                                    contentDescription = "Vehicle"
                                )

                                NavDestination.BottomNav.Profile -> Icon(
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
                NavDestination.BottomNav.Home -> {
                    HomeScreen(
                        onNavigateToBroadcast = {
                            navController.navigate(NavDestination.Broadcast.route)
                        }
                    )
                }

                NavDestination.BottomNav.Parking -> {
                    ParkingScreen()
                }

                NavDestination.BottomNav.Vehicle -> {
                    VehicleScreen(
                        onAddVehicleClick = {
                            navController.navigate(NavDestination.AddVehicle.route) {
                                this.popUpTo(NavDestination.Main.route)
                                navController.currentBackStackEntry?.arguments?.putString("return_to_tab", NavDestination.BottomNav.Vehicle.route)
                            }
                        },
                        onEditVehicleClick = { vehicleId ->
                            navController.navigate(NavDestination.EditVehicle.createRoute(vehicleId))
                        },
                        navController = navController
                    )
                }

                NavDestination.BottomNav.Profile -> {

                    val authViewModel: AuthViewModel = hiltViewModel()
                    val logoutState by authViewModel.logoutState.collectAsState()

                    // Effect to navigate when logout is successful
                    LaunchedEffect(logoutState) {
                        if (logoutState is LogoutState.Success) {
                            navController.navigate(NavDestination.Login.route) {
                                popUpTo(NavDestination.Main.route) { inclusive = true }
                            }
                        }
                    }

                    ProfileScreen(
                        onChangePasswordClick = {
                            navController.navigate(NavDestination.ChangePassword.route)
                        },
                        onAboutClick = {
                            navController.navigate(NavDestination.About.route)
                        },
                        onLogoutClick = {
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainScreenPreview() {
    ParkirkanAppTheme {
        val navController = rememberNavController()
        MainScreen(navController)
    }
}
