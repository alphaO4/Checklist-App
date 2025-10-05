package com.feuerwehr.checklist.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.feuerwehr.checklist.presentation.screen.DashboardScreen
import com.feuerwehr.checklist.presentation.screen.LoginScreen
import com.feuerwehr.checklist.presentation.screen.VehicleListScreen
import com.feuerwehr.checklist.presentation.screen.VehicleChecklistScreen
import com.feuerwehr.checklist.presentation.screen.VehicleSelectionScreen
import com.feuerwehr.checklist.presentation.screen.ChecklistScreen
import com.feuerwehr.checklist.presentation.screen.ChecklistListScreen
import com.feuerwehr.checklist.presentation.screen.ChecklistExecutionScreen
import com.feuerwehr.checklist.presentation.screen.ChecklistDetailScreen
import com.feuerwehr.checklist.presentation.screen.VehicleDetailScreen
import com.feuerwehr.checklist.presentation.screen.TemplatesScreen
import com.feuerwehr.checklist.presentation.screen.ProfileScreen
import com.feuerwehr.checklist.presentation.screen.TuvManagementScreen

/**
 * Main navigation for the Android-first Checklist App
 * Uses Jetpack Navigation Compose
 */
@Composable
fun ChecklistNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoute.Login.route
    ) {
        composable(NavigationRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavigationRoute.Dashboard.route) {
                        popUpTo(NavigationRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoute.Dashboard.route) {
            DashboardScreen(
                onNavigateToVehicles = {
                    navController.navigate(NavigationRoute.VehicleList.route)
                },
                onNavigateToChecklists = {
                    navController.navigate(NavigationRoute.ChecklistList.route)
                },
                onNavigateToTemplates = {
                    navController.navigate(NavigationRoute.Templates.route)
                },
                onNavigateToProfile = {
                    navController.navigate(NavigationRoute.Profile.route)
                },
                onLogout = {
                    navController.navigate(NavigationRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoute.VehicleList.route) {
            VehicleListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToVehicleChecklists = { vehicleId ->
                    navController.navigate("vehicle_checklist_management/$vehicleId")
                },
                onNavigateToVehicleDetails = { vehicleId ->
                    navController.navigate("vehicle_details/$vehicleId")
                }
            )
        }
        
        composable(NavigationRoute.ChecklistList.route) {
            // Vehicle selection screen for checklists
            VehicleSelectionScreen(
                onVehicleSelected = { vehicleId ->
                    navController.navigate("vehicle_checklists/$vehicleId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("vehicle_checklists/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull() ?: 0
            ChecklistListScreen(
                vehicleId = vehicleId,
                onNavigateToChecklistDetails = { checklistId ->
                    navController.navigate("checklist_details/$checklistId")
                },
                onNavigateToExecution = { checklistId, executionVehicleId ->
                    navController.navigate("checklist_execution/$checklistId/$executionVehicleId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // New route for vehicle checklist management
        composable("vehicle_checklist_management/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull() ?: 0
            VehicleChecklistScreen(
                vehicleId = vehicleId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExecution = { executionId ->
                    navController.navigate("execution_details/$executionId")
                }
            )
        }
        
        composable(NavigationRoute.Templates.route) {
            TemplatesScreen()
        }

        composable("checklist_details/{checklistId}") { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getString("checklistId")?.toIntOrNull() ?: 0
            ChecklistDetailScreen(
                checklistId = checklistId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartExecution = { clId, vehId ->
                    if (vehId != null) {
                        navController.navigate("checklist_execution/$clId/$vehId")
                    }
                },
                onSelectVehicleForExecution = { clId ->
                    navController.navigate("vehicle_selection_for_checklist/$clId")
                }
            )
        }
        
        composable("vehicle_details/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull() ?: 0
            VehicleDetailScreen(
                vehicleId = vehicleId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChecklists = { vehId ->
                    navController.navigate("vehicle_checklists/$vehId")
                },
                onNavigateToTuvManagement = { vehId ->
                    navController.navigate("tuv_management/$vehId")
                }
            )
        }
        
        composable("vehicle_selection_for_checklist/{checklistId}") { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getString("checklistId")?.toIntOrNull() ?: 0
            VehicleSelectionScreen(
                onVehicleSelected = { vehicleId ->
                    navController.navigate("checklist_execution/$checklistId/$vehicleId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("checklist_execution/{checklistId}/{vehicleId}") { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getString("checklistId")?.toIntOrNull() ?: 0
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull() ?: 0
            
            ChecklistExecutionScreen(
                checklistId = checklistId,
                vehicleId = vehicleId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExecutionComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavigationRoute.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(NavigationRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("tuv_management/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toIntOrNull() ?: 0
            TuvManagementScreen(
                vehicleId = vehicleId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation routes for the app
 */
sealed class NavigationRoute(val route: String) {
    object Login : NavigationRoute("login")
    object Dashboard : NavigationRoute("dashboard")
    object VehicleList : NavigationRoute("vehicles")
    object ChecklistList : NavigationRoute("checklists")
    object Templates : NavigationRoute("templates")
    object TuvOverview : NavigationRoute("tuv")
    object Profile : NavigationRoute("profile")
}