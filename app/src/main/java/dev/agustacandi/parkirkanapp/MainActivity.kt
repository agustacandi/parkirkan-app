package dev.agustacandi.parkirkanapp

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import dev.agustacandi.parkirkanapp.presentation.SplashScreen
import dev.agustacandi.parkirkanapp.presentation.auth.AuthScreen
import dev.agustacandi.parkirkanapp.presentation.auth.AuthViewModel
import dev.agustacandi.parkirkanapp.presentation.auth.LoginState
import dev.agustacandi.parkirkanapp.presentation.home.HomeScreen
import dev.agustacandi.parkirkanapp.presentation.parking.ParkingScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.VehicleScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.add.AddVehicleScreen
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme
import dev.agustacandi.parkirkanapp.util.FCMTokenManager
import dev.agustacandi.parkirkanapp.util.RequestNotificationPermission
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cek status login sebelumnya
        authViewModel.checkLoginStatus()

        // Setup FCM
        setupFCM()

        setContent {
            ParkirkanAppTheme {
                RequestNotificationPermission()

                val navController = rememberNavController()
                val loginState by authViewModel.loginState.collectAsState()

                LaunchedEffect(loginState) {
                    when (loginState) {
                        is LoginState.Success,
                        is LoginState.AlreadyLoggedIn -> {
                            navController.navigate(NavDestination.Main.route) {
                                popUpTo(NavDestination.Login.route) { inclusive = true }
                            }
                        }

                        is LoginState.Error -> {
                            // Handle error state
                        }

                        else -> {}
                    }
                }
                ParkingAppNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun setupFCM() {
        // Subscribe ke topik umum jika diperlukan
        FirebaseMessaging.getInstance().subscribeToTopic("broadcast")

        // Log token FCM saat ini untuk debugging
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                fcmTokenManager.getFCMToken().collectLatest { result ->
                    if (result.isSuccess) {
                        Log.d("FCM", "Current FCM Token: ${result.getOrNull()}")
                    } else {
                        Log.e("FCM", "Failed to get FCM token", result.exceptionOrNull())
                    }
                }
            }
        }
    }
}

// 1. Definisi NavDestination
sealed class NavDestination(val route: String) {
    // Main destinations
    data object Splash : NavDestination("splash")
    data object Login : NavDestination("login")
    data object Main : NavDestination("main")

    // Nested destinations
    sealed class BottomNav(route: String) : NavDestination(route) {
        data object Home : BottomNav("home")
        data object Parking : BottomNav("parking")
        data object Vehicle : BottomNav("vehicle")
        data object Profile : BottomNav("profile")
    }

    // Sub-destinations
    data object Broadcast : NavDestination("broadcast")

    data object HistoryDetail : NavDestination("history_detail/{historyId}") {
        fun createRoute(historyId: String) = "history_detail/$historyId"
        const val ARG_HISTORY_ID = "historyId"
    }

    data object AddVehicle : NavDestination("add_vehicle")

    data object EditVehicle : NavDestination("edit_vehicle/{vehicleId}") {
        fun createRoute(vehicleId: String) = "edit_vehicle/$vehicleId"
        const val ARG_VEHICLE_ID = "vehicleId"
    }

    data object ChangePassword : NavDestination("change_password")
    data object About : NavDestination("about")
}

// 2. Navigation setup
@Composable
fun ParkingAppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {


    NavHost(
        navController = navController,
        startDestination = NavDestination.Splash.route,
        modifier = modifier
    ) {
        // Splash Screen
        composable(NavDestination.Splash.route) {
            SplashScreen()
        }

        // Login Screen
        composable(NavDestination.Login.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(NavDestination.Main.route) {
                        popUpTo(NavDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main Screen dengan Bottom Navigation
        composable(NavDestination.Main.route) {
            MainScreen(navController)
        }

        // Broadcast Screen
        composable(NavDestination.Broadcast.route) {
//            BroadcastScreen(
//                onNavigateBack = { navController.navigateUp() }
//            )
        }

        // History Detail Screen dengan parameter
        composable(
            route = NavDestination.HistoryDetail.route,
            arguments = listOf(
                navArgument(NavDestination.HistoryDetail.ARG_HISTORY_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val historyId =
                backStackEntry.arguments?.getString(NavDestination.HistoryDetail.ARG_HISTORY_ID)
                    ?: ""
//            HistoryDetailScreen(
//                historyId = historyId,
//                onNavigateBack = { navController.navigateUp() }
//            )
        }

        // Add Vehicle Screen
        composable(NavDestination.AddVehicle.route) {
            AddVehicleScreen(
                onNavigateBack = { navController.navigateUp() },
                onVehicleAdded = {
                    // Kembali ke halaman Vehicle dengan refresh data
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_vehicles", true)
                    navController.navigateUp()
                }
            )        }

        // Edit Vehicle Screen dengan parameter
        composable(
            route = NavDestination.EditVehicle.route,
            arguments = listOf(
                navArgument(NavDestination.EditVehicle.ARG_VEHICLE_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val vehicleId =
                backStackEntry.arguments?.getString(NavDestination.EditVehicle.ARG_VEHICLE_ID) ?: ""
//            EditVehicleScreen(
//                vehicleId = vehicleId,
//                onNavigateBack = { navController.navigateUp() },
//                onVehicleUpdated = { navController.navigateUp() }
//            )
        }

        // Change Password Screen
        composable(NavDestination.ChangePassword.route) {
//            ChangePasswordScreen(
//                onNavigateBack = { navController.navigateUp() }
//            )
        }

        // About Screen
        composable(NavDestination.About.route) {
//            AboutScreen(
//                onNavigateBack = { navController.navigateUp() }
//            )
        }
    }
}

// 3. Implementation MainScreen dengan Bottom Navigation
@Composable
fun MainScreen(navController: NavHostController) {
    val bottomNavItems = listOf(
        NavDestination.BottomNav.Home,
        NavDestination.BottomNav.Parking,
        NavDestination.BottomNav.Vehicle,
        NavDestination.BottomNav.Profile
    )

    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
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
                            navController.navigate(NavDestination.AddVehicle.route)
                        },
                        onEditVehicleClick = { vehicleId ->
                            navController.navigate(NavDestination.EditVehicle.createRoute(vehicleId))
                        },
                        navController = navController
                    )
                }

                NavDestination.BottomNav.Profile -> {
                    ProfileScreen(
                        onChangePasswordClick = {
                            navController.navigate(NavDestination.ChangePassword.route)
                        },
                        onAboutClick = {
                            navController.navigate(NavDestination.About.route)
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

@Composable
fun ProfileScreen(
    onChangePasswordClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Profile Screen", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onChangePasswordClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Password")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAboutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("About")
        }
    }

}
