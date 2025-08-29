from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import IntegrityError
from typing import Optional

from ...db.session import get_db
from ...models.group import Gruppe
from ...models.vehicle import FahrzeugGruppe
from ...models.user import Benutzer
from ...schemas.group import (
    Gruppe as GruppeSchema, GruppeCreate, GruppeUpdate, GruppeList, GruppeWithRelations
)
from ...schemas.vehicle import FahrzeugGruppe as FahrzeugGruppeSchema, FahrzeugGruppeCreate, FahrzeugGruppeUpdate
from ...core.deps import get_current_user

router = APIRouter()


def check_admin_permission(current_user: Benutzer):
    """Check if user is admin"""
    if current_user.rolle != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin Berechtigung erforderlich"
        )


def check_write_permission(current_user: Benutzer):
    """Check if user can create/modify groups"""
    if current_user.rolle not in ["organisator", "admin"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Organisator oder Admin Berechtigung erforderlich"
        )


# Group management routes
@router.get("", response_model=GruppeList)
def list_groups(
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    name: Optional[str] = Query(None, description="Filter by group name"),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List all groups with optional filtering and pagination"""
    offset = (page - 1) * per_page
    
    query = db.query(Gruppe).options(
        joinedload(Gruppe.gruppenleiter),
        joinedload(Gruppe.fahrzeuggruppe)
    )
    
    # Apply filters
    if name:
        query = query.filter(Gruppe.name.ilike(f"%{name}%"))
    
    total = query.count()
    groups = query.offset(offset).limit(per_page).all()
    
    return GruppeList(
        items=[GruppeSchema.model_validate(group) for group in groups],
        total=total,
        page=page,
        per_page=per_page,
        total_pages=(total + per_page - 1) // per_page
    )


@router.post("", response_model=GruppeSchema, status_code=status.HTTP_201_CREATED)
def create_group(
    group_data: GruppeCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new group"""
    check_write_permission(current_user)
    
    # Check if gruppenleiter exists if provided
    if group_data.gruppenleiter_id:
        gruppenleiter = db.get(Benutzer, group_data.gruppenleiter_id)
        if not gruppenleiter:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Gruppenleiter nicht gefunden"
            )
    
    # Check if fahrzeuggruppe exists if provided
    if group_data.fahrzeuggruppe_id:
        fahrzeuggruppe = db.get(FahrzeugGruppe, group_data.fahrzeuggruppe_id)
        if not fahrzeuggruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeuggruppe nicht gefunden"
            )
        
        # Check if fahrzeuggruppe is already assigned to another group
        existing_group = db.query(Gruppe).filter(
            Gruppe.fahrzeuggruppe_id == group_data.fahrzeuggruppe_id
        ).first()
        if existing_group:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Fahrzeuggruppe bereits der Gruppe '{existing_group.name}' zugeordnet"
            )
    
    try:
        db_group = Gruppe(**group_data.model_dump())
        db.add(db_group)
        db.commit()
        db.refresh(db_group)
        return GruppeSchema.model_validate(db_group)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Gruppenname bereits vergeben"
        )


@router.get("/{group_id}", response_model=GruppeWithRelations)
def get_group(
    group_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get group by ID with all relations"""
    group = db.query(Gruppe).options(
        joinedload(Gruppe.benutzer),
        joinedload(Gruppe.gruppenleiter),
        joinedload(Gruppe.fahrzeuggruppe)
    ).filter(
        Gruppe.id == group_id
    ).first()
    
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Gruppe nicht gefunden"
        )
    
    return GruppeWithRelations.model_validate(group)


@router.put("/{group_id}", response_model=GruppeSchema)
def update_group(
    group_id: int,
    group_data: GruppeUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update group"""
    check_write_permission(current_user)
    
    group = db.get(Gruppe, group_id)
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Gruppe nicht gefunden"
        )
    
    # Check if gruppenleiter exists if provided
    if group_data.gruppenleiter_id:
        gruppenleiter = db.get(Benutzer, group_data.gruppenleiter_id)
        if not gruppenleiter:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Gruppenleiter nicht gefunden"
            )
    
    # Check if fahrzeuggruppe exists if provided
    if group_data.fahrzeuggruppe_id:
        fahrzeuggruppe = db.get(FahrzeugGruppe, group_data.fahrzeuggruppe_id)
        if not fahrzeuggruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Fahrzeuggruppe nicht gefunden"
            )
        
        # Check if fahrzeuggruppe is already assigned to another group
        existing_group = db.query(Gruppe).filter(
            Gruppe.fahrzeuggruppe_id == group_data.fahrzeuggruppe_id,
            Gruppe.id != group_id
        ).first()
        if existing_group:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Fahrzeuggruppe bereits der Gruppe '{existing_group.name}' zugeordnet"
            )
    
    try:
        update_data = group_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(group, field, value)
        
        db.commit()
        db.refresh(group)
        return GruppeSchema.model_validate(group)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Gruppenname bereits vergeben"
        )


@router.delete("/{group_id}")
def delete_group(
    group_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete group"""
    check_admin_permission(current_user)
    
    group = db.get(Gruppe, group_id)
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Gruppe nicht gefunden"
        )
    
    # Check if group has users assigned
    if group.benutzer:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Gruppe kann nicht gelöscht werden - Benutzer sind noch zugeordnet"
        )
    
    db.delete(group)
    db.commit()
    return {"detail": "Gruppe gelöscht"}
