from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from .vehicle_type import FahrzeugTyp


class FahrzeugGruppeBase(BaseModel):
    name: str


class FahrzeugGruppeCreate(FahrzeugGruppeBase):
    pass


class FahrzeugGruppeUpdate(BaseModel):
    name: Optional[str] = None


class FahrzeugGruppe(FahrzeugGruppeBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True


class FahrzeugBase(BaseModel):
    kennzeichen: str
    fahrzeugtyp_id: int
    fahrzeuggruppe_id: int


class FahrzeugCreate(FahrzeugBase):
    pass


class FahrzeugUpdate(BaseModel):
    kennzeichen: Optional[str] = None
    fahrzeugtyp_id: Optional[int] = None
    fahrzeuggruppe_id: Optional[int] = None


class Fahrzeug(FahrzeugBase):
    id: int
    created_at: datetime
    fahrzeugtyp: Optional[FahrzeugTyp] = None

    class Config:
        from_attributes = True


class FahrzeugWithGroup(Fahrzeug):
    fahrzeuggruppe: FahrzeugGruppe


# List response with pagination
class FahrzeugList(BaseModel):
    items: list[Fahrzeug]
    total: int
    page: int
    per_page: int
    total_pages: int
