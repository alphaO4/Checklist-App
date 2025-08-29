from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from .auth import User
from .vehicle import FahrzeugGruppe


class GruppeBase(BaseModel):
    name: str
    gruppenleiter_id: Optional[int] = None
    fahrzeuggruppe_id: Optional[int] = None


class GruppeCreate(GruppeBase):
    pass


class GruppeUpdate(BaseModel):
    name: Optional[str] = None
    gruppenleiter_id: Optional[int] = None
    fahrzeuggruppe_id: Optional[int] = None


class Gruppe(GruppeBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True


class GruppeWithRelations(Gruppe):
    benutzer: Optional[List[User]] = None
    gruppenleiter: Optional[User] = None
    fahrzeuggruppe: Optional[FahrzeugGruppe] = None


# List response with pagination
class GruppeList(BaseModel):
    items: List[Gruppe]
    total: int
    page: int
    per_page: int
    total_pages: int
