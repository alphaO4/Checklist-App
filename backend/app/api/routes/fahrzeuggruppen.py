from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from ...db.session import get_db
from ...models.vehicle import FahrzeugGruppe
from ...models.group import Gruppe
from ...models.user import Benutzer
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


@router.get("", response_model=list[FahrzeugGruppeSchema])
def list_fahrzeuggruppen(
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List all fahrzeuggruppen"""
    fahrzeuggruppen = db.query(FahrzeugGruppe).all()
    return [FahrzeugGruppeSchema.model_validate(fg) for fg in fahrzeuggruppen]


@router.post("", response_model=FahrzeugGruppeSchema, status_code=status.HTTP_201_CREATED)
def create_fahrzeuggruppe(
    fahrzeuggruppe_data: FahrzeugGruppeCreate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new fahrzeuggruppe"""
    check_admin_permission(current_user)
    
    try:
        db_fahrzeuggruppe = FahrzeugGruppe(**fahrzeuggruppe_data.model_dump())
        db.add(db_fahrzeuggruppe)
        db.commit()
        db.refresh(db_fahrzeuggruppe)
        return FahrzeugGruppeSchema.model_validate(db_fahrzeuggruppe)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fehler beim Erstellen der Fahrzeuggruppe"
        )


@router.get("/{fahrzeuggruppe_id}", response_model=FahrzeugGruppeSchema)
def get_fahrzeuggruppe(
    fahrzeuggruppe_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get fahrzeuggruppe by ID"""
    fahrzeuggruppe = db.get(FahrzeugGruppe, fahrzeuggruppe_id)
    if not fahrzeuggruppe:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeuggruppe nicht gefunden"
        )
    
    return FahrzeugGruppeSchema.model_validate(fahrzeuggruppe)


@router.put("/{fahrzeuggruppe_id}", response_model=FahrzeugGruppeSchema)
def update_fahrzeuggruppe(
    fahrzeuggruppe_id: int,
    fahrzeuggruppe_data: FahrzeugGruppeUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update fahrzeuggruppe"""
    check_admin_permission(current_user)
    
    fahrzeuggruppe = db.get(FahrzeugGruppe, fahrzeuggruppe_id)
    if not fahrzeuggruppe:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeuggruppe nicht gefunden"
        )
    
    try:
        update_data = fahrzeuggruppe_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(fahrzeuggruppe, field, value)
        
        db.commit()
        db.refresh(fahrzeuggruppe)
        return FahrzeugGruppeSchema.model_validate(fahrzeuggruppe)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fehler beim Aktualisieren der Fahrzeuggruppe"
        )


@router.delete("/{fahrzeuggruppe_id}")
def delete_fahrzeuggruppe(
    fahrzeuggruppe_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete fahrzeuggruppe"""
    check_admin_permission(current_user)
    
    fahrzeuggruppe = db.get(FahrzeugGruppe, fahrzeuggruppe_id)
    if not fahrzeuggruppe:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Fahrzeuggruppe nicht gefunden"
        )
    
    # Check if fahrzeuggruppe is assigned to a group
    assigned_group = db.query(Gruppe).filter(
        Gruppe.fahrzeuggruppe_id == fahrzeuggruppe_id
    ).first()
    if assigned_group:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Fahrzeuggruppe kann nicht gelöscht werden - ist der Gruppe '{assigned_group.name}' zugeordnet"
        )
    
    # Check if fahrzeuggruppe has vehicles
    if fahrzeuggruppe.fahrzeuge:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Fahrzeuggruppe kann nicht gelöscht werden - enthält noch Fahrzeuge"
        )
    
    db.delete(fahrzeuggruppe)
    db.commit()
    return {"detail": "Fahrzeuggruppe gelöscht"}
