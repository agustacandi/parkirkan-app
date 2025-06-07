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
import androidx.hilt.navigation.compose.hiltViewModel
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
import dev.agustacandi.parkirkanapp.presentation.security.SecurityMainScreen
import dev.agustacandi.parkirkanapp.presentation.security.broadcast.add.AddBroadcastScreen
import dev.agustacandi.parkirkanapp.presentation.security.broadcast.edit.EditBroadcastScreen
import dev.agustacandi.parkirkanapp.presentation.broadcast.BroadcastScreen
import dev.agustacandi.parkirkanapp.presentation.broadcast.detail.BroadcastDetailScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.add.AddVehicleScreen
import dev.agustacandi.parkirkanapp.presentation.vehicle.edit.EditVehicleScreen
import dev.agustacandi.parkirkanapp.ui.theme.ParkirkanAppTheme
import dev.agustacandi.parkirkanapp.util.FCMTokenManager
import dev.agustacandi.parkirkanapp.util.PreferenceManager
import dev.agustacandi.parkirkanapp.presentation.notification.NotificationPermissionScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val _navigationEvent = MutableStateFlow<String?>(null)
    private val navigationEvent: StateFlow<String?> = _navigationEvent.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for deep link first
        val isAlertDeepLink = intent?.data?.toString()?.startsWith("parkirkanapp://alert") ?: false
        if (isAlertDeepLink) {
            Log.d(TAG, "Alert deep link detected in onCreate")
            lifecycleScope.launch {
                _navigationEvent.value = NavDestination.Alert.route
            }
        }
        
        initApp()
        
        setContent {
            ParkirkanAppTheme {
                val navController = rememberNavController()
                val loginState by authViewModel.loginState.collectAsState()
                val navEvent by navigationEvent.collectAsState()

                // Handle navigation events with high priority
                LaunchedEffect(navEvent) {
                    navEvent?.let { destination ->
                        Log.d(TAG, "Navigation event detected, navigating to: $destination")
                        try {
                            if (destination == NavDestination.Alert.route) {
                                Log.d(TAG, "Starting navigation to Alert screen...")
                                
                                // First make sure NavController is ready
                                if (navController.graph.findNode(NavDestination.Alert.route) != null) {
                                    // Force navigation to Alert with extreme priority
                                    navController.navigate(destination) {
                                        // Pop entire back stack to make sure this screen shows
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                    Log.d(TAG, "Navigation to Alert completed successfully")
                                } else {
                                    Log.e(TAG, "Alert route not found in nav graph")
                                }
                            } else if (destination.startsWith("broadcast_detail/")) {
                                Log.d(TAG, "Starting navigation to Broadcast Detail screen...")
                                
                                // For broadcast detail, ensure user gets to the main screen first then navigate
                                if (loginState is LoginState.AlreadyLoggedIn || loginState is LoginState.Success) {
                                    navController.navigate(destination) {
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                    Log.d(TAG, "Navigation to Broadcast Detail completed successfully")
                                } else {
                                    // If user not logged in, delay navigation until login completes
                                    Log.d(TAG, "User not logged in, will navigate after login")
                                    return@LaunchedEffect
                                }
                            } else {
                                // Normal navigation for other destinations
                                navController.navigate(destination)
                            }
                            
                            // Clear the event after navigation attempt
                            _navigationEvent.value = null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during navigation", e)
                        }
                    }
                }

                LaunchedEffect(loginState) {
                    Log.d(TAG, "MainActivity observed LoginState change: ${loginState::class.simpleName}")
                    handleLoginState(loginState, navController)
                }

                ParkingAppNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    preferenceManager = preferenceManager,
                    authViewModel = authViewModel
                )
            }
        }
    }
    
    private fun initApp() {
        authViewModel.checkLoginStatus()
        setupFCM()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Check for deep link first - high priority
        val isAlertDeepLink = intent.data?.toString()?.startsWith("parkirkanapp://alert") ?: false
        if (isAlertDeepLink) {
            Log.d(TAG, "Alert deep link detected in onNewIntent")
            lifecycleScope.launch {
                _navigationEvent.value = NavDestination.Alert.route
            }
            return
        }
        
        // Then process as normal notification
        handleNotificationIntent(intent)
    }

    private fun setupFCM() {
        FirebaseMessaging.getInstance().subscribeToTopic("broadcast")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Successfully subscribed to 'broadcast' topic")
                } else {
                    Log.e(TAG, "❌ Failed to subscribe to 'broadcast' topic", task.exception)
                }
            }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                fcmTokenManager.getFCMToken().collectLatest { result ->
                    result.onSuccess { token ->
                        Log.d(TAG, "Current FCM Token: $token")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to get FCM token", error)
                    }
                }
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        Log.d(TAG, "handleNotificationIntent called with action: ${intent?.action}")
        Log.d(TAG, "Intent extras: ${intent?.extras?.keySet()?.joinToString()}")
        
        intent?.takeIf { it.getBooleanExtra("notification_opened", false) }?.let {
            val notificationType = it.getStringExtra("notification_type")
            val targetRoute = it.getStringExtra("target_route")
            val forceNavigation = it.getBooleanExtra("force_navigation", false)
            
            Log.d(TAG, "Notification intent detected: type=$notificationType, route=$targetRoute, force=$forceNavigation")
            
            if (notificationType == "alert" || targetRoute == NavDestination.Alert.route) {
                Log.d(TAG, "Processing alert notification intent, navigating to Alert screen")
                
                // Direct navigation using lifecycleScope for immediate effect
                lifecycleScope.launch {
                    try {
                        // Force navigation to Alert screen with high priority
                        _navigationEvent.value = NavDestination.Alert.route
                        Log.d(TAG, "Navigation event set to: ${_navigationEvent.value}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting navigation event", e)
                    }
                }
            } else if (notificationType == "broadcast" || intent.action == "OPEN_BROADCAST_NOTIFICATION") {
                Log.d(TAG, "Processing broadcast notification intent")
                
                val broadcastId = it.getStringExtra("broadcast_id")
                Log.d(TAG, "Broadcast ID from intent: $broadcastId")
                
                lifecycleScope.launch {
                    try {
                        val navigationRoute = if (broadcastId != null) {
                            NavDestination.BroadcastDetail.createRoute(broadcastId)
                        } else {
                            // Fallback to broadcast list if no ID
                            NavDestination.Broadcast.route
                        }
                        
                        _navigationEvent.value = navigationRoute
                        Log.d(TAG, "Broadcast navigation event set to: ${_navigationEvent.value}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting broadcast navigation event", e)
                    }
                }
            } else {
                Log.d(TAG, "No specific notification action, ignoring intent")
            }
        }
    }
    
    private fun handleLoginState(loginState: LoginState, navController: NavHostController) {
        Log.d(TAG, "handleLoginState called with state: ${loginState::class.simpleName}")
        
        when (loginState) {
            is LoginState.Login -> {
                Log.d(TAG, "Navigating to login flow")
                navigateToLoginFlow(navController)
            }
            is LoginState.AlreadyLoggedIn -> {
                Log.d(TAG, "User already logged in with role: ${loginState.userRole}")
                navigateToMainFlow(loginState.userRole, navController)
            }
            is LoginState.Success -> {
                Log.d(TAG, "Login success with role: ${loginState.userRole}")
                navigateToMainFlow(loginState.userRole, navController)
            }
            is LoginState.Error -> {
                Log.e(TAG, "Login error: ${loginState.message}")
                // Stay on login screen and show error
                if (navController.currentDestination?.route != NavDestination.Login.route) {
                    navigateToLoginFlow(navController)
                }
            }
            is LoginState.Loading -> {
                Log.d(TAG, "Login in progress, staying on current screen")
            }
            else -> {
                Log.d(TAG, "Unhandled login state: ${loginState::class.simpleName}")
            }
        }
    }
    
    private fun navigateToMainFlow(userRole: String, navController: NavHostController) {
        val destination = if (userRole == "security") {
            Log.d(TAG, "Navigating to security screen for security role")
            // Unsubscribe from alert topic first to ensure clean state
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alert")
            // Then subscribe to alert topic
            FirebaseMessaging.getInstance().subscribeToTopic("alert")
            NavDestination.SecurityNav.Home.route
        } else {
            Log.d(TAG, "Navigating to main screen for user role")
            // Ensure user is unsubscribed from alert topic
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alert")
            NavDestination.Main.route
        }
        
        Log.d(TAG, "Final destination: $destination")
        navController.navigate(destination) {
            popUpTo(NavDestination.Login.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    private fun navigateToLoginFlow(navController: NavHostController) {
        val destination = if (!preferenceManager.isNotificationPermissionRequested()) {
            Log.d(TAG, "Navigating to notification permission screen")
            NavDestination.NotificationPermission.route
        } else {
            Log.d(TAG, "Navigating to login screen")
            NavDestination.Login.route
        }
        
        navController.navigate(destination) {
            popUpTo(NavDestination.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }
}

// Navigation destinations
sealed class NavDestination(val route: String) {
    // Main destinations
    data object Splash : NavDestination("splash")
    data object NotificationPermission : NavDestination("notification_permission")
    data object Login : NavDestination("login")
    data object Main : NavDestination("main")
    data object Alert : NavDestination("alert")
    data object Broadcast : NavDestination("broadcast")
    data object ChangePassword : NavDestination("change_password")
    data object About : NavDestination("about")
    
    // Vehicle related destinations
    data object AddVehicle : NavDestination("add_vehicle")
    data object EditVehicle : NavDestination("edit_vehicle/{vehicleId}") {
        fun createRoute(vehicleId: String) = "edit_vehicle/$vehicleId"
        const val ARG_VEHICLE_ID = "vehicleId"
    }

    // Broadcast related destinations
    data object AddBroadcast : NavDestination("add_broadcast")
    data object EditBroadcast : NavDestination("edit_broadcast/{broadcastId}") {
        fun createRoute(broadcastId: String) = "edit_broadcast/$broadcastId"
        const val ARG_BROADCAST_ID = "broadcastId"
    }
    data object BroadcastDetail : NavDestination("broadcast_detail/{broadcastId}") {
        fun createRoute(broadcastId: String) = "broadcast_detail/$broadcastId"
        const val ARG_BROADCAST_ID = "broadcastId"
    }

    // Nested destinations
    sealed class BottomNav(route: String) : NavDestination(route) {
        data object Home : BottomNav("home")
        data object Parking : BottomNav("parking")
        data object Vehicle : BottomNav("vehicle")
        data object Profile : BottomNav("profile")
    }

    sealed class SecurityNav(route: String) : NavDestination(route) {
        data object Home : SecurityNav("security_home")
        data object Broadcast : SecurityNav("security_broadcast")
        data object Profile : SecurityNav("security_profile")
    }
}

// Navigation setup
@Composable
fun ParkingAppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    preferenceManager: PreferenceManager,
    authViewModel: AuthViewModel
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
        
        // Notification Permission Screen
        composable(NavDestination.NotificationPermission.route) {
            NotificationPermissionScreen(
                onContinue = { _ ->
                    preferenceManager.setNotificationPermissionRequested(true)
                    navController.navigate(NavDestination.Login.route) {
                        popUpTo(NavDestination.Splash.route) { inclusive = true }
                    }
                },
                preferenceManager = preferenceManager
            )
        }

        // Alert Screen
        composable(NavDestination.Alert.route) {
            AlertScreen(
                onConfirmClick = {
                    navController.popBackStack(NavDestination.Main.route, false)
                },
                onRejectClick = {
                    navController.popBackStack()
                }
            )
        }

        // Login Screen
        composable(NavDestination.Login.route) {
            AuthScreen(viewModel = authViewModel)
        }

        // Broadcast Screen
        composable(NavDestination.Broadcast.route) {
            BroadcastScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDetail = { broadcastId ->
                    navController.navigate(NavDestination.BroadcastDetail.createRoute(broadcastId))
                }
            )
        }

        // Main Screen with Bottom Navigation
        composable(NavDestination.Main.route) {
            val loginState by authViewModel.loginState.collectAsState()
            
            // Ambil userRole dari LoginState yang ada
            val userRole = when (loginState) {
                is LoginState.AlreadyLoggedIn -> (loginState as LoginState.AlreadyLoggedIn).userRole
                is LoginState.Success -> (loginState as LoginState.Success).userRole
                else -> "user"
            }
            
            Log.d(TAG, "Main route composable - User role: $userRole")
            
            RoleBasedMainScreen(role = userRole, navController = navController, authViewModel = authViewModel)
        }

        // Security Main Screen
        composable(NavDestination.SecurityNav.Home.route) {
            SecurityMainScreen(navController, authViewModel)
        }

        // Vehicle Screens
        composable(NavDestination.AddVehicle.route) {
            AddVehicleScreen(
                onNavigateBack = { navController.navigateUp() },
                onVehicleAdded = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_vehicles", true)
                    navController.navigateUp()
                }
            )
        }

        composable(
            route = NavDestination.EditVehicle.route,
            arguments = listOf(
                navArgument(NavDestination.EditVehicle.ARG_VEHICLE_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            EditVehicleScreen(
                onNavigateBack = { navController.navigateUp() },
                onVehicleUpdated = { setRefreshAndNavigateUp(navController, "refresh_vehicles") },
                onVehicleDeleted = { setRefreshAndNavigateUp(navController, "refresh_vehicles") }
            )
        }

        // Profile related screens
        composable(NavDestination.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(NavDestination.About.route) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Broadcast related screens
        composable(NavDestination.AddBroadcast.route) {
            AddBroadcastScreen(
                onNavigateBack = { navController.navigateUp() },
                onBroadcastAdded = { setRefreshAndNavigateUp(navController, "refresh_broadcasts") }
            )
        }

        composable(
            route = NavDestination.EditBroadcast.route,
            arguments = listOf(
                navArgument(NavDestination.EditBroadcast.ARG_BROADCAST_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            EditBroadcastScreen(
                onNavigateBack = { navController.navigateUp() },
                onBroadcastUpdated = { setRefreshAndNavigateUp(navController, "refresh_broadcasts") },
                onBroadcastDeleted = { setRefreshAndNavigateUp(navController, "refresh_broadcasts") }
            )
        }

        // Broadcast Detail Screen
        composable(
            route = NavDestination.BroadcastDetail.route,
            arguments = listOf(
                navArgument(NavDestination.BroadcastDetail.ARG_BROADCAST_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            BroadcastDetailScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

private fun setRefreshAndNavigateUp(navController: NavHostController, refreshKey: String) {
    navController.previousBackStackEntry
        ?.savedStateHandle
        ?.set(refreshKey, true)
    navController.navigateUp()
}

@Composable
fun RoleBasedMainScreen(
    role: String,
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    Log.d("RoleBasedMainScreen", "Current user role: $role")
    when (role) {
        "security" -> SecurityMainScreen(navController, authViewModel)
        else -> MainScreen(navController) // Default for "user" role
    }
}
