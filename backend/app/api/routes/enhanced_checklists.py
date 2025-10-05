"""
Enhanced Checklist API Routes

Routes for managing enhanced checklists with different item types,
validation rules, and role-based editing permissions.
"""

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional, List, Dict, Any
from datetime import datetime

from ...db.session import get_db
from ...models.checklist import (
    Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis, ChecklistItemTypeEnum
)
from ...models.vehicle import Fahrzeug, FahrzeugGruppe
from ...models.user import Benutzer
from ...schemas.checklist import (
    ChecklistItemCreate, ChecklistItemUpdate, ChecklistItem as ChecklistItemSchema,
    ItemErgebnisCreate, ItemErgebnisUpdate, ItemErgebnis as ItemErgebnisSchema,
    AtemschutzErgebnis, RatingErgebnis, PercentageErgebnis
)
from ...core.deps import get_current_user
from ...core.permissions import (
    check_organisator_permission, 
    check_checklist_edit_permission,
    can_edit_checklist_item,
    check_item_edit_permission,
    check_template_creation_permission
)

router = APIRouter()


@router.post("/items", response_model=ChecklistItemSchema)
def create_enhanced_checklist_item(
    item_data: ChecklistItemCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new enhanced checklist item with type-specific validation"""
    
    # Check if user has permission to edit checklists
    check_checklist_edit_permission(current_user)
    
    # Verify the checklist exists
    checklist = db.get(Checkliste, item_data.checkliste_id)
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Create the item
    db_item = ChecklistItem(**item_data.model_dump())
    db.add(db_item)
    
    try:
        db.commit()
        db.refresh(db_item)
        return db_item
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fehler beim Erstellen des Checklistenpunkts"
        )


@router.put("/items/{item_id}", response_model=ChecklistItemSchema)
def update_enhanced_checklist_item(
    item_id: int,
    item_data: ChecklistItemUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update an enhanced checklist item"""
    
    # Check if user has permission to edit checklists
    check_checklist_edit_permission(current_user)
    
    # Get the item
    db_item = db.get(ChecklistItem, item_id)
    if not db_item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checklistenpunkt nicht gefunden"
        )
    
    # Check if user can edit this specific item type
    item_editable_roles = getattr(db_item, 'editable_roles', None) or ["organisator", "admin"]
    check_item_edit_permission(current_user, item_editable_roles)
    
    # Update the item
    update_data = item_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_item, field, value)
    
    try:
        db.commit()
        db.refresh(db_item)
        return db_item
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fehler beim Aktualisieren des Checklistenpunkts"
        )


@router.post("/executions/{execution_id}/results", response_model=ItemErgebnisSchema)
def create_enhanced_item_result(
    execution_id: int,
    result_data: ItemErgebnisCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create or update an item result with enhanced validation"""
    
    # Get the execution
    execution = db.get(ChecklistAusfuehrung, execution_id)
    if not execution:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checklistenausführung nicht gefunden"
        )
    
    # Check permissions - user must be assigned to execution or have admin/organisator role
    execution_benutzer_id = getattr(execution, 'benutzer_id', 0)
    current_user_id = getattr(current_user, 'id', 0)
    current_user_rolle = getattr(current_user, 'rolle', 'benutzer')
    if (execution_benutzer_id != current_user_id and 
        current_user_rolle not in ["organisator", "admin"]):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Keine Berechtigung für diese Checklistenausführung"
        )
    
    # Get the item to validate result format
    item = db.get(ChecklistItem, result_data.item_id)
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checklistenpunkt nicht gefunden"
        )
    
    # Validate result data based on item type
    validation_error = validate_item_result(item, result_data)
    if validation_error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=validation_error
        )
    
    # Check if result already exists
    existing_result = db.query(ItemErgebnis).filter(
        ItemErgebnis.ausfuehrung_id == execution_id,
        ItemErgebnis.item_id == result_data.item_id
    ).first()
    
    if existing_result:
        # Update existing result
        result_dict = result_data.model_dump(exclude={'item_id'})
        for field, value in result_dict.items():
            if value is not None:
                setattr(existing_result, field, value)
        
        db_result = existing_result
    else:
        # Create new result
        db_result = ItemErgebnis(
            ausfuehrung_id=execution_id,
            **result_data.model_dump()
        )
        db.add(db_result)
    
    try:
        db.commit()
        db.refresh(db_result)
        return db_result
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fehler beim Speichern des Ergebnisses"
        )


@router.get("/items/{item_id}/validation", response_model=Dict[str, Any])
def get_item_validation_info(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get validation information for a checklist item"""
    
    item = db.get(ChecklistItem, item_id)
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checklistenpunkt nicht gefunden"
        )
    
    validation_info = {
        "item_type": item.item_type.value if item.item_type else "standard",
        "validation_config": item.validation_config or {},
        "requires_tuv": item.requires_tuv,
        "subcategories": item.subcategories or {},
        "editable_by_current_user": can_edit_checklist_item(
            current_user, 
            item.editable_roles or ["organisator", "admin"]
        ),
        "editable_roles": item.editable_roles or ["organisator", "admin"]
    }
    
    return validation_info


