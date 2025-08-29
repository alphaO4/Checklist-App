from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from .core.settings import settings
from .api.routes import health, auth, ws, groups, fahrzeuggruppen
from .api.routes import vehicles, tuv, checklists, sync, vehicle_types
from .db.session import Base, engine
from sqlalchemy.orm import Session
from .models.user import Benutzer
from .models.group import Gruppe
from .models.vehicle import Fahrzeug, FahrzeugGruppe
from .models.vehicle_type import FahrzeugTyp
from .models.checklist import (
    TuvTermin, Checkliste, ChecklistItem, 
    ChecklistAusfuehrung, ItemErgebnis
)
from .core.security import hash_password
from .services.seed_data import create_sample_data

app = FastAPI(
    title="Checklist App Backend", 
    version="0.1.0",
    description="Backend für Feuerwehr Fahrzeugprüfung und TÜV-Verwaltung"
)

# CORS for Electron dev and potential web clients
origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins or ["http://localhost", "http://127.0.0.1"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(health.router)
app.include_router(auth.router, prefix="/auth", tags=["auth"])
app.include_router(ws.router)
app.include_router(groups.router, prefix="/groups", tags=["groups"])
app.include_router(fahrzeuggruppen.router, prefix="/fahrzeuggruppen", tags=["fahrzeuggruppen"])
app.include_router(vehicles.router, prefix="/vehicles", tags=["vehicles"])
app.include_router(vehicle_types.router, prefix="/vehicle-types", tags=["vehicle-types"])
app.include_router(tuv.router, prefix="/tuv", tags=["tuv"])
app.include_router(checklists.router, prefix="/checklists", tags=["checklists"])
app.include_router(sync.router, prefix="/sync", tags=["sync"])

@app.get("/")
def root():
    return {
        "status": "ok", 
        "service": "checklist-backend",
        "version": "0.1.0",
        "description": "Feuerwehr Fahrzeugprüfung Backend"
    }


@app.get("/seed-data")
def seed_sample_data(
    force: bool = Query(False, description="Force recreate sample data")
):
    """Create sample data for development (admin only in production)"""
    try:
        with Session(bind=engine) as db:
            # Check if data already exists
            existing_users = db.query(Benutzer).count()
            
            if existing_users > 1 and not force:
                return {
                    "message": "Sample data already exists",
                    "existing_users": existing_users,
                    "hint": "Use ?force=true to recreate"
                }
            
            # Clear existing data if forcing
            if force and existing_users > 1:
                # Note: In production, this should be more careful about data deletion
                db.query(ItemErgebnis).delete()
                db.query(ChecklistAusfuehrung).delete()
                db.query(ChecklistItem).delete()
                db.query(Checkliste).delete()
                db.query(TuvTermin).delete()
                db.query(Fahrzeug).delete()
                db.query(FahrzeugGruppe).delete()
                db.query(Gruppe).delete()
                # Keep admin user, delete others
                db.query(Benutzer).filter(Benutzer.username != "admin").delete()
                db.commit()
            
            result = create_sample_data(db)
            return {
                "message": "Sample data created successfully",
                **result
            }
    except Exception as e:
        return {
            "error": "Failed to create sample data",
            "details": str(e),
            "hint": "Try using ?force=true to recreate existing data"
        }


@app.on_event("startup")
def on_startup():
    # Create tables
    Base.metadata.create_all(bind=engine)
    
    # Seed a default admin if none exists (dev convenience)
    with Session(bind=engine) as db:
        if db.query(Benutzer).count() == 0:
            admin = Benutzer(
                username="admin",
                email="admin@example.com",
                password_hash=hash_password("admin"),
                rolle="admin",
            )
            db.add(admin)
            db.commit()
            print("✅ Default admin user created (username: admin, password: admin)")
        else:
            print("✅ Database initialized, users exist")
