from fastapi import HTTPException, status
from ..models.user import Benutzer


def check_admin_permission(current_user: Benutzer):
    """Check if user has admin role"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    if user_role != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin Berechtigung erforderlich"
        )


def check_organisator_permission(current_user: Benutzer):
    """Check if user has organisator or admin role"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    if user_role not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )


def check_gruppenleiter_permission(current_user: Benutzer):
    """Check if user has gruppenleiter, organisator or admin role"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    if user_role not in ["gruppenleiter", "organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Gruppenleiter, Organisator oder Admin Berechtigung erforderlich"
        )


def check_write_permission(current_user: Benutzer):
    """Check if user can modify data (all roles except basic benutzer for some operations)"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    return user_role in ["gruppenleiter", "organisator", "admin"]


def get_user_permission_level(current_user: Benutzer) -> str:
    """Get user permission level as string"""
    role_hierarchy = {
        "benutzer": 1,
        "gruppenleiter": 2, 
        "organisator": 3,
        "admin": 4
    }
    return getattr(current_user, 'rolle', 'benutzer')


def can_access_resource(current_user: Benutzer, resource_owner_id: int, required_level: str = "benutzer") -> bool:
    """Check if user can access a resource based on ownership and role"""
    user_role = getattr(current_user, 'rolle', '')
    user_id = getattr(current_user, 'id', 0)
    
    # Admin can access everything
    if user_role == "admin":
        return True
    
    # Organisator can access most things
    if user_role == "organisator" and required_level in ["benutzer", "gruppenleiter"]:
        return True
        
    # Gruppenleiter can access benutzer level resources
    if user_role == "gruppenleiter" and required_level == "benutzer":
        return True
    
    # Users can access their own resources
    if user_id == resource_owner_id:
        return True
        
    return False


# Role hierarchy levels for easy comparison
ROLE_LEVELS = {
    "benutzer": 1,
    "gruppenleiter": 2,
    "organisator": 3, 
    "admin": 4
}


def has_role_level(current_user: Benutzer, required_level: str) -> bool:
    """Check if user has at least the required role level"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    user_level = ROLE_LEVELS.get(user_role, 0)
    required = ROLE_LEVELS.get(required_level, 0)
    return user_level >= required


def check_checklist_edit_permission(current_user: Benutzer):
    """Check if user can edit checklists (Organisator role or higher)"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    if user_role not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Nur Benutzer in der Organisator-Gruppe oder Administratoren können Checklisten bearbeiten"
        )


def can_edit_checklist_item(current_user: Benutzer, item_editable_roles: list) -> bool:
    """Check if user can edit a specific checklist item based on its editable roles"""
    if not item_editable_roles:
        # Default to Organisator if no specific roles defined
        item_editable_roles = ["organisator", "admin"]
    
    user_role = getattr(current_user, 'rolle', 'benutzer')
    return user_role in item_editable_roles


def check_item_edit_permission(current_user: Benutzer, item_editable_roles: list):
    """Check if user can edit a specific checklist item"""
    if not can_edit_checklist_item(current_user, item_editable_roles):
        roles_str = ", ".join(item_editable_roles or ["organisator", "admin"])
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Nur Benutzer in folgenden Gruppen können diesen Punkt bearbeiten: {roles_str}"
        )


def check_template_creation_permission(current_user: Benutzer):
    """Check if user can create checklist templates"""
    user_role = getattr(current_user, 'rolle', 'benutzer')
    if user_role not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Nur Organisator oder Admin können Checklisten-Templates erstellen"
        )
