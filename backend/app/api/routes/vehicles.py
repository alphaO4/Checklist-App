from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional
from datetime import datetime

from ...db.session import get_db
from ...models.vehicle import Fahrzeug, FahrzeugGruppe
from ...models.vehicle_type import FahrzeugTyp
from ...models.checklist import TuvTermin
from ...models.user import Benutzer
from ...schemas.vehicle import (
    Fahrzeug as FahrzeugSchema, FahrzeugCreate, FahrzeugUpdate, FahrzeugList, FahrzeugWithGroup
)
from ...schemas.tuv import TuvTerminCreate, TuvTerminUpdate
from ...core.deps import get_current_user

router = APIRouter()


def check_write_permission(current_user: Benutzer):
    """Check if user can create/modify vehicles"""
    if current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )


@router.get("", response_model=FahrzeugList)
def list_vehicles(
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    kennzeichen: Optional[str] = Query(None, description="Filter by kennzeichen"),
    fahrzeugtyp_id: Optional[int] = Query(None, description="Filter by vehicle type ID"),
    fahrzeuggruppe_id: Optional[int] = Query(None, description="Filter by vehicle group"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List all vehicles with optional filtering and pagination"""
    offset = (page - 1) * per_page
    
    query = db.query(Fahrzeug).options(joinedload(Fahrzeug.fahrzeugtyp))
    
    # Apply filters
    if kennzeichen:
        query = query.filter(Fahrzeug.kennzeichen.ilike(f"%{kennzeichen}%"))
    if fahrzeugtyp_id:
        query = query.filter(Fahrzeug.fahrzeugtyp_id == fahrzeugtyp_id)
    if fahrzeuggruppe_id:
        query = query.filter(Fahrzeug.fahrzeuggruppe_id == fahrzeuggruppe_id)
    
    total = query.count()
    vehicles = query.offset(offset).limit(per_page).all()
    
    return FahrzeugList(
        items=[FahrzeugSchema.model_validate(vehicle) for vehicle in vehicles],
        total=total,
        page=page,
        per_page=per_page,
        total_pages=(total + per_page - 1) // per_page
    )


@router.post("", response_model=FahrzeugSchema, status_code=status.HTTP_201_CREATED)
def create_vehicle(
    vehicle_data: FahrzeugCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new vehicle"""
    check_write_permission(current_user)
    
    # Check if fahrzeugtyp exists
    fahrzeugtyp = db.get(FahrzeugTyp, vehicle_data.fahrzeugtyp_id)
    if not fahrzeugtyp:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeugtyp nicht gefunden"
        )
    
    # Check if fahrzeuggruppe exists
    fahrzeuggruppe = db.get(FahrzeugGruppe, vehicle_data.fahrzeuggruppe_id)
    if not fahrzeuggruppe:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeuggruppe nicht gefunden"
        )
    
    try:
        db_vehicle = Fahrzeug(**vehicle_data.model_dump())
        db.add(db_vehicle)
        db.commit()
        db.refresh(db_vehicle)
        return FahrzeugSchema.model_validate(db_vehicle)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Kennzeichen bereits vergeben"
        )


@router.get("/{vehicle_id}", response_model=FahrzeugWithGroup)
def get_vehicle(
    vehicle_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get vehicle by ID with group information and TÜV data"""
    vehicle = db.query(Fahrzeug).options(
        joinedload(Fahrzeug.fahrzeuggruppe),
        joinedload(Fahrzeug.fahrzeugtyp),
        joinedload(Fahrzeug.tuv_termine)
    ).filter(
        Fahrzeug.id == vehicle_id
    ).first()
    
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    return FahrzeugWithGroup.model_validate(vehicle)


@router.put("/{vehicle_id}", response_model=FahrzeugSchema)
def update_vehicle(
    vehicle_id: int,
    vehicle_data: FahrzeugUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update vehicle"""
    check_write_permission(current_user)
    
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    # Check if fahrzeugtyp exists if provided
    if vehicle_data.fahrzeugtyp_id:
        fahrzeugtyp = db.get(FahrzeugTyp, vehicle_data.fahrzeugtyp_id)
        if not fahrzeugtyp:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeugtyp nicht gefunden"
            )
    
    # Check if fahrzeuggruppe exists if provided
    if vehicle_data.fahrzeuggruppe_id:
        fahrzeuggruppe = db.get(FahrzeugGruppe, vehicle_data.fahrzeuggruppe_id)
        if not fahrzeuggruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeuggruppe nicht gefunden"
            )
    
    try:
        update_data = vehicle_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(vehicle, field, value)
        
        db.commit()
        db.refresh(vehicle)
        return FahrzeugSchema.model_validate(vehicle)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Kennzeichen bereits vergeben"
        )


