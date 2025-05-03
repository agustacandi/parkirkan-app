package dev.agustacandi.parkirkanapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import dev.agustacandi.parkirkanapp.presentation.alert.AlertScreen
import dev.agustacandi.parkirkanapp.presentation.splash.SplashScreen
import dev.agustacandi.parkirkanapp.presentation.auth.AuthScreen
import dev.agustacandi.parkirkanapp.presentation.auth.AuthViewModel
import dev.agustacandi.parkirkanapp.presentation.auth.LoginState
import dev.agustacandi.parkirkanapp.presentation.main.MainScreen
import dev.agustacandi.parkirkanapp.presentation.profile.about.AboutScreen
import dev.agustacandi.parkirkanapp.presentation.profile.password.ChangePasswordScreen
import dev.agustacandi.parkirkanapp.presentation.settings.BatteryOptimizationDialog
import dev.agustacandi.parkirkanapp.presentation.settings.MiuiSettingsDialog
import dev.agustacandi.parkirkanapp.presentation.vehicle.add.AddVehicleScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.edit.EditVehicleScreen
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme
import dev.agustacandi.parkirkanapp.util.BatteryOptimizationChecker
import dev.agustacandi.parkirkanapp.util.FCMTokenManager
import dev.agustacandi.parkirkanapp.util.MiuiHelper
import dev.agustacandi.parkirkanapp.util.RequestNotificationPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    private val _navigationEvent = MutableStateFlow<String?>(null)
    private val navigationEvent: StateFlow<String?> = _navigationEvent.asStateFlow()

    // Add state for battery dialog
    private val _showBatteryDialog = MutableStateFlow(false)
    private val showBatteryDialog: StateFlow<Boolean> = _showBatteryDialog.asStateFlow()

    private val _showMiuiDialog = MutableStateFlow(false)
    private val showMiuiDialog: StateFlow<Boolean> = _showMiuiDialog.asStateFlow()

    private fun checkDeviceSettings() {
        // Check MIUI device
        if (MiuiHelper.isMiuiDevice()) {
            // Show dialog after a delay to avoid blocking app start
            lifecycleScope.launch {
                delay(1000)
                _showMiuiDialog.value = true
            }
        } else if (!BatteryOptimizationChecker.isIgnoringBatteryOptimizations(this)) {
            // Regular battery optimization dialog for non-MIUI devices
            lifecycleScope.launch {
                delay(1000)
                _showBatteryDialog.value = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cek status login sebelumnya
        authViewModel.checkLoginStatus()

        // Setup FCM
        setupFCM()

        handleNotificationIntent(intent)

        checkDeviceSettings()

        // Check battery optimization status
        checkBatteryOptimization()

        setContent {
            ParkirkanAppTheme {
                RequestNotificationPermission()

                val navController = rememberNavController()
                val loginState by authViewModel.loginState.collectAsState()
                // Collect navigation events from intents
                val navEvent by navigationEvent.collectAsState()
                val shouldShowBatteryDialog by showBatteryDialog.collectAsState()
                val shouldShowMiuiDialog by showMiuiDialog.collectAsState()

                if (shouldShowMiuiDialog) {
                    MiuiSettingsDialog(
                        onDismiss = { _showMiuiDialog.value = false }
                    )
                }


                // Show battery optimization dialog if needed
                if (shouldShowBatteryDialog) {
                    BatteryOptimizationDialog(
                        onDismiss = { _showBatteryDialog.value = false }
                    )
                }

                // Handle navigation events
                LaunchedEffect(navEvent) {
                    navEvent?.let { destination ->
                        Log.d("MainActivity", "Navigating to: $destination")
                        // Wait for login state to be determined before navigating
                        delay(300) // Small delay to ensure login state is processed
                        // Navigate to the destination
                        navController.navigate(destination) {
                            // Clear back stack when navigating to Alert screen
                            if (destination == NavDestination.Alert.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            }
                        }

                        // Reset the navigation event
                        _navigationEvent.value = null
                    }
                }

                LaunchedEffect(loginState) {
                    when (loginState) {
                        is LoginState.Login -> {
                            navController.navigate(NavDestination.Login.route) {
                                popUpTo(NavDestination.Splash.route) { inclusive = true }
                            }
                        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Save the new intent
        setIntent(intent)
        handleNotificationIntent(intent)
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

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            Log.d("MainActivity", "Intent received: action=${it.action}, extras=${it.extras}")

            // Check if this is from a notification
            if (it.getBooleanExtra("notification_opened", false)) {
                // Get notification type and target route
                val notificationType = it.getStringExtra("notification_type")
                val targetRoute = it.getStringExtra("target_route")
                val timestamp = it.getLongExtra("timestamp", 0)

                Log.d("MainActivity", "Notification: type=$notificationType, route=$targetRoute, time=$timestamp")

                // Handle alert notifications
                if (notificationType == "alert" || targetRoute == NavDestination.Alert.route) {
                    // Set navigation event with a small delay to ensure app is ready
                    lifecycleScope.launch {
                        delay(100)
                        _navigationEvent.value = NavDestination.Alert.route
                        Log.d("MainActivity", "Navigation set to Alert")
                    }
                }
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (!BatteryOptimizationChecker.isIgnoringBatteryOptimizations(this)) {
            // App is under battery optimization - show dialog after a delay to avoid blocking app start
            lifecycleScope.launch {
                delay(1000) // Show dialog after a short delay
                _showBatteryDialog.value = true
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

    data object Alert : NavDestination("alert")

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

        // Alert Screen
        composable(NavDestination.Alert.route) {
            AlertScreen(
                onConfirmClick = {
                    // Handle confirmation - navigate back to main
                    navController.popBackStack(NavDestination.Main.route, false)
                },
                onRejectClick = {
                    // Handle rejection - just go back
                    navController.popBackStack()
                }
            )
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
        ) { _ ->
            EditVehicleScreen(
                onNavigateBack = { navController.navigateUp() },
                onVehicleUpdated = {
                    // Set refresh flag and navigate back
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_vehicles", true)
                    navController.navigateUp()
                },
                onVehicleDeleted = {
                    // Set refresh flag and navigate back
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_vehicles", true)
                    navController.navigateUp()
                }
            )
        }

        // Change Password Screen
        composable(NavDestination.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // About Screen
        composable(NavDestination.About.route) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}