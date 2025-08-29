from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import datetime


class ChecklistItemBase(BaseModel):
    beschreibung: str
    pflicht: bool = True
    reihenfolge: int = 0


class ChecklistItemCreate(ChecklistItemBase):
    pass


class ChecklistItemUpdate(BaseModel):
    beschreibung: Optional[str] = None
    pflicht: Optional[bool] = None
    reihenfolge: Optional[int] = None


class ChecklistItem(ChecklistItemBase):
    id: int
    checkliste_id: int
    created_at: datetime

    class Config:
        from_attributes = True


class ChecklisteBase(BaseModel):
    name: str
    fahrzeuggruppe_id: int
    template: bool = False


class ChecklisteCreate(ChecklisteBase):
    items: Optional[List[ChecklistItemCreate]] = []


class ChecklisteUpdate(BaseModel):
    name: Optional[str] = None
    fahrzeuggruppe_id: Optional[int] = None
    template: Optional[bool] = None


class Checkliste(ChecklisteBase):
    id: int
    ersteller_id: Optional[int] = None
    created_at: datetime

    class Config:
        from_attributes = True


class ChecklisteWithItems(Checkliste):
    items: List[ChecklistItem] = []


class ChecklistAusfuehrungBase(BaseModel):
    checkliste_id: int
    fahrzeug_id: int


class ChecklistAusfuehrungCreate(ChecklistAusfuehrungBase):
    pass


class ChecklistAusfuehrung(ChecklistAusfuehrungBase):
    id: int
    benutzer_id: int
    status: str  # started, completed, cancelled
    started_at: datetime
    completed_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class ItemErgebnisBase(BaseModel):
    status: str  # ok, fehler, nicht_pruefbar
    kommentar: Optional[str] = None


class ItemErgebnisCreate(ItemErgebnisBase):
    item_id: int


class ItemErgebnisUpdate(ItemErgebnisBase):
    pass


class ItemErgebnis(ItemErgebnisBase):
    id: int
    ausfuehrung_id: int
    item_id: int
    created_at: datetime

    class Config:
        from_attributes = True


class ChecklisteList(BaseModel):
    items: List[Checkliste]
    total: int
    page: int
    per_page: int
    total_pages: int


# For sync operations
class OfflineAction(BaseModel):
    action_type: str  # create_checklist, update_item, etc.
    resource_type: str  # checklist, item_result, etc.
    resource_id: Optional[str] = None
    data: Any
    timestamp: datetime
    client_id: str
