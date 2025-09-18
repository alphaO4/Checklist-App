from pydantic import BaseModel, Field, validator
from typing import Optional, List, Any, Dict, Union
from datetime import datetime
from enum import Enum


class ChecklistItemTypeEnum(str, Enum):
    """Enum for checklist item types"""
    VEHICLE_INFO = "vehicle_info"
    RATING_1_6 = "rating_1_6"
    PERCENTAGE = "percentage"
    ATEMSCHUTZ = "atemschutz"
    STANDARD = "standard"
    QUANTITY = "quantity"
    DATE_CHECK = "date_check"
    STATUS_CHECK = "status_check"


class ChecklistItemBase(BaseModel):
    beschreibung: str
    item_type: ChecklistItemTypeEnum = ChecklistItemTypeEnum.STANDARD
    validation_config: Optional[Dict[str, Any]] = None
    editable_roles: Optional[List[str]] = Field(default_factory=lambda: ["organisator", "admin"])
    requires_tuv: bool = False
    subcategories: Optional[Dict[str, Any]] = None
    pflicht: bool = True
    reihenfolge: int = 0


class ChecklistItemCreateForTemplate(ChecklistItemBase):
    """Schema for creating items as part of a checklist/template (no checkliste_id needed)"""
    pass


class ChecklistItemCreate(ChecklistItemBase):
    checkliste_id: int
    
    @validator('validation_config')
    def validate_config(cls, v, values):
        if values.get('item_type') == ChecklistItemTypeEnum.RATING_1_6:
            if not v or 'min_value' not in v or 'max_value' not in v:
                return {'min_value': 1, 'max_value': 6}
        elif values.get('item_type') == ChecklistItemTypeEnum.PERCENTAGE:
            if not v or 'min_value' not in v or 'max_value' not in v:
                return {'min_value': 0, 'max_value': 100}
        return v


class ChecklistItemUpdate(BaseModel):
    beschreibung: Optional[str] = None
    item_type: Optional[ChecklistItemTypeEnum] = None
    validation_config: Optional[Dict[str, Any]] = None
    editable_roles: Optional[List[str]] = None
    requires_tuv: Optional[bool] = None
    subcategories: Optional[Dict[str, Any]] = None
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
    items: Optional[List[ChecklistItemCreateForTemplate]] = []


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


class ChecklisteWithVehicles(Checkliste):
    """Checklist with vehicles that can use it (through fahrzeuggruppe)"""
    available_vehicles: Optional[List[Dict[str, Any]]] = []


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
    status: str = "ok"  # ok, fehler, nicht_pruefbar
    wert: Optional[Union[int, float, str, Dict[str, Any]]] = None  # Item value (rating, percentage, etc.)
    vorhanden: Optional[bool] = None      # For standard items - is item present?
    tuv_datum: Optional[datetime] = None  # TÜV expiration date
    tuv_status: Optional[str] = None      # current, warning, expired
    menge: Optional[int] = None           # For quantity items
    kommentar: Optional[str] = None


class ItemErgebnisCreate(ItemErgebnisBase):
    item_id: int
    
    @validator('wert')
    def validate_wert(cls, v, values):
        # Add validation based on item type if needed
        return v


class ItemErgebnisUpdate(ItemErgebnisBase):
    pass


class ItemErgebnis(ItemErgebnisBase):
    id: int
    ausfuehrung_id: int
    item_id: int
    created_at: datetime

    class Config:
        from_attributes = True


# Enhanced schemas for specific item types
class AtemschutzErgebnis(BaseModel):
    """Specific schema for Atemschutz results"""
    tuv_platte: Optional[datetime] = None
    tuv_respihood: Optional[datetime] = None
    pa_geraete: List[Dict[str, Any]] = Field(default_factory=list)
    
    
class RatingErgebnis(BaseModel):
    """Schema for rating items (1-6)"""
    bewertung: int = Field(..., ge=1, le=6)
    
    
class PercentageErgebnis(BaseModel):
    """Schema for percentage items (0-100%)"""
    prozent: int = Field(..., ge=0, le=100)


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
