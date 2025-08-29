from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import Dict, Any
import json
from datetime import datetime

from ...db.session import get_db
from ...models.user import Benutzer
from ...models.checklist import (
    Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis
)
from ...models.vehicle import Fahrzeug
from ...schemas.sync import SyncBatchRequest, SyncBatchResponse
from ...core.deps import get_current_user

router = APIRouter()


def process_sync_action(action_data: Dict[str, Any], db: Session, current_user: Benutzer) -> Dict[str, Any]:
    """Process a single sync action and return result"""
    try:
        action_type = action_data.get("action_type")
        resource_type = action_data.get("resource_type")
        data = action_data.get("data", {})
        
        if action_type == "create_checklist_run":
            # Start a checklist execution
            checklist_id = data.get("checklist_id")
            fahrzeug_id = data.get("fahrzeug_id")
            
            # Validate checklist exists
            checklist = db.get(Checkliste, checklist_id)
            if not checklist:
                return {"success": False, "error": "Checkliste nicht gefunden"}
            
            # Validate vehicle exists
            vehicle = db.get(Fahrzeug, fahrzeug_id)
            if not vehicle:
                return {"success": False, "error": "Fahrzeug nicht gefunden"}
            
            # Check for existing active run
            existing_run = db.query(ChecklistAusfuehrung).filter(
                ChecklistAusfuehrung.checkliste_id == checklist_id,
                ChecklistAusfuehrung.fahrzeug_id == fahrzeug_id,
                ChecklistAusfuehrung.status == "started"
            ).first()
            
            if existing_run:
                return {"success": True, "resource_id": existing_run.id, "message": "Bereits aktive Durchführung"}
            
            # Create new run
            new_run = ChecklistAusfuehrung(
                checkliste_id=checklist_id,
                fahrzeug_id=fahrzeug_id,
                benutzer_id=current_user.id
            )
            db.add(new_run)
            db.flush()
            
            return {"success": True, "resource_id": new_run.id}
        
        elif action_type == "update_item_result":
            # Update or create item result
            run_id = data.get("run_id")
            item_id = data.get("item_id")
            item_status = data.get("status")
            kommentar = data.get("kommentar")
            
            # Validate run exists and is active
            run = db.get(ChecklistAusfuehrung, run_id)
            if not run:
                return {"success": False, "error": "Durchführung nicht gefunden"}
            
            if run.status != "started":
                return {"success": False, "error": "Durchführung ist nicht aktiv"}
            
            # Validate item belongs to this checklist
            item = db.query(ChecklistItem).filter(
                ChecklistItem.id == item_id,
                ChecklistItem.checkliste_id == run.checkliste_id
            ).first()
            
            if not item:
                return {"success": False, "error": "Item gehört nicht zu dieser Checkliste"}
            
            # Find or create result
            result = db.query(ItemErgebnis).filter(
                ItemErgebnis.ausfuehrung_id == run_id,
                ItemErgebnis.item_id == item_id
            ).first()
            
            if result:
                # Update existing result
                result.status = item_status
                result.kommentar = kommentar
            else:
                # Create new result
                result = ItemErgebnis(
                    ausfuehrung_id=run_id,
                    item_id=item_id,
                    status=item_status,
                    kommentar=kommentar
                )
                db.add(result)
            
            db.flush()
            return {"success": True, "resource_id": result.id}
        
        elif action_type == "complete_checklist_run":
            # Complete a checklist execution
            run_id = data.get("run_id")
            
            run = db.get(ChecklistAusfuehrung, run_id)
            if not run:
                return {"success": False, "error": "Durchführung nicht gefunden"}
            
            if run.status != "started":
                return {"success": False, "error": "Durchführung ist nicht aktiv"}
            
            # Check if user has permission
            if run.benutzer_id != current_user.id and current_user.rolle not in ["organisator", "admin"]:
                return {"success": False, "error": "Keine Berechtigung"}
            
            run.status = "completed"
            run.completed_at = datetime.now()
            
            return {"success": True, "resource_id": run_id}
        
        elif action_type == "create_checklist":
            # Create a new checklist (admin/organisator only)
            if current_user.rolle not in ["organisator", "admin"]:
                return {"success": False, "error": "Keine Berechtigung"}
            
            name = data.get("name")
            fahrzeuggruppe_id = data.get("fahrzeuggruppe_id")
            template = data.get("template", False)
            items_data = data.get("items", [])
            
            # Create checklist
            checklist = Checkliste(
                name=name,
                fahrzeuggruppe_id=fahrzeuggruppe_id,
                template=template,
                ersteller_id=current_user.id
            )
            db.add(checklist)
            db.flush()
            
            # Create items
            for item_data in items_data:
                item = ChecklistItem(
                    checkliste_id=checklist.id,
                    beschreibung=item_data.get("beschreibung"),
                    pflicht=item_data.get("pflicht", True),
                    reihenfolge=item_data.get("reihenfolge", 0)
                )
                db.add(item)
            
            return {"success": True, "resource_id": checklist.id}
        
        else:
            return {"success": False, "error": f"Unbekannter Action-Typ: {action_type}"}
            
    except Exception as e:
        return {"success": False, "error": str(e)}


@router.post("/actions", response_model=SyncBatchResponse)
def push_actions(
    sync_request: SyncBatchRequest,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Process batch of offline sync actions"""
    
    processed = 0
    failed = 0
    errors = []
    
    try:
        for action in sync_request.actions:
            action_data = action.model_dump()
            result = process_sync_action(action_data, db, current_user)
            
            if result.get("success"):
                processed += 1
            else:
                failed += 1
                errors.append({
                    "action_type": action_data.get("action_type"),
                    "error": result.get("error"),
                    "timestamp": action_data.get("timestamp")
                })
        
        # Commit all changes if any were successful
        if processed > 0:
            db.commit()
        else:
            db.rollback()
            
    except Exception as e:
        db.rollback()
        return SyncBatchResponse(
            processed=0,
            failed=len(sync_request.actions),
            errors=[{"error": f"Batch processing failed: {str(e)}"}]
        )
    
    return SyncBatchResponse(
        processed=processed,
        failed=failed,
        errors=errors
    )


@router.get("/status")
def get_sync_status(current_user: Benutzer = Depends(get_current_user)):
    """Get sync status and server time"""
    return {
        "server_time": datetime.now().isoformat(),
        "user_id": current_user.id,
        "sync_enabled": True
    }


@router.post("/test")
def test_sync_connectivity(current_user: Benutzer = Depends(get_current_user)):
    """Test sync connectivity"""
    return {
        "status": "ok",
        "user": current_user.username,
        "server_time": datetime.now().isoformat()
    }
