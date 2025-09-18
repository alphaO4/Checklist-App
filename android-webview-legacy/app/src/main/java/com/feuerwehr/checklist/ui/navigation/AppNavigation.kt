package com.feuerwehr.checklist.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.feuerwehr.checklist.data.models.UserRole
import com.feuerwehr.checklist.ui.screens.*
import com.feuerwehr.checklist.ui.viewmodel.AuthViewModel

/**
 * Main app navigation with bottom navigation and authentication handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    // Navigate to login if not authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
    }
    
    if (authState.isAuthenticated) {
        MainScreenContent(
            navController = navController,
            currentUser = authState.currentUser,
            onLogout = { authViewModel.logout() },
            modifier = modifier
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = modifier
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    navController: NavHostController,
    currentUser: com.feuerwehr.checklist.data.models.User?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val isAdmin = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.ORGANISATOR
    
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (shouldShowBottomBar(currentDestination)) {
                BottomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    isAdmin = isAdmin
                )
            }
        },
        topBar = {
            if (shouldShowTopBar(currentDestination)) {
                TopAppBar(
                    title = { 
                        Text(getScreenTitle(currentDestination?.route))
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Abmelden"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Main screens
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToVehicles = {
                        navController.navigate(Screen.Vehicles.route)
                    },
                    onNavigateToChecklists = {
                        navController.navigate(Screen.Checklists.route)
                    },
                    onNavigateToTuv = {
                        navController.navigate(Screen.TuvManagement.route)
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Screen.AdminPanel.route)
                    }
                )
            }
            
            composable(Screen.Vehicles.route) {
                VehicleListScreen(
                    onVehicleClick = { vehicleId ->
                        navController.navigate(Screen.VehicleDetail.createRoute(vehicleId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Checklists.route) {
                ChecklistListScreen(
                    onChecklistClick = { checklistId ->
                        navController.navigate(Screen.ChecklistDetail.createRoute(checklistId))
                    },
                    onExecuteChecklist = { checklistId, vehicleId ->
                        navController.navigate(Screen.ChecklistExecution.createRoute(checklistId, vehicleId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    isAdmin = isAdmin,
                    onEditChecklist = { checklistId ->
                        navController.navigate(Screen.ChecklistEditor.createRoute(checklistId))
                    }
                )
            }
            
            composable(Screen.TuvManagement.route) {
                TuvManagementScreen(
                    onTuvClick = { tuvId ->
                        navController.navigate(Screen.TuvDetail.createRoute(tuvId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Admin screens
            if (isAdmin) {
                composable(Screen.AdminPanel.route) {
                    AdminPanelScreen(
                        onNavigateToChecklistEditor = {
                            navController.navigate(Screen.ChecklistEditor.createNewRoute())
                        },
                        onNavigateToVehicleTypes = {
                            navController.navigate(Screen.VehicleTypeManagement.route)
                        },
                        onNavigateToGroups = {
                            navController.navigate(Screen.GroupManagement.route)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                
                composable(Screen.ChecklistEditor.route) { backStackEntry ->
                    val checklistIdArg = backStackEntry.arguments?.getString(NavigationArgs.CHECKLIST_ID)
                    val checklistId = if (checklistIdArg == "new") null else checklistIdArg?.toIntOrNull()
                    
                    ChecklistEditorScreen(
                        checklistId = checklistId,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onSaveSuccess = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            
            // Detail screens
            composable(Screen.VehicleDetail.route) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString(NavigationArgs.VEHICLE_ID)?.toIntOrNull() ?: 0
                VehicleDetailScreen(
                    vehicleId = vehicleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ChecklistDetail.route) { backStackEntry ->
                val checklistId = backStackEntry.arguments?.getString(NavigationArgs.CHECKLIST_ID)?.toIntOrNull() ?: 0
                ChecklistDetailScreen(
                    checklistId = checklistId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ChecklistExecution.route) { backStackEntry ->
                val checklistId = backStackEntry.arguments?.getString(NavigationArgs.CHECKLIST_ID)?.toIntOrNull() ?: 0
                val vehicleId = backStackEntry.arguments?.getString(NavigationArgs.VEHICLE_ID)?.toIntOrNull() ?: 0
                ChecklistExecutionScreen(
                    checklistId = checklistId,
                    vehicleId = vehicleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.TuvDetail.route) { backStackEntry ->
                val tuvId = backStackEntry.arguments?.getString(NavigationArgs.TUV_ID)?.toIntOrNull() ?: 0
                TuvDetailScreen(
                    tuvId = tuvId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Additional admin screens
            if (isAdmin) {
                composable(Screen.VehicleTypeManagement.route) {
                    VehicleTypeManagementScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                composable(Screen.GroupManagement.route) {
                    GroupManagementScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    isAdmin: Boolean
) {
    val items = buildList {
        add(BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard))
        add(BottomNavItem(Screen.Vehicles.route, "Fahrzeuge", Icons.Default.DirectionsCar))
        add(BottomNavItem(Screen.Checklists.route, "Checklisten", Icons.Default.Checklist))
        add(BottomNavItem(Screen.TuvManagement.route, "TÜV", Icons.Default.Schedule))
        if (isAdmin) {
            add(BottomNavItem(Screen.AdminPanel.route, "Admin", Icons.Default.AdminPanelSettings))
        }
    }
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(item.icon, contentDescription = item.label)
                },
                label = { 
                    Text(item.label) 
                },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private fun shouldShowBottomBar(destination: NavDestination?): Boolean {
    return when (destination?.route) {
        Screen.Login.route -> false
        else -> true
    }
}

private fun shouldShowTopBar(destination: NavDestination?): Boolean {
    return when (destination?.route) {
        Screen.Login.route -> false
        Screen.Dashboard.route -> false // Dashboard has its own header
        else -> true
    }
}

private fun getScreenTitle(route: String?): String {
    return when (route) {
        Screen.Vehicles.route -> "Fahrzeuge"
        Screen.Checklists.route -> "Checklisten"
        Screen.TuvManagement.route -> "TÜV-Verwaltung"
        Screen.AdminPanel.route -> "Administration"
        Screen.VehicleTypeManagement.route -> "Fahrzeugtypen"
        Screen.GroupManagement.route -> "Gruppen"
        else -> "Feuerwehr Checklist"
    }
}