"""
Enhanced Checklist Item Types for Database Schema

Based on CSV analysis, we need to support these item types:

1. FAHRZEUG/KENNZEICHEN: Read-only fields (unchangeable)
   - Vehicle identification data

2. FÄCHER_G* (G1-G5): Rating scale 1-6
   - Storage compartment quality ratings

3. KRAFTSTOFF: Percentage 0-100%
   - Fuel level as percentage

4. ATEMSCHUTZ: Complex object with subcategories
   - TÜV Platte: Date validation
   - TÜV RespiHood: Date validation
   - PA (Breathing apparatus) with pressure readings

5. STANDARD ITEMS: Vorhanden (Present) + TÜV status
   - Most equipment items have presence check + TÜV date

6. QUANTITY ITEMS: Number counts (e.g., "7x", "3x")
   - Equipment quantity verification

7. DATE ITEMS: TÜV expiration dates
   - Equipment certification dates

8. STATUS ITEMS: OK/Fehler/Nicht prüfbar
   - Simple status checks
"""

from enum import Enum
from typing import Dict, Any, Optional
from pydantic import BaseModel, Field
from datetime import datetime


class ChecklistItemType(str, Enum):
    """Types of checklist items with different validation rules"""
    VEHICLE_INFO = "vehicle_info"      # Read-only vehicle data
    RATING_1_6 = "rating_1_6"         # Fächer rating 1-6
    PERCENTAGE = "percentage"          # Kraftstoff 0-100%
    ATEMSCHUTZ = "atemschutz"         # Complex breathing apparatus
    STANDARD = "standard"             # Vorhanden + TÜV
    QUANTITY = "quantity"             # Count items (e.g., "7x")
    DATE_CHECK = "date_check"         # TÜV dates only
    STATUS_CHECK = "status_check"     # OK/Fehler/Nicht prüfbar


class ValidationRule(BaseModel):
    """Validation rules for different item types"""
    min_value: Optional[int] = None
    max_value: Optional[int] = None
    required_fields: Optional[list[str]] = None
    allowed_values: Optional[list[str]] = None
    date_format: Optional[str] = None


class ChecklistItemConfig(BaseModel):
    """Configuration for checklist item behavior"""
    item_type: ChecklistItemType
    validation: ValidationRule
    editable_by_roles: list[str] = Field(default=["organisator", "admin"])
    requires_tuv: bool = False
    subcategories: Optional[Dict[str, Any]] = None


# Define configurations for different item categories
ITEM_TYPE_CONFIGS = {
    # Vehicle identification (read-only)
    "fahrzeug": ChecklistItemConfig(
        item_type=ChecklistItemType.VEHICLE_INFO,
        validation=ValidationRule(),
        editable_by_roles=["admin"],  # Only admin can edit vehicle info
    ),
    "kennzeichen": ChecklistItemConfig(
        item_type=ChecklistItemType.VEHICLE_INFO,
        validation=ValidationRule(),
        editable_by_roles=["admin"],
    ),
    
    # Fächer ratings (1-6 scale)
    "fächer_g1": ChecklistItemConfig(
        item_type=ChecklistItemType.RATING_1_6,
        validation=ValidationRule(min_value=1, max_value=6),
    ),
    "fächer_g2": ChecklistItemConfig(
        item_type=ChecklistItemType.RATING_1_6,
        validation=ValidationRule(min_value=1, max_value=6),
    ),
    "fächer_g3": ChecklistItemConfig(
        item_type=ChecklistItemType.RATING_1_6,
        validation=ValidationRule(min_value=1, max_value=6),
    ),
    "fächer_g4": ChecklistItemConfig(
        item_type=ChecklistItemType.RATING_1_6,
        validation=ValidationRule(min_value=1, max_value=6),
    ),
    "fächer_g5": ChecklistItemConfig(
        item_type=ChecklistItemType.RATING_1_6,
        validation=ValidationRule(min_value=1, max_value=6),
    ),
    
    # Kraftstoff percentage
    "kraftstoff": ChecklistItemConfig(
        item_type=ChecklistItemType.PERCENTAGE,
        validation=ValidationRule(min_value=0, max_value=100),
    ),
    
    # Atemschutz with subcategories
    "atemschutz": ChecklistItemConfig(
        item_type=ChecklistItemType.ATEMSCHUTZ,
        validation=ValidationRule(
            required_fields=["tuv_platte", "tuv_respihood", "pa_geraete"]
        ),
        requires_tuv=True,
        subcategories={
            "tuv_platte": {
                "type": "date",
                "label": "TÜV Platte",
                "required": True
            },
            "tuv_respihood": {
                "type": "date", 
                "label": "TÜV RespiHood",
                "required": True
            },
            "pa_geraete": {
                "type": "array",
                "label": "PA Geräte",
                "fields": ["pa_type", "nr_rueckenplatte", "tuv_date", "pa_druck"]
            }
        }
    ),
    
    # Standard items with Vorhanden + TÜV
    "default_standard": ChecklistItemConfig(
        item_type=ChecklistItemType.STANDARD,
        validation=ValidationRule(
            required_fields=["vorhanden", "tuv_status"],
            allowed_values=["ok", "fehler", "nicht_pruefbar"]
        ),
        requires_tuv=True,
    ),
    
    # Quantity items
    "quantity_items": ChecklistItemConfig(
        item_type=ChecklistItemType.QUANTITY,
        validation=ValidationRule(min_value=0, max_value=999),
    ),
    
    # Date-only checks
    "date_only": ChecklistItemConfig(
        item_type=ChecklistItemType.DATE_CHECK,
        validation=ValidationRule(date_format="%m/%y"),
        requires_tuv=True,
    ),
    
    # Simple status checks
    "status_only": ChecklistItemConfig(
        item_type=ChecklistItemType.STATUS_CHECK,
        validation=ValidationRule(
            allowed_values=["ok", "fehler", "nicht_pruefbar"]
        ),
    ),
}


# Map CSV column patterns to item types
CSV_COLUMN_MAPPING = {
    # Vehicle info (read-only)
    r"^(fahrzeug|kennzeichen)$": "fahrzeug",
    
    # Fächer ratings
    r"^fächer_g[1-6]$": "fächer_g1",  # Will be dynamically assigned
    
    # Kraftstoff
    r"^kraftstoff$": "kraftstoff",
    
    # Atemschutz
    r"^atemschutz$": "atemschutz",
    
    # Items that typically have quantities (e.g., "7x", "3x")
    r".*(handlampe|warnlampe|fluchthauben).*": "quantity_items",
    
    # Items with date formats (MM/YY)
    r".*(defibrillator|titan|co.warner|strahlenmess).*": "date_only",
    
    # Everything else defaults to standard Vorhanden + TÜV
    r".*": "default_standard"
}