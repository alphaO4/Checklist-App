from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class TuvTerminBase(BaseModel):
    fahrzeug_id: int
    ablauf_datum: datetime
    letzte_pruefung: Optional[datetime] = None


class TuvTerminCreate(TuvTerminBase):
    pass


class TuvTerminUpdate(BaseModel):
    fahrzeug_id: Optional[int] = None
    ablauf_datum: Optional[datetime] = None
    letzte_pruefung: Optional[datetime] = None


class TuvTermin(TuvTerminBase):
    id: int
    status: str  # current, warning, expired
    created_at: datetime

    class Config:
        from_attributes = True


class TuvTerminWithFahrzeug(TuvTermin):
    fahrzeug: dict  # Will include kennzeichen, typ


class TuvTerminList(BaseModel):
    items: list[TuvTermin]
    total: int
    page: int
    per_page: int
    total_pages: int
