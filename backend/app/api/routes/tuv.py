from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional
from datetime import datetime, timedelta

from ...db.session import get_db
from ...models.checklist import TuvTermin
from ...models.vehicle import Fahrzeug
from ...models.user import Benutzer
from ...schemas.tuv import (
    TuvTermin as TuvTerminSchema, TuvTerminCreate, TuvTerminUpdate, 
    TuvTerminList, TuvTerminWithFahrzeug
)
from ...core.deps import get_current_user

router = APIRouter()


def calculate_tuv_status(ablauf_datum: datetime) -> str:
    """Calculate TÜV status based on expiration date"""
    now = datetime.now()
    days_remaining = (ablauf_datum - now).days
    
    if days_remaining < 0:
        return "expired"
    elif days_remaining <= 30:
        return "warning"
    else:
        return "current"


def check_write_permission(current_user: Benutzer):
    """Check if user can create/modify TÜV records"""
    if current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )


@router.get("/deadlines", response_model=TuvTerminList)
def list_deadlines(
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    fahrzeug_id: Optional[int] = Query(None, description="Filter by vehicle ID"),
    status: Optional[str] = Query(None, description="Filter by status: current, warning, expired"),
    kennzeichen: Optional[str] = Query(None, description="Filter by vehicle license plate"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List TÜV deadlines with filtering and pagination"""
    offset = (page - 1) * per_page
    
    query = db.query(TuvTermin).join(Fahrzeug)
    
    # Apply filters
    if fahrzeug_id:
        query = query.filter(TuvTermin.fahrzeug_id == fahrzeug_id)
    if kennzeichen:
        query = query.filter(Fahrzeug.kennzeichen.ilike(f"%{kennzeichen}%"))
    
    # Get all records first to calculate status
    all_termine = query.all()
    
    # Update status based on current date
    for termin in all_termine:
        ablauf_datum = getattr(termin, 'ablauf_datum', None)
        current_status = getattr(termin, 'status', '')
        
        if ablauf_datum:
            new_status = calculate_tuv_status(ablauf_datum)
            if current_status != new_status:
                setattr(termin, 'status', new_status)
    
    db.commit()
    
    # Apply status filter after updating
    if status:
        all_termine = [t for t in all_termine if getattr(t, 'status', '') == status]
    
    total = len(all_termine)
    termine = all_termine[offset:offset + per_page]
    
    return TuvTerminList(
        items=[TuvTerminSchema.model_validate(termin) for termin in termine],
        total=total,
        page=page,
        per_page=per_page,
        total_pages=(total + per_page - 1) // per_page
    )


@router.post("/deadlines", response_model=TuvTerminSchema, status_code=status.HTTP_201_CREATED)
def create_deadline(
    termin_data: TuvTerminCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new TÜV deadline record"""
    check_write_permission(current_user)
    
    # Check if vehicle exists
    vehicle = db.get(Fahrzeug, termin_data.fahrzeug_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeug nicht gefunden"
        )
    
    # Check if there's already an active TÜV record for this vehicle
    existing = db.query(TuvTermin).filter(
        TuvTermin.fahrzeug_id == termin_data.fahrzeug_id
    ).first()
    
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="TÜV-Termin für dieses Fahrzeug bereits vorhanden"
        )
    
    # Calculate initial status
    initial_status = calculate_tuv_status(termin_data.ablauf_datum)
    
    db_termin = TuvTermin(
        **termin_data.model_dump(),
        status=initial_status
    )
    
    db.add(db_termin)
    db.commit()
    db.refresh(db_termin)
    return TuvTerminSchema.model_validate(db_termin)


@router.get("/deadlines/{termin_id}", response_model=TuvTerminWithFahrzeug)
def get_deadline(
    termin_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get TÜV deadline by ID with vehicle information"""
    termin = db.query(TuvTermin).join(Fahrzeug).filter(
        TuvTermin.id == termin_id
    ).first()
    
    if not termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="TÜV-Termin nicht gefunden"
        )
    
    # Update status
    termin.status = calculate_tuv_status(termin.ablauf_datum)
    db.commit()
    
    # Get vehicle info for response
    vehicle = db.get(Fahrzeug, termin.fahrzeug_id)
    
    termin_dict = TuvTerminSchema.model_validate(termin).model_dump()
    termin_dict["fahrzeug"] = {
        "id": vehicle.id,
        "kennzeichen": vehicle.kennzeichen,
        "typ": vehicle.typ
    }
    
    return TuvTerminWithFahrzeug.model_validate(termin_dict)


@router.put("/deadlines/{termin_id}", response_model=TuvTerminSchema)
def update_deadline(
    termin_id: int,
    termin_data: TuvTerminUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update TÜV deadline"""
    check_write_permission(current_user)
    
    termin = db.get(TuvTermin, termin_id)
    if not termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="TÜV-Termin nicht gefunden"
        )
    
    # Check if vehicle exists if fahrzeug_id is being updated
    if termin_data.fahrzeug_id and termin_data.fahrzeug_id != termin.fahrzeug_id:
        vehicle = db.get(Fahrzeug, termin_data.fahrzeug_id)
        if not vehicle:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeug nicht gefunden"
            )
    
    update_data = termin_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(termin, field, value)
    
    # Recalculate status if ablauf_datum changed
    if termin_data.ablauf_datum:
        termin.status = calculate_tuv_status(termin.ablauf_datum)
    
    db.commit()
    db.refresh(termin)
    return TuvTerminSchema.model_validate(termin)