@router.delete("/{vehicle_id}")
def delete_vehicle(
    vehicle_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete vehicle"""
    check_write_permission(current_user)
    
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    # Check if vehicle has active checklists or TÜV records
    # TODO: Add these checks when implementing those features
    
    db.delete(vehicle)
    db.commit()
    return {"detail": "Fahrzeug gelöscht"}


# TÜV management for vehicles
@router.get("/{vehicle_id}/tuv")
def get_vehicle_tuv(
    vehicle_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get TÜV information for a specific vehicle"""
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    tuv_termin = db.query(TuvTermin).filter(TuvTermin.fahrzeug_id == vehicle_id).first()
    
    if not tuv_termin:
        return {"vehicle_id": vehicle_id, "tuv_data": None}
    
    from datetime import datetime
    now = datetime.now()
    days_remaining = (tuv_termin.ablauf_datum - now).days if tuv_termin.ablauf_datum is not None else None
    
    tuv_status = "unknown"
    if days_remaining is not None:
        if days_remaining < 0:
            tuv_status = "expired"
        elif days_remaining <= 30:
            tuv_status = "warning"
        else:
            tuv_status = "current"
    
    return {
        "vehicle_id": vehicle_id,
        "tuv_data": {
            "id": tuv_termin.id,
            "ablauf_datum": tuv_termin.ablauf_datum,
            "letzte_pruefung": tuv_termin.letzte_pruefung,
            "status": tuv_status,
            "days_remaining": days_remaining,
            "created_at": tuv_termin.created_at
        }
    }


@router.post("/{vehicle_id}/tuv")
def create_vehicle_tuv(
    vehicle_id: int,
    tuv_data: TuvTerminCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create or update TÜV information for a vehicle"""
    check_write_permission(current_user)
    
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    # Check if TÜV record already exists
    existing_tuv = db.query(TuvTermin).filter(TuvTermin.fahrzeug_id == vehicle_id).first()
    
    if existing_tuv:
        # Update existing record
        for field, value in tuv_data.model_dump(exclude_unset=True).items():
            if field != 'fahrzeug_id':  # Don't update fahrzeug_id
                setattr(existing_tuv, field, value)
        
        db.commit()
        db.refresh(existing_tuv)
        return {"detail": "TÜV-Daten aktualisiert", "tuv_id": existing_tuv.id}
    else:
        # Create new record
        tuv_data_dict = tuv_data.model_dump()
        tuv_data_dict['fahrzeug_id'] = vehicle_id  # Ensure correct vehicle ID
        
        db_tuv = TuvTermin(**tuv_data_dict)
        db.add(db_tuv)
        db.commit()
        db.refresh(db_tuv)
        return {"detail": "TÜV-Daten erstellt", "tuv_id": db_tuv.id}


@router.put("/{vehicle_id}/tuv")
def update_vehicle_tuv(
    vehicle_id: int,
    tuv_data: TuvTerminUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update TÜV information for a vehicle"""
    check_write_permission(current_user)
    
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    tuv_termin = db.query(TuvTermin).filter(TuvTermin.fahrzeug_id == vehicle_id).first()
    if not tuv_termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="TÜV-Daten für dieses Fahrzeug nicht gefunden"
        )
    
    update_data = tuv_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        if field != 'fahrzeug_id':  # Don't update fahrzeug_id
            setattr(tuv_termin, field, value)
    
    db.commit()
    db.refresh(tuv_termin)
    return {"detail": "TÜV-Daten aktualisiert", "tuv_id": tuv_termin.id}


@router.delete("/{vehicle_id}/tuv")
def delete_vehicle_tuv(
    vehicle_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete TÜV information for a vehicle"""
    check_write_permission(current_user)
    
    vehicle = db.get(Fahrzeug, vehicle_id)
    if not vehicle:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeug nicht gefunden"
        )
    
    tuv_termin = db.query(TuvTermin).filter(TuvTermin.fahrzeug_id == vehicle_id).first()
    if not tuv_termin:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="TÜV-Daten für dieses Fahrzeug nicht gefunden"
        )
    
    db.delete(tuv_termin)
    db.commit()
    return {"detail": "TÜV-Daten gelöscht"}


@router.get("/types/available")
def get_vehicle_types(current_user: Benutzer = Depends(get_current_user)):
    """Get available vehicle types"""
    return {
        "types": [
            {"code": "MTF", "name": "Mannschaftstransportfahrzeug"},
            {"code": "RTB", "name": "Rettungsboot"},
            {"code": "FR", "name": "First-Responder"},
            {"code": "TLF", "name": "Tanklöschfahrzeug"},
            {"code": "LHF", "name": "Lösch- und Hilfeleistungsfahrzeug"},
            {"code": "RTW", "name": "Rettungstransportwagen"}
        ]
    }
