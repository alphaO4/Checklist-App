from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional

from ...db.session import get_db
from ...models.vehicle import Fahrzeug, FahrzeugGruppe
from ...models.vehicle_type import FahrzeugTyp
from ...models.user import Benutzer
from ...schemas.vehicle import (
    Fahrzeug as FahrzeugSchema, FahrzeugCreate, FahrzeugUpdate, FahrzeugList, FahrzeugWithGroup
)
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
    """Get vehicle by ID with group information"""
    vehicle = db.query(Fahrzeug).options(
        joinedload(Fahrzeug.fahrzeuggruppe),
        joinedload(Fahrzeug.fahrzeugtyp)
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
