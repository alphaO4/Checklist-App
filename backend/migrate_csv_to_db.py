#!/usr/bin/env python3
"""
CSV to Database Migration Script

This script converts the existing CSV checklist files into the new enhanced database format
that supports different item types, validation rules, and role-based editing permissions.
"""

import csv
import json
import re
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional

from sqlalchemy.orm import Session
from app.db.session import SessionLocal, engine
from app.models.checklist import (
    Checkliste, 
    ChecklistItem, 
    ChecklistAusfuehrung, 
    ItemErgebnis,
    ChecklistItemTypeEnum
)
from app.models.vehicle import Fahrzeug
from app.models.user import Benutzer


class CSVToDBMigrator:
    """Converts CSV checklist data to enhanced database format"""
    
    def __init__(self):
        self.db: Session = SessionLocal()
        self.item_type_patterns = {
            # Vehicle info (read-only)
            r'^(fahrzeug|kennzeichen)$': ChecklistItemTypeEnum.VEHICLE_INFO,
            
            # Fächer ratings (1-6)
            r'^fächer_g[1-6]$': ChecklistItemTypeEnum.RATING_1_6,
            
            # Kraftstoff percentage
            r'^kraftstoff$': ChecklistItemTypeEnum.PERCENTAGE,
            
            # Atemschutz with subcategories
            r'^atemschutz$': ChecklistItemTypeEnum.ATEMSCHUTZ,
            
            # Quantity items (items with "x" counts)
            r'.*(handlampe|warnlampe|fluchthauben).*': ChecklistItemTypeEnum.QUANTITY,
            
            # Date-only TÜV items
            r'.*(defibrillator|titan|co.warner|strahlenmess).*': ChecklistItemTypeEnum.DATE_CHECK,
            
            # Simple status items
            r'.*(funkgeräte|sonstiges|maschinist|bremsdruck).*': ChecklistItemTypeEnum.STATUS_CHECK,
        }
    
    def determine_item_type(self, column_name: str) -> ChecklistItemTypeEnum:
        """Determine the item type based on column name patterns"""
        column_lower = column_name.lower().strip()
        
        for pattern, item_type in self.item_type_patterns.items():
            if re.match(pattern, column_lower):
                return item_type
        
        # Default to standard type for unknown items
        return ChecklistItemTypeEnum.STANDARD
    
    def create_validation_config(self, item_type: ChecklistItemTypeEnum) -> Dict[str, Any]:
        """Create validation configuration for different item types"""
        configs = {
            ChecklistItemTypeEnum.VEHICLE_INFO: {},
            ChecklistItemTypeEnum.RATING_1_6: {
                'min_value': 1,
                'max_value': 6,
                'required': True
            },
            ChecklistItemTypeEnum.PERCENTAGE: {
                'min_value': 0,
                'max_value': 100,
                'unit': '%'
            },
            ChecklistItemTypeEnum.ATEMSCHUTZ: {
                'required_fields': ['tuv_platte', 'tuv_respihood', 'pa_geraete'],
                'date_format': '%m/%y'
            },
            ChecklistItemTypeEnum.QUANTITY: {
                'min_value': 0,
                'max_value': 999,
                'unit': 'x'
            },
            ChecklistItemTypeEnum.DATE_CHECK: {
                'date_format': '%m/%y',
                'required': True
            },
            ChecklistItemTypeEnum.STATUS_CHECK: {
                'allowed_values': ['ok', 'fehler', 'nicht_pruefbar']
            },
            ChecklistItemTypeEnum.STANDARD: {
                'required_fields': ['vorhanden', 'tuv_status'],
                'allowed_values': ['ok', 'fehler', 'nicht_pruefbar']
            }
        }
        return configs.get(item_type, {})
    
    def create_subcategories_config(self, item_type: ChecklistItemTypeEnum) -> Optional[Dict[str, Any]]:
        """Create subcategories configuration for complex items"""
        if item_type == ChecklistItemTypeEnum.ATEMSCHUTZ:
            return {
                'tuv_platte': {
                    'type': 'date',
                    'label': 'TÜV Platte',
                    'required': True,
                    'format': 'MM/YY'
                },
                'tuv_respihood': {
                    'type': 'date',
                    'label': 'TÜV RespiHood', 
                    'required': True,
                    'format': 'MM/YY'
                },
                'pa_geraete': {
                    'type': 'array',
                    'label': 'PA Geräte',
                    'fields': {
                        'pa_type': {'type': 'string', 'label': 'PA Typ'},
                        'nr_rueckenplatte': {'type': 'string', 'label': 'Nr. Rückenplatte'},
                        'tuv_date': {'type': 'date', 'label': 'TÜV Datum', 'format': 'MM/YY'},
                        'pa_druck': {'type': 'integer', 'label': 'PA Druck', 'unit': 'bar'}
                    }
                }
            }
        return None
    
    def get_editable_roles(self, item_type: ChecklistItemTypeEnum) -> List[str]:
        """Get roles that can edit different item types"""
        if item_type == ChecklistItemTypeEnum.VEHICLE_INFO:
            return ['admin']  # Only admin can edit vehicle info
        else:
            return ['organisator', 'admin']  # Organisator and admin can edit checklist items
    
    def parse_csv_file(self, csv_path: Path) -> Dict[str, Any]:
        """Parse a CSV file and extract checklist structure and data"""
        with open(csv_path, 'r', encoding='utf-8') as file:
            reader = csv.DictReader(file)
            
            # Get the header to understand the checklist structure
            headers = reader.fieldnames
            if not headers:
                raise ValueError(f"No headers found in {csv_path}")
            
            # Read the first data row to understand the format
            data_rows = list(reader)
            
            return {
                'headers': headers,
                'data': data_rows,
                'vehicle_name': self.extract_vehicle_name(csv_path.stem),
                'csv_file': csv_path.name
            }
    
    def extract_vehicle_name(self, filename: str) -> str:
        """Extract vehicle name from filename"""
        # Remove common patterns and return clean vehicle name
        vehicle_name = filename.replace('_', ' ')
        # Remove license plate patterns (B-XXXX)
        vehicle_name = re.sub(r'B-\d{4,5}', '', vehicle_name).strip()
        return vehicle_name or filename
    
    def parse_atemschutz_data(self, atemschutz_str: str) -> Dict[str, Any]:
        """Parse complex Atemschutz data from CSV"""
        try:
            # Try to parse as JSON-like string
            if atemschutz_str.startswith('[') and atemschutz_str.endswith(']'):
                # Remove brackets and parse individual PA entries
                content = atemschutz_str[1:-1]
                if not content.strip():
                    return {'pa_geraete': []}
                
                # This is a simplified parser - in reality you'd need more robust parsing
                pa_geraete = []
                entries = content.split('}, {')
                
                for entry in entries:
                    entry = entry.strip('{}\' ')
                    if entry:
                        pa_data = {}
                        # Parse key-value pairs
                        for pair in entry.split('\', \''):
                            if ':' in pair:
                                key, value = pair.split(':', 1)
                                key = key.strip('\'" ')
                                value = value.strip('\'" ')
                                pa_data[key.lower().replace(' ', '_')] = value
                        
                        if pa_data:
                            pa_geraete.append(pa_data)
                
                return {'pa_geraete': pa_geraete}
            
        except Exception as e:
            print(f"Error parsing Atemschutz data: {e}")
        
        # Return empty structure if parsing fails
        return {'pa_geraete': []}
    
    def convert_csv_value_to_db_format(self, value: str, item_type: ChecklistItemTypeEnum) -> Dict[str, Any]:
        """Convert CSV value to database format based on item type"""
        result = {
            'status': 'ok',
            'wert': None,
            'vorhanden': None,
            'tuv_datum': None,
            'tuv_status': None,
            'menge': None,
            'kommentar': None
        }
        
        if not value or value.strip() == '':
            result['status'] = 'nicht_pruefbar'
            return result
        
        value = value.strip()
        
        try:
            if item_type == ChecklistItemTypeEnum.RATING_1_6:
                # Extract rating number (1-6)
                rating = int(value)
                if 1 <= rating <= 6:
                    result['wert'] = rating
                else:
                    result['status'] = 'fehler'
                    result['kommentar'] = f'Invalid rating: {value}'
                    
            elif item_type == ChecklistItemTypeEnum.PERCENTAGE:
                # Parse percentage or fuel level
                if value.lower() in ['voll', 'full']:
                    result['wert'] = 100
                elif value.lower() in ['leer', 'empty']:
                    result['wert'] = 0
                elif '/' in value:  # e.g., "3/4"
                    parts = value.split('/')
                    if len(parts) == 2:
                        result['wert'] = int((int(parts[0]) / int(parts[1])) * 100)
                else:
                    # Try to extract percentage
                    percent_match = re.search(r'(\d+)%?', value)
                    if percent_match:
                        result['wert'] = int(percent_match.group(1))
                        
            elif item_type == ChecklistItemTypeEnum.ATEMSCHUTZ:
                # Parse complex Atemschutz data
                atemschutz_data = self.parse_atemschutz_data(value)
                result['wert'] = atemschutz_data
                
            elif item_type == ChecklistItemTypeEnum.QUANTITY:
                # Extract quantity (e.g., "7x", "3x")
                qty_match = re.search(r'(\d+)x?', value)
                if qty_match:
                    result['menge'] = int(qty_match.group(1))
                else:
                    result['status'] = 'fehler'
                    result['kommentar'] = f'Could not parse quantity: {value}'
                    
            elif item_type == ChecklistItemTypeEnum.DATE_CHECK:
                # Parse TÜV dates (MM/YY format)
                if re.match(r'\d{2}/\d{2}', value):
                    month, year = value.split('/')
                    # Convert 2-digit year to 4-digit
                    full_year = 2000 + int(year) if int(year) < 50 else 1900 + int(year)
                    result['tuv_datum'] = datetime(full_year, int(month), 1)
                    
                    # Determine TÜV status based on current date
                    now = datetime.now()
                    if result['tuv_datum'] > now:
                        result['tuv_status'] = 'current'
                    elif (result['tuv_datum'].year == now.year and result['tuv_datum'].month >= now.month - 3):
                        result['tuv_status'] = 'warning'
                    else:
                        result['tuv_status'] = 'expired'
                        
            elif item_type == ChecklistItemTypeEnum.STATUS_CHECK:
                # Simple status mapping
                if value.lower() in ['ok', 'gut', 'good']:
                    result['status'] = 'ok'
                elif value.lower() in ['fehler', 'error', 'schlecht', 'bad']:
                    result['status'] = 'fehler'
                else:
                    result['status'] = 'ok'
                    result['kommentar'] = value
                    
            elif item_type == ChecklistItemTypeEnum.STANDARD:
                # Standard items with presence and TÜV
                if value.lower() in ['ok', 'vorhanden', 'ja', 'yes']:
                    result['vorhanden'] = True
                    result['status'] = 'ok'
                elif value.lower() in ['nein', 'no', 'fehlt', 'missing']:
                    result['vorhanden'] = False
                    result['status'] = 'fehler'
                else:
                    result['vorhanden'] = True
                    result['status'] = 'ok'
                    result['kommentar'] = value
                    
            elif item_type == ChecklistItemTypeEnum.VEHICLE_INFO:
                # Store vehicle info as text
                result['wert'] = value
                result['status'] = 'ok'
                
        except Exception as e:
            result['status'] = 'fehler'
            result['kommentar'] = f'Parsing error: {str(e)}'
        
        return result
    
    def create_checklist_from_csv(self, csv_data: Dict[str, Any]) -> Checkliste:
        """Create a checklist template from CSV structure"""
        
        # Create the checklist
        checklist = Checkliste(
            name=f"{csv_data['vehicle_name']} Checklist Template",
            fahrzeuggruppe_id=1,  # Default to first vehicle group, should be configurable
            ersteller_id=None,    # System-generated template
            template=True
        )
        
        self.db.add(checklist)
        self.db.flush()  # Get the ID
        
        # Create checklist items for each column (except metadata columns)
        metadata_columns = {'datum', 'maschinist', 'sonstiges'}
        order = 0
        
        for header in csv_data['headers']:
            if header.lower() not in metadata_columns:
                item_type = self.determine_item_type(header)
                
                item = ChecklistItem(
                    checkliste_id=checklist.id,
                    beschreibung=header.replace('_', ' ').title(),
                    item_type=item_type,
                    validation_config=self.create_validation_config(item_type),
                    editable_roles=self.get_editable_roles(item_type),
                    requires_tuv=(item_type in [
                        ChecklistItemTypeEnum.ATEMSCHUTZ,
                        ChecklistItemTypeEnum.DATE_CHECK,
                        ChecklistItemTypeEnum.STANDARD
                    ]),
                    subcategories=self.create_subcategories_config(item_type),
                    pflicht=True,
                    reihenfolge=order
                )
                
                self.db.add(item)
                order += 10
        
        return checklist
    
    def migrate_csv_files(self, csv_directory: str = "checklists"):
        """Migrate all CSV files in the directory"""
        csv_path = Path(__file__).parent / csv_directory
        
        if not csv_path.exists():
            print(f"CSV directory not found: {csv_path}")
            return
        
        csv_files = list(csv_path.glob("*.csv"))
        
        if not csv_files:
            print(f"No CSV files found in {csv_path}")
            return
        
        print(f"Found {len(csv_files)} CSV files to migrate")
        
        for csv_file in csv_files:
            try:
                print(f"\\nProcessing {csv_file.name}...")
                
                # Parse CSV data
                csv_data = self.parse_csv_file(csv_file)
                
                # Create checklist template
                checklist = self.create_checklist_from_csv(csv_data)
                
                print(f"Created checklist template: {checklist.name}")
                print(f"Items created: {len(checklist.items)}")
                
                # Optionally create sample executions from CSV data
                if csv_data['data']:
                    print(f"Sample data rows available: {len(csv_data['data'])}")
                
            except Exception as e:
                print(f"Error processing {csv_file.name}: {e}")
                self.db.rollback()
                continue
        
        # Commit all changes
        try:
            self.db.commit()
            print("\\nMigration completed successfully!")
        except Exception as e:
            print(f"Error committing changes: {e}")
            self.db.rollback()
    
    def cleanup(self):
        """Close database connection"""
        self.db.close()


def main():
    """Main migration function"""
    print("Starting CSV to Database Migration...")
    
    migrator = CSVToDBMigrator()
    
    try:
        migrator.migrate_csv_files()
    finally:
        migrator.cleanup()


if __name__ == "__main__":
    main()