@router.delete("/deadlines/{termin_id}")
def delete_deadline(
    termin_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete TÜV deadline"""
    check_write_permission(current_user)
    
    termin = db.get(TuvTermin, termin_id)
    if not termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="TÜV-Termin nicht gefunden"
        )
    
    db.delete(termin)
    db.commit()
    return {"detail": "TÜV-Termin gelöscht"}


@router.get("/vehicle/{vehicle_id}", response_model=TuvTerminSchema)
def get_deadline_for_vehicle(
    vehicle_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get TÜV deadline for a specific vehicle"""
    # Check if vehicle exists
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    termin = db.query(TuvTermin).filter(
        TuvTermin.fahrzeug_id == vehicle_id
    ).first()
    
    if not termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Kein TÜV-Termin für dieses Fahrzeug gefunden"
        )
    
    # Update status
    termin.status = calculate_tuv_status(termin.ablauf_datum)
    db.commit()
    
    return TuvTerminSchema.model_validate(termin)


@router.get("/alerts/upcoming")
def get_alerts(
    days_ahead: int = Query(30, ge=1, le=365, description="Days to look ahead for warnings"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get upcoming TÜV deadlines and expired ones"""
    cutoff_date = datetime.now() + timedelta(days=days_ahead)
    
    alerts = db.query(TuvTermin).join(Fahrzeug).filter(
        TuvTermin.ablauf_datum <= cutoff_date
    ).all()
    
    # Update statuses and categorize
    expired = []
    warning = []
    
    for termin in alerts:
        termin.status = calculate_tuv_status(termin.ablauf_datum)
        vehicle = db.get(Fahrzeug, termin.fahrzeug_id)
        
        alert_data = {
            "termin_id": termin.id,
            "fahrzeug_id": termin.fahrzeug_id,
            "kennzeichen": vehicle.kennzeichen,
            "typ": vehicle.typ,
            "ablauf_datum": termin.ablauf_datum.isoformat(),
            "status": termin.status,
            "tage_verbleibend": (termin.ablauf_datum - datetime.now()).days
        }
        
        if termin.status == "expired":
            expired.append(alert_data)
        elif termin.status == "warning":
            warning.append(alert_data)
    
    db.commit()
    
    return {
        "expired": expired,
        "warning": warning,
        "total_alerts": len(expired) + len(warning)
    }
