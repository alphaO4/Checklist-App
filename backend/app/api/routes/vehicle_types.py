from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from typing import Optional, List

from ...db.session import get_db
from ...models.vehicle_type import FahrzeugTyp
from ...models.user import Benutzer
from ...schemas.vehicle_type import (
    FahrzeugTyp as FahrzeugTypSchema, FahrzeugTypCreate, FahrzeugTypUpdate
)
from ...core.deps import get_current_user

router = APIRouter()


def check_admin_permission(current_user: Benutzer):
    """Check if user can manage vehicle types"""
    if current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )


@router.get("", response_model=List[FahrzeugTypSchema])
def list_vehicle_types(
    aktiv: Optional[bool] = Query(True, description="Filter by active status"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List all vehicle types"""
    query = db.query(FahrzeugTyp)
    
    if aktiv is not None:
        query = query.filter(FahrzeugTyp.aktiv == aktiv)
        
    return query.order_by(FahrzeugTyp.name).all()


@router.post("", response_model=FahrzeugTypSchema, status_code=status.HTTP_201_CREATED)
def create_vehicle_type(
    fahrzeugtyp_data: FahrzeugTypCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new vehicle type"""
    check_admin_permission(current_user)
    
    # Check if name already exists
    existing = db.query(FahrzeugTyp).filter(FahrzeugTyp.name == fahrzeugtyp_data.name).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeugtyp mit diesem Namen existiert bereits"
        )
    
    db_fahrzeugtyp = FahrzeugTyp(**fahrzeugtyp_data.model_dump())
    db.add(db_fahrzeugtyp)
    db.commit()
    db.refresh(db_fahrzeugtyp)
    
    return db_fahrzeugtyp


@router.get("/{fahrzeugtyp_id}", response_model=FahrzeugTypSchema)
def get_vehicle_type(
    fahrzeugtyp_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get vehicle type by ID"""
    fahrzeugtyp = db.get(FahrzeugTyp, fahrzeugtyp_id)
    if not fahrzeugtyp:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeugtyp nicht gefunden"
        )
    return fahrzeugtyp


@router.put("/{fahrzeugtyp_id}", response_model=FahrzeugTypSchema)
def update_vehicle_type(
    fahrzeugtyp_id: int,
    fahrzeugtyp_data: FahrzeugTypUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update vehicle type"""
    check_admin_permission(current_user)
    
    fahrzeugtyp = db.get(FahrzeugTyp, fahrzeugtyp_id)
    if not fahrzeugtyp:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeugtyp nicht gefunden"
        )
    
    # Check if new name already exists (if name is being changed)
    if fahrzeugtyp_data.name and fahrzeugtyp_data.name != fahrzeugtyp.name:
        existing = db.query(FahrzeugTyp).filter(FahrzeugTyp.name == fahrzeugtyp_data.name).first()
        if existing:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeugtyp mit diesem Namen existiert bereits"
            )
    
    # Update fields
    update_data = fahrzeugtyp_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(fahrzeugtyp, field, value)
    
    db.commit()
    db.refresh(fahrzeugtyp)
    
    return fahrzeugtyp


@router.delete("/{fahrzeugtyp_id}")
def delete_vehicle_type(
    fahrzeugtyp_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete vehicle type (soft delete by setting aktiv=False)"""
    check_admin_permission(current_user)
    
    fahrzeugtyp = db.get(FahrzeugTyp, fahrzeugtyp_id)
    if not fahrzeugtyp:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeugtyp nicht gefunden"
        )
    
    # Check if any vehicles use this type
    from ...models.vehicle import Fahrzeug
    vehicles_count = db.query(Fahrzeug).filter(Fahrzeug.fahrzeugtyp_id == fahrzeugtyp_id).count()
    if vehicles_count > 0:
        # Soft delete - set as inactive
        setattr(fahrzeugtyp, 'aktiv', False)
        db.commit()
        return {"message": f"Fahrzeugtyp '{fahrzeugtyp.name}' wurde deaktiviert (wird von {vehicles_count} Fahrzeugen verwendet)"}
    else:
        # Hard delete if no vehicles use this type
        db.delete(fahrzeugtyp)
        db.commit()
        return {"message": f"Fahrzeugtyp '{fahrzeugtyp.name}' wurde gelöscht"}