def validate_item_result(item: ChecklistItem, result_data: ItemErgebnisCreate) -> Optional[str]:
    """Validate item result based on item type and configuration"""
    
    if not item.item_type:
        return None
    
    validation_config = item.validation_config or {}
    
    try:
        if item.item_type == ChecklistItemTypeEnum.RATING_1_6:
            if result_data.wert is not None:
                rating = int(result_data.wert)
                min_val = validation_config.get('min_value', 1)
                max_val = validation_config.get('max_value', 6)
                if not (min_val <= rating <= max_val):
                    return f"Bewertung muss zwischen {min_val} und {max_val} liegen"
        
        elif item.item_type == ChecklistItemTypeEnum.PERCENTAGE:
            if result_data.wert is not None:
                percentage = float(result_data.wert)
                min_val = validation_config.get('min_value', 0)
                max_val = validation_config.get('max_value', 100)
                if not (min_val <= percentage <= max_val):
                    return f"Prozentwert muss zwischen {min_val}% und {max_val}% liegen"
        
        elif item.item_type == ChecklistItemTypeEnum.ATEMSCHUTZ:
            if result_data.wert and isinstance(result_data.wert, dict):
                # Validate Atemschutz structure
                required_fields = validation_config.get('required_fields', [])
                for field in required_fields:
                    if field not in result_data.wert:
                        return f"Pflichtfeld fehlt: {field}"
        
        elif item.item_type == ChecklistItemTypeEnum.QUANTITY:
            if result_data.menge is not None:
                min_val = validation_config.get('min_value', 0)
                max_val = validation_config.get('max_value', 999)
                if not (min_val <= result_data.menge <= max_val):
                    return f"Anzahl muss zwischen {min_val} und {max_val} liegen"
        
        elif item.item_type == ChecklistItemTypeEnum.VEHICLE_INFO:
            # Vehicle info should not be editable
            return "Fahrzeugdaten können nicht bearbeitet werden"
        
        elif item.item_type == ChecklistItemTypeEnum.STATUS_CHECK:
            if result_data.status:
                allowed_values = validation_config.get('allowed_values', ['ok', 'fehler', 'nicht_pruefbar'])
                if result_data.status not in allowed_values:
                    return f"Status muss einer der folgenden Werte sein: {', '.join(allowed_values)}"
        
        elif item.item_type == ChecklistItemTypeEnum.DATE_CHECK:
            if result_data.tuv_datum and validation_config.get('required', True):
                # Check if TÜV date is in the future
                if result_data.tuv_datum < datetime.now():
                    return "TÜV-Datum liegt in der Vergangenheit"
        
        elif item.item_type == ChecklistItemTypeEnum.STANDARD:
            required_fields = validation_config.get('required_fields', [])
            if 'vorhanden' in required_fields and result_data.vorhanden is None:
                return "Angabe erforderlich: Ist das Element vorhanden?"
            
    except (ValueError, TypeError) as e:
        return f"Ungültiger Wert: {str(e)}"
    
    return None


