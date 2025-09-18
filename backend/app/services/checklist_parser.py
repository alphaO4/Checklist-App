import csv
import os
import json
from datetime import datetime
from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from ..models.checklist import Checkliste, ChecklistItem
from ..models.vehicle import Fahrzeug
from ..models.vehicle_type import FahrzeugTyp
from ..db.session import get_db

class ChecklistCSVParser:
    """
    Parser for CSV checklist files from the checklists folder
    Converts CSV data to database entities for fire department vehicle inspections
    """
    
    def __init__(self, checklists_folder: str = "checklists"):
        self.checklists_folder = checklists_folder
        
    def parse_all_checklists(self, db: Session) -> List[Dict[str, Any]]:
        """Parse all CSV files in the checklists folder"""
        parsed_checklists = []
        
        if not os.path.exists(self.checklists_folder):
            print(f"Checklists folder '{self.checklists_folder}' not found")
            return parsed_checklists
            
        for filename in os.listdir(self.checklists_folder):
            if filename.endswith('.csv'):
                try:
                    checklist_data = self.parse_csv_file(filename, db)
                    if checklist_data:
                        parsed_checklists.append(checklist_data)
                except Exception as e:
                    print(f"Error parsing {filename}: {e}")
                    
        return parsed_checklists
    
    def parse_csv_file(self, filename: str, db: Session) -> Optional[Dict[str, Any]]:
        """Parse a single CSV checklist file"""
        filepath = os.path.join(self.checklists_folder, filename)
        
        # Extract vehicle info from filename (e.g., "FirstResponder_B-20218.csv")
        base_name = filename.replace('.csv', '')
        parts = base_name.split('_')
        
        if len(parts) < 2:
            print(f"Invalid filename format: {filename}")
            return None
            
        vehicle_type_name = parts[0]  # "FirstResponder", "LHF1", "TLF"
        kennzeichen = parts[1]       # "B-20218", "B-2183", "B-2226"
        
        with open(filepath, 'r', encoding='utf-8') as file:
            reader = csv.DictReader(file)
            
            # Get column headers for checklist items
            fieldnames = reader.fieldnames
            if not fieldnames:
                return None
                
            # Create checklist template from CSV structure
            checklist_items = self.extract_checklist_items_from_headers(fieldnames)
            
            # Create or get vehicle type
            vehicle_type = self.get_or_create_vehicle_type(db, vehicle_type_name)
            
            # Read the first data row if exists (for sample data)
            sample_data = None
            try:
                sample_data = next(reader)
            except StopIteration:
                pass
            
            return {
                'filename': filename,
                'vehicle_type': vehicle_type_name,
                'kennzeichen': kennzeichen,
                'checklist_items': checklist_items,
                'sample_data': sample_data,
                'vehicle_type_id': vehicle_type.id if vehicle_type else None
            }
    
    def extract_checklist_items_from_headers(self, fieldnames: List[str]) -> List[Dict[str, Any]]:
        """Extract checklist items from CSV headers"""
        # Skip metadata columns
        skip_columns = {
            'Datum', 'Fahrzeug', 'Kennzeichen', 'Maschinist', 
            'Kraftstoff', 'Bremsdruck', 'Sonstiges'
        }
        
        checklist_items = []
        reihenfolge = 1
        
        for field in fieldnames:
            if field in skip_columns:
                continue
                
            # Parse grouped items (e.g., "Kontrolle Einzelgeräte_Handfunkgerät")
            if '_' in field:
                kategorie, item_name = field.split('_', 1)
                beschreibung = f"{kategorie}: {item_name}"
            else:
                kategorie = "Allgemein"
                beschreibung = field
                
            # Determine if item is mandatory based on name patterns
            pflicht = any(keyword in field.lower() for keyword in [
                'defibrillator', 'sauerstoff', 'co warner', 'funkgerät', 
                'atemschutz', 'fluchthaube'
            ])
            
            checklist_items.append({
                'beschreibung': beschreibung,
                'kategorie': kategorie,
                'pflicht': pflicht,
                'reihenfolge': reihenfolge
            })
            
            reihenfolge += 1
            
        return checklist_items
    
    def get_or_create_vehicle_type(self, db: Session, type_name: str) -> Optional[FahrzeugTyp]:
        """Get existing or create new vehicle type"""
        try:
            # Normalize vehicle type names
            type_mapping = {
                'FirstResponder': 'First Responder',
                'LHF1': 'LHF (Lösch- und Hilfeleistungsfahrzeug)',
                'LHF': 'LHF (Lösch- und Hilfeleistungsfahrzeug)',
                'TLF': 'TLF (Tanklöschfahrzeug)',
                'MTF': 'MTF (Mannschaftstransportfahrzeug)',
                'RW': 'RW (Rüstwagen)',
                'DLK': 'DLK (Drehleiter)'
            }
            
            normalized_name = type_mapping.get(type_name, type_name)
            
            # Check if vehicle type exists
            existing_type = db.query(FahrzeugTyp).filter(
                FahrzeugTyp.name == normalized_name
            ).first()
            
            if existing_type:
                return existing_type
                
            # Create new vehicle type
            new_type = FahrzeugTyp(
                name=normalized_name,
                beschreibung=f"Fahrzeugtyp für {type_name} basierend auf CSV-Checkliste"
            )
            
            db.add(new_type)
            db.commit()
            db.refresh(new_type)
            
            return new_type
            
        except Exception as e:
            print(f"Error creating vehicle type {type_name}: {e}")
            db.rollback()
            return None
    
    def create_checklist_templates(self, db: Session) -> List[Checkliste]:
        """Create checklist templates from all CSV files"""
        parsed_data = self.parse_all_checklists(db)
        created_templates = []
        
        for data in parsed_data:
            try:
                # Create checklist template
                checklist = Checkliste(
                    name=f"Fahrzeugkontrolle {data['vehicle_type']}",
                    fahrzeuggruppe_id=None,  # Template - not assigned to specific group
                    ersteller_id=1,  # Default admin user
                    template=True
                )
                
                db.add(checklist)
                db.flush()  # Get ID without committing
                
                # Create checklist items
                for item_data in data['checklist_items']:
                    item = ChecklistItem(
                        checkliste_id=checklist.id,
                        beschreibung=item_data['beschreibung'],
                        pflicht=item_data['pflicht'],
                        reihenfolge=item_data['reihenfolge']
                    )
                    db.add(item)
                
                db.commit()
                created_templates.append(checklist)
                
                print(f"Created template: {checklist.name} with {len(data['checklist_items'])} items")
                
            except Exception as e:
                print(f"Error creating template for {data['vehicle_type']}: {e}")
                db.rollback()
                
        return created_templates
    
    def get_checklist_summary(self) -> Dict[str, Any]:
        """Get summary of all available checklists"""
        parsed_data = self.parse_all_checklists(next(get_db()))
        
        summary = {
            'total_checklists': len(parsed_data),
            'vehicle_types': [],
            'checklists': []
        }
        
        vehicle_types = set()
        
        for data in parsed_data:
            vehicle_types.add(data['vehicle_type'])
            
            summary['checklists'].append({
                'filename': data['filename'],
                'vehicle_type': data['vehicle_type'],
                'kennzeichen': data['kennzeichen'],
                'item_count': len(data['checklist_items']),
                'categories': list(set(item['kategorie'] for item in data['checklist_items']))
            })
        
        summary['vehicle_types'] = list(vehicle_types)
        
        return summary


# Initialize parser instance
checklist_parser = ChecklistCSVParser()