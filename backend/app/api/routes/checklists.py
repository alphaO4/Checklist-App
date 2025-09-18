from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional, List

from ...db.session import get_db
from ...models.checklist import (
    Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis
)
from ...models.vehicle import Fahrzeug, FahrzeugGruppe
from ...models.user import Benutzer
from ...schemas.checklist import (
    Checkliste as ChecklisteSchema, ChecklisteCreate, ChecklisteUpdate, ChecklisteList,
    ChecklisteWithItems, ChecklistItem as ChecklistItemSchema,
    ChecklistAusfuehrung as ChecklistAusfuehrungSchema, ChecklistAusfuehrungCreate,
    ItemErgebnis as ItemErgebnisSchema, ItemErgebnisCreate, ItemErgebnisUpdate
)
from ...core.deps import get_current_user

router = APIRouter()


def check_write_permission(current_user: Benutzer):
    """Check if user can create/modify checklists"""
    if current_user.rolle not in ["gruppenleiter", "organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Gruppenleiter, Organisator oder Admin Berechtigung erforderlich"
        )


@router.get("", response_model=ChecklisteList)
def list_checklists(
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    fahrzeuggruppe_id: Optional[int] = Query(None, description="Filter by vehicle group"),
    template: Optional[bool] = Query(None, description="Filter templates only"),
    name: Optional[str] = Query(None, description="Filter by name"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List checklists with filtering and pagination"""
    offset = (page - 1) * per_page
    
    query = db.query(Checkliste)
    
    # Apply filters
    if fahrzeuggruppe_id:
        query = query.filter(Checkliste.fahrzeuggruppe_id == fahrzeuggruppe_id)
    if template is not None:
        query = query.filter(Checkliste.template == template)
    if name:
        query = query.filter(Checkliste.name.ilike(f"%{name}%"))
    
    total = query.count()
    checklists = query.offset(offset).limit(per_page).all()
    
    return ChecklisteList(
        items=[ChecklisteSchema.model_validate(checklist) for checklist in checklists],
        total=total,
        page=page,
        per_page=per_page,
        total_pages=(total + per_page - 1) // per_page
    )


@router.post("", response_model=ChecklisteWithItems, status_code=status.HTTP_201_CREATED)
def create_checklist(
    checklist_data: ChecklisteCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new checklist with items"""
    check_write_permission(current_user)
    
    # Check if fahrzeuggruppe exists
    fahrzeuggruppe = db.get(FahrzeugGruppe, checklist_data.fahrzeuggruppe_id)
    if not fahrzeuggruppe:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeuggruppe nicht gefunden"
        )
    
    # Create checklist
    db_checklist = Checkliste(
        name=checklist_data.name,
        fahrzeuggruppe_id=checklist_data.fahrzeuggruppe_id,
        template=checklist_data.template,
        ersteller_id=current_user.id
    )
    
    db.add(db_checklist)
    db.flush()  # Get the ID without committing
    
    # Create items if provided
    items = []
    if checklist_data.items:
        for item_data in checklist_data.items:
            db_item = ChecklistItem(
                checkliste_id=db_checklist.id,
                **item_data.model_dump()
            )
            db.add(db_item)
            items.append(db_item)
    
    db.commit()
    db.refresh(db_checklist)
    
    # Create response with only the newly created items to avoid validation issues
    checklist_dict = {
        "id": db_checklist.id,
        "name": db_checklist.name,
        "fahrzeuggruppe_id": db_checklist.fahrzeuggruppe_id,
        "template": db_checklist.template,
        "ersteller_id": db_checklist.ersteller_id,
        "created_at": db_checklist.created_at,
        "items": [ChecklistItemSchema.model_validate(item) for item in items]
    }
    
    return ChecklisteWithItems.model_validate(checklist_dict)


@router.get("/{checklist_id}", response_model=ChecklisteWithItems)
def get_checklist(
    checklist_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get checklist by ID with items"""
    checklist = db.query(Checkliste).filter(Checkliste.id == checklist_id).first()
    
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Get items
    items = db.query(ChecklistItem).filter(
        ChecklistItem.checkliste_id == checklist_id
    ).order_by(ChecklistItem.reihenfolge).all()
    
    result = ChecklisteWithItems.model_validate(checklist)
    result.items = [ChecklistItemSchema.model_validate(item) for item in items]
    
    return result


@router.put("/{checklist_id}", response_model=ChecklisteSchema)
def update_checklist(
    checklist_id: int,
    checklist_data: ChecklisteUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update checklist"""
    check_write_permission(current_user)
    
    checklist = db.get(Checkliste, checklist_id)
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Check if fahrzeuggruppe exists if provided
    if checklist_data.fahrzeuggruppe_id:
        fahrzeuggruppe = db.get(FahrzeugGruppe, checklist_data.fahrzeuggruppe_id)
        if not fahrzeuggruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeuggruppe nicht gefunden"
            )
    
    update_data = checklist_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(checklist, field, value)
    
    db.commit()
    db.refresh(checklist)
    return ChecklisteSchema.model_validate(checklist)


@router.delete("/{checklist_id}")
def delete_checklist(
    checklist_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete checklist"""
    check_write_permission(current_user)
    
    checklist = db.get(Checkliste, checklist_id)
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Check if checklist has active runs
    active_runs = db.query(ChecklistAusfuehrung).filter(
        ChecklistAusfuehrung.checkliste_id == checklist_id,
        ChecklistAusfuehrung.status == "started"
    ).count()
    
    if active_runs > 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Kann Checkliste mit aktiven Durchführungen nicht löschen"
        )
    
    db.delete(checklist)
    db.commit()
    return {"detail": "Checkliste gelöscht"}


# Checklist execution routes
@router.post("/{checklist_id}/runs", response_model=ChecklistAusfuehrungSchema, status_code=status.HTTP_201_CREATED)
def start_checklist_run(
    checklist_id: int,
    run_data: ChecklistAusfuehrungCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Start a new checklist execution"""
    # Check if checklist exists
    checklist = db.get(Checkliste, checklist_id)
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Check if vehicle exists
    vehicle = db.get(Fahrzeug, run_data.fahrzeug_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeug nicht gefunden"
        )
    
    # Check if there's already an active run for this checklist and vehicle
    existing_run = db.query(ChecklistAusfuehrung).filter(
        ChecklistAusfuehrung.checkliste_id == checklist_id,
        ChecklistAusfuehrung.fahrzeug_id == run_data.fahrzeug_id,
        ChecklistAusfuehrung.status == "started"
    ).first()
    
    if existing_run:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Aktive Durchführung für diese Kombination bereits vorhanden"
        )
    
    db_run = ChecklistAusfuehrung(
        checkliste_id=checklist_id,
        fahrzeug_id=run_data.fahrzeug_id,
        benutzer_id=current_user.id
    )
    
    db.add(db_run)
    db.commit()
    db.refresh(db_run)
    
    return ChecklistAusfuehrungSchema.model_validate(db_run)


@router.get("/{checklist_id}/runs")
def list_checklist_runs(
    checklist_id: int,
    fahrzeug_id: Optional[int] = Query(None),
    status: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List executions for a checklist"""
    query = db.query(ChecklistAusfuehrung).filter(
        ChecklistAusfuehrung.checkliste_id == checklist_id
    )
    
    if fahrzeug_id:
        query = query.filter(ChecklistAusfuehrung.fahrzeug_id == fahrzeug_id)
    if status:
        query = query.filter(ChecklistAusfuehrung.status == status)
    
    runs = query.all()
    return [ChecklistAusfuehrungSchema.model_validate(run) for run in runs]


@router.post("/runs/{run_id}/items", response_model=ItemErgebnisSchema, status_code=status.HTTP_201_CREATED)
def record_item_result(
    run_id: int,
    result_data: ItemErgebnisCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Record result for a checklist item"""
    # Check if run exists and is active
    run = db.get(ChecklistAusfuehrung, run_id)
    if not run:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Durchführung nicht gefunden"
        )
    
    if run.status != "started":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Durchführung ist nicht aktiv"
        )
    
    # Check if user has permission to modify this run
    if run.benutzer_id != current_user.id and current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Keine Berechtigung diese Durchführung zu bearbeiten"
        )
    
    # Check if item exists in this checklist
    item = db.query(ChecklistItem).filter(
        ChecklistItem.id == result_data.item_id,
        ChecklistItem.checkliste_id == run.checkliste_id
    ).first()
    
    if not item:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Item gehört nicht zu dieser Checkliste"
        )
    
    # Validate status
    valid_statuses = ["ok", "fehler", "nicht_pruefbar"]
    if result_data.status not in valid_statuses:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Ungültiger Status. Erlaubt: {', '.join(valid_statuses)}"
        )
    
    # Check if result already exists
    existing_result = db.query(ItemErgebnis).filter(
        ItemErgebnis.ausfuehrung_id == run_id,
        ItemErgebnis.item_id == result_data.item_id
    ).first()
    
    if existing_result:
        # Update existing result
        existing_result.status = result_data.status
        existing_result.kommentar = result_data.kommentar
        db.commit()
        db.refresh(existing_result)
        return ItemErgebnisSchema.model_validate(existing_result)
    else:
        # Create new result
        db_result = ItemErgebnis(
            ausfuehrung_id=run_id,
            **result_data.model_dump()
        )
        db.add(db_result)
        db.commit()
        db.refresh(db_result)
        return ItemErgebnisSchema.model_validate(db_result)


