from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from ..models.user import Benutzer
from ..models.group import Gruppe
from ..models.vehicle import FahrzeugGruppe, Fahrzeug
from ..models.vehicle_type import FahrzeugTyp
from ..models.checklist import Checkliste, ChecklistItem, TuvTermin
from ..core.security import hash_password


def create_sample_data(db: Session):
    """Create sample data for development and testing"""
    
    # Create sample groups
    sample_gruppe = Gruppe(
        name="Feuerwehr Muster",
        gruppenleiter_id=None  # Will be set after creating gruppenleiter
    )
    db.add(sample_gruppe)
    db.flush()
    
    # Create sample users (check if they exist first)
    users_to_create = []
    
    # Check if admin already exists, if not create one with different email
    existing_admin = db.query(Benutzer).filter(Benutzer.username == "admin").first()
    if not existing_admin:
        admin = Benutzer(
            username="admin",
            email="admin@feuerwehr-muster.de",
            password_hash=hash_password("admin123"),
            rolle="admin"
        )
        users_to_create.append(admin)
    else:
        admin = existing_admin
    
    # Check other users
    existing_organisator = db.query(Benutzer).filter(Benutzer.username == "organisator").first()
    if not existing_organisator:
        organisator = Benutzer(
            username="organisator",
            email="organisator@feuerwehr-muster.de", 
            password_hash=hash_password("org123"),
            rolle="organisator"
        )
        users_to_create.append(organisator)
    else:
        organisator = existing_organisator
    
    existing_gruppenleiter = db.query(Benutzer).filter(Benutzer.username == "gruppenleiter").first()
    if not existing_gruppenleiter:
        gruppenleiter = Benutzer(
            username="gruppenleiter",
            email="leiter@feuerwehr-muster.de",
            password_hash=hash_password("leiter123"),
            rolle="gruppenleiter"
        )
        users_to_create.append(gruppenleiter)
    else:
        gruppenleiter = existing_gruppenleiter
    
    existing_benutzer = db.query(Benutzer).filter(Benutzer.username == "benutzer").first()
    if not existing_benutzer:
        benutzer = Benutzer(
            username="benutzer",
            email="benutzer@feuerwehr-muster.de",
            password_hash=hash_password("user123"),
            rolle="benutzer"
        )
        users_to_create.append(benutzer)
    else:
        benutzer = existing_benutzer
    
    # Add only new users
    if users_to_create:
        db.add_all(users_to_create)
        db.flush()
    
    # Update gruppe with gruppenleiter
    sample_gruppe.gruppenleiter_id = gruppenleiter.id
    
    # Create vehicle types first
    vehicle_types_data = [
        {"name": "MTF", "beschreibung": "Mannschaftstransportfahrzeug"},
        {"name": "TLF", "beschreibung": "Tanklöschfahrzeug"},
        {"name": "DLK", "beschreibung": "Drehleiterfahrzeug"},
        {"name": "LF", "beschreibung": "Löschfahrzeug"},
        {"name": "RW", "beschreibung": "Rüstwagen"},
        {"name": "GW", "beschreibung": "Gerätewagen"},
        {"name": "KdoW", "beschreibung": "Kommandowagen"}
    ]
    
    vehicle_types = []
    for vt_data in vehicle_types_data:
        existing_type = db.query(FahrzeugTyp).filter(FahrzeugTyp.name == vt_data["name"]).first()
        if not existing_type:
            vt = FahrzeugTyp(**vt_data)
            db.add(vt)
            vehicle_types.append(vt)
        else:
            vehicle_types.append(existing_type)
    
    db.flush()  # Ensure IDs are assigned
    
    # Get the types we'll use
    mtf_type = next(vt for vt in vehicle_types if vt.name == "MTF")
    tlf_type = next(vt for vt in vehicle_types if vt.name == "TLF")
    dlk_type = next(vt for vt in vehicle_types if vt.name == "DLK")

    # Create vehicle groups
    fahrzeuggruppe = FahrzeugGruppe(
        name="Löschfahrzeuge",
        gruppe_id=sample_gruppe.id
    )
    db.add(fahrzeuggruppe)
    db.flush()

    # Create sample vehicles
    vehicles = [
        Fahrzeug(
            kennzeichen="FW-MT-01",
            fahrzeugtyp_id=mtf_type.id, 
            fahrzeuggruppe_id=fahrzeuggruppe.id
        ),
        Fahrzeug(
            kennzeichen="FW-LF-20",
            fahrzeugtyp_id=tlf_type.id,
            fahrzeuggruppe_id=fahrzeuggruppe.id
        ),
        Fahrzeug(
            kennzeichen="FW-DL-30",
            fahrzeugtyp_id=dlk_type.id,
            fahrzeuggruppe_id=fahrzeuggruppe.id
        )
    ]
    db.add_all(vehicles)
    db.flush()
    
    # Create TÜV deadlines
    tuv_termine = [
        TuvTermin(
            fahrzeug_id=vehicles[0].id,
            ablauf_datum=datetime.now() + timedelta(days=45),  # 45 days ahead
            letzte_pruefung=datetime.now() - timedelta(days=320),
            status="current"
        ),
        TuvTermin(
            fahrzeug_id=vehicles[1].id,
            ablauf_datum=datetime.now() + timedelta(days=15),  # Warning zone
            letzte_pruefung=datetime.now() - timedelta(days=350),
            status="warning"
        ),
        TuvTermin(
            fahrzeug_id=vehicles[2].id,
            ablauf_datum=datetime.now() - timedelta(days=5),   # Expired
            letzte_pruefung=datetime.now() - timedelta(days=370),
            status="expired"
        )
    ]
    db.add_all(tuv_termine)
    
    # Create sample checklist templates
    checklist_template = Checkliste(
        name="Standard Fahrzeugprüfung",
        fahrzeuggruppe_id=fahrzeuggruppe.id,
        ersteller_id=organisator.id,
        template=True
    )
    db.add(checklist_template)
    db.flush()
    
    # Create checklist items
    checklist_items = [
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Ölstand prüfen",
            pflicht=True,
            reihenfolge=1
        ),
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Reifendruck kontrollieren",
            pflicht=True,
            reihenfolge=2
        ),
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Lichtanlage testen",
            pflicht=True,
            reihenfolge=3
        ),
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Funkgerät prüfen",
            pflicht=True,
            reihenfolge=4
        ),
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Wassertank kontrollieren (falls vorhanden)",
            pflicht=False,
            reihenfolge=5
        ),
        ChecklistItem(
            checkliste_id=checklist_template.id,
            beschreibung="Ausrüstung auf Vollständigkeit prüfen",
            pflicht=True,
            reihenfolge=6
        )
    ]
    db.add_all(checklist_items)
    
    # Create another checklist template
    monthly_checklist = Checkliste(
        name="Monatliche Wartung",
        fahrzeuggruppe_id=fahrzeuggruppe.id,
        ersteller_id=organisator.id,
        template=True
    )
    db.add(monthly_checklist)
    db.flush()
    
    monthly_items = [
        ChecklistItem(
            checkliste_id=monthly_checklist.id,
            beschreibung="Motorhaube öffnen und Motor visuell prüfen",
            pflicht=True,
            reihenfolge=1
        ),
        ChecklistItem(
            checkliste_id=monthly_checklist.id,
            beschreibung="Bremsflüssigkeit kontrollieren",
            pflicht=True,
            reihenfolge=2
        ),
        ChecklistItem(
            checkliste_id=monthly_checklist.id,
            beschreibung="Batteriezustand prüfen",
            pflicht=True,
            reihenfolge=3
        ),
        ChecklistItem(
            checkliste_id=monthly_checklist.id,
            beschreibung="Scheibenwischwasser auffüllen",
            pflicht=False,
            reihenfolge=4
        )
    ]
    db.add_all(monthly_items)
    
    db.commit()
    
    return {
        "users_created": len(users_to_create),
        "users_existing": 4 - len(users_to_create),
        "groups_created": 1,
        "vehicle_types_created": len([vt for vt in vehicle_types if vt in db.new]),
        "vehicle_groups_created": 1,
        "vehicles_created": 3,
        "tuv_deadlines_created": 3,
        "checklist_templates_created": 2,
        "checklist_items_created": 10
    }