@router.get("/types", response_model=Dict[str, Any])
def get_item_types():
    """Get available checklist item types and their configurations"""
    
    item_types = {}
    
    for item_type in ChecklistItemTypeEnum:
        if item_type == ChecklistItemTypeEnum.VEHICLE_INFO:
            config = {
                "label": "Fahrzeugdaten",
                "description": "Schreibgeschützte Fahrzeuginformationen",
                "editable": False,
                "fields": ["fahrzeug", "kennzeichen"]
            }
        elif item_type == ChecklistItemTypeEnum.RATING_1_6:
            config = {
                "label": "Bewertung (1-6)",
                "description": "Fächer-Bewertung von 1 (schlecht) bis 6 (sehr gut)",
                "editable": True,
                "input_type": "rating",
                "min_value": 1,
                "max_value": 6
            }
        elif item_type == ChecklistItemTypeEnum.PERCENTAGE:
            config = {
                "label": "Prozentwert",
                "description": "Kraftstoffstand oder ähnliche Prozentwerte",
                "editable": True,
                "input_type": "percentage",
                "min_value": 0,
                "max_value": 100
            }
        elif item_type == ChecklistItemTypeEnum.ATEMSCHUTZ:
            config = {
                "label": "Atemschutzgeräte",
                "description": "Komplexe Atemschutzprüfung mit TÜV-Daten",
                "editable": True,
                "input_type": "complex",
                "subcategories": ["TÜV Platte", "TÜV RespiHood", "PA Geräte"]
            }
        elif item_type == ChecklistItemTypeEnum.QUANTITY:
            config = {
                "label": "Anzahl",
                "description": "Stückzahl von Ausrüstungsgegenständen",
                "editable": True,
                "input_type": "number",
                "min_value": 0,
                "max_value": 999
            }
        elif item_type == ChecklistItemTypeEnum.DATE_CHECK:
            config = {
                "label": "TÜV-Datum",
                "description": "Prüfung von TÜV-Ablaufdaten",
                "editable": True,
                "input_type": "date",
                "requires_tuv": True
            }
        elif item_type == ChecklistItemTypeEnum.STATUS_CHECK:
            config = {
                "label": "Status",
                "description": "Einfache Status-Prüfung",
                "editable": True,
                "input_type": "select",
                "options": ["ok", "fehler", "nicht_pruefbar"]
            }
        else:  # STANDARD
            config = {
                "label": "Standard",
                "description": "Standard-Prüfung mit Vorhanden/TÜV-Status",
                "editable": True,
                "input_type": "standard",
                "fields": ["vorhanden", "tuv_status"]
            }
        
        item_types[item_type.value] = config
    
    return {
        "item_types": item_types,
        "editable_roles": ["organisator", "admin"],
        "readonly_roles": ["benutzer", "gruppenleiter"]
    }


@router.post("/templates", response_model=Dict[str, Any])
def create_template_from_enhanced_items(
    template_data: Dict[str, Any],
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new checklist template with enhanced item types - Organisator+ only"""
    
    # Check if user has permission to create templates
    check_template_creation_permission(current_user)
    
    try:
        # Extract template information
        template_name = template_data.get("name", "Neues Template")
        fahrzeuggruppe_id = template_data.get("fahrzeuggruppe_id")
        items_data = template_data.get("items", [])
        
        if not fahrzeuggruppe_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeuggruppe erforderlich für Template-Erstellung"
            )
        
        # Verify fahrzeuggruppe exists
        fahrzeuggruppe = db.get(FahrzeugGruppe, fahrzeuggruppe_id)
        if not fahrzeuggruppe:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Fahrzeuggruppe nicht gefunden"
            )
        
        # Create the template
        template = Checkliste(
            name=template_name,
            fahrzeuggruppe_id=fahrzeuggruppe_id,
            template=True,  # This is a template
            ersteller_id=current_user.id
        )
        
        db.add(template)
        db.flush()  # Get the ID
        
        # Create enhanced items
        created_items = []
        for i, item_data in enumerate(items_data):
            item = ChecklistItem(
                checkliste_id=template.id,
                beschreibung=item_data.get("beschreibung", f"Item {i+1}"),
                item_type=ChecklistItemTypeEnum(item_data.get("item_type", "standard")),
                validation_config=item_data.get("validation_config"),
                editable_roles=item_data.get("editable_roles", ["organisator", "admin"]),
                requires_tuv=item_data.get("requires_tuv", False),
                subcategories=item_data.get("subcategories"),
                pflicht=item_data.get("pflicht", True),
                reihenfolge=item_data.get("reihenfolge", i * 10)
            )
            
            db.add(item)
            created_items.append(item)
        
        db.commit()
        db.refresh(template)
        
        return {
            "message": f"Template '{template_name}' erfolgreich erstellt",
            "template": {
                "id": template.id,
                "name": template.name,
                "fahrzeuggruppe_id": template.fahrzeuggruppe_id,
                "item_count": len(created_items),
                "ersteller_id": template.ersteller_id,
                "created_at": template.created_at.isoformat()
            },
            "items": [
                {
                    "id": item.id,
                    "beschreibung": item.beschreibung,
                    "item_type": item.item_type.value if item.item_type else "standard",
                    "requires_tuv": item.requires_tuv,
                    "pflicht": item.pflicht
                } for item in created_items
            ]
        }
        
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Ungültiger item_type: {str(e)}"
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Fehler beim Erstellen des Templates: {str(e)}"
        )