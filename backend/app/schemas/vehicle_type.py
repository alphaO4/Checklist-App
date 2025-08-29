from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class FahrzeugTypBase(BaseModel):
    name: str
    beschreibung: Optional[str] = None
    aktiv: bool = True


class FahrzeugTypCreate(FahrzeugTypBase):
    pass


class FahrzeugTypUpdate(BaseModel):
    name: Optional[str] = None
    beschreibung: Optional[str] = None
    aktiv: Optional[bool] = None


class FahrzeugTyp(FahrzeugTypBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True
