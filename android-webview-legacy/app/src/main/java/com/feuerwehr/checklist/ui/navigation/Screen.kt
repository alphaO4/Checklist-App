package com.feuerwehr.checklist.ui.navigation

/**
 * Navigation destinations for the app
 */
sealed class Screen(val route: String) {
    // Authentication
    data object Login : Screen("login")
    
    // Main screens
    data object Dashboard : Screen("dashboard")
    data object Vehicles : Screen("vehicles")
    data object Checklists : Screen("checklists")
    data object TuvManagement : Screen("tuv")
    data object Profile : Screen("profile")
    
    // Detail screens
    data object VehicleDetail : Screen("vehicle_detail/{vehicleId}") {
        fun createRoute(vehicleId: Int) = "vehicle_detail/$vehicleId"
    }
    
    data object ChecklistExecution : Screen("checklist_execution/{checklistId}/{vehicleId}") {
        fun createRoute(checklistId: Int, vehicleId: Int) = "checklist_execution/$checklistId/$vehicleId"
    }
    
    data object ChecklistDetail : Screen("checklist_detail/{checklistId}") {
        fun createRoute(checklistId: Int) = "checklist_detail/$checklistId"
    }
    
    data object TuvDetail : Screen("tuv_detail/{tuvId}") {
        fun createRoute(tuvId: Int) = "tuv_detail/$tuvId"
    }
    
    // Admin screens (only accessible by admin users)
    data object AdminPanel : Screen("admin")
    data object ChecklistEditor : Screen("checklist_editor/{checklistId}") {
        fun createRoute(checklistId: Int) = "checklist_editor/$checklistId"
        fun createNewRoute() = "checklist_editor/new"
    }
    
    data object VehicleTypeManagement : Screen("vehicle_types")
    data object GroupManagement : Screen("groups")
}

/**
 * Navigation arguments
 */
object NavigationArgs {
    const val VEHICLE_ID = "vehicleId"
    const val CHECKLIST_ID = "checklistId"
    const val TUV_ID = "tuvId"
}