@router.put("/runs/{run_id}/complete")
def complete_checklist_run(
    run_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Mark checklist execution as completed"""
    run = db.get(ChecklistAusfuehrung, run_id)
    if not run:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Durchführung nicht gefunden"
        )
    
    if run.benutzer_id != current_user.id and current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Keine Berechtigung diese Durchführung zu bearbeiten"
        )
    
    if run.status != "started":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Durchführung ist nicht aktiv"
        )
    
    from datetime import datetime
    run.status = "completed"
    run.completed_at = datetime.now()
    
    db.commit()
    return {"detail": "Durchführung abgeschlossen"}


@router.get("/runs/{run_id}/results")
def get_run_results(
    run_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get all results for a checklist execution"""
    # Check if run exists
    run = db.get(ChecklistAusfuehrung, run_id)
    if not run:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Durchführung nicht gefunden"
        )
    
    results = db.query(ItemErgebnis).filter(
        ItemErgebnis.ausfuehrung_id == run_id
    ).all()
    
    return [ItemErgebnisSchema.model_validate(result) for result in results]


@router.post("/import-csv-templates")
def import_csv_templates(
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Import checklist templates from CSV files in checklists folder"""
    from ...services.checklist_parser import checklist_parser
    
    # Only organisator and admin can import templates
    if current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )
    
    try:
        created_templates = checklist_parser.create_checklist_templates(db)
        
        return {
            "message": f"Erfolgreich {len(created_templates)} Checklisten-Templates importiert",
            "templates": [
                {
                    "id": template.id,
                    "name": template.name,
                    "item_count": len([item for item in template.items])
                } for template in created_templates
            ]
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Fehler beim Import: {str(e)}"
        )


@router.get("/csv-summary")
def get_csv_summary(
    current_user: Benutzer = Depends(get_current_user)
):
    """Get summary of available CSV checklists"""
    from ...services.checklist_parser import checklist_parser
    
    try:
        summary = checklist_parser.get_checklist_summary()
        return summary
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Fehler beim Lesen der CSV-Dateien: {str(e)}"
        )


@router.get("/{checklist_id}/vehicles")
def get_checklist_vehicles(
    checklist_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get all vehicles that can use this checklist (through fahrzeuggruppe)"""
    # Check if checklist exists
    checklist = db.get(Checkliste, checklist_id)
    if not checklist:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Checkliste nicht gefunden"
        )
    
    # Get all vehicles in the same fahrzeuggruppe
    vehicles = db.query(Fahrzeug).filter(
        Fahrzeug.fahrzeuggruppe_id == checklist.fahrzeuggruppe_id
    ).options(joinedload(Fahrzeug.fahrzeugtyp)).all()
    
    # Get active executions for this checklist
    active_executions = db.query(ChecklistAusfuehrung).filter(
        ChecklistAusfuehrung.checkliste_id == checklist_id,
        ChecklistAusfuehrung.status == "started"
    ).all()
    
    execution_map = {exec.fahrzeug_id: exec.id for exec in active_executions}
    
    return {
        "checklist_id": checklist_id,
        "checklist_name": checklist.name,
        "fahrzeuggruppe_id": checklist.fahrzeuggruppe_id,
        "available_vehicles": [
            {
                "id": vehicle.id,
                "kennzeichen": vehicle.kennzeichen,
                "fahrzeugtyp": {
                    "id": vehicle.fahrzeugtyp.id,
                    "name": vehicle.fahrzeugtyp.name,
                    "beschreibung": vehicle.fahrzeugtyp.beschreibung
                } if vehicle.fahrzeugtyp else None,
                "is_active": vehicle.id in execution_map,
                "active_execution_id": execution_map.get(vehicle.id)
            } for vehicle in vehicles
        ]
    }
