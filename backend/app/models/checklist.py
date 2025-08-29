from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, func, Boolean, Text
from sqlalchemy.orm import relationship
from ..db.session import Base


class TuvTermin(Base):
    __tablename__ = "tuv_termine"

    id = Column(Integer, primary_key=True, index=True)
    fahrzeug_id = Column(Integer, ForeignKey("fahrzeuge.id"), nullable=False)
    ablauf_datum = Column(DateTime, nullable=False)
    status = Column(String(50), default="reminder")
    letzte_pruefung = Column(DateTime, nullable=True)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    fahrzeug = relationship("Fahrzeug", back_populates="tuv_termine")


class Checkliste(Base):
    __tablename__ = "checklisten"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), nullable=False)
    fahrzeuggruppe_id = Column(Integer, ForeignKey("fahrzeuggruppen.id"), nullable=False)
    ersteller_id = Column(Integer, ForeignKey("benutzer.id"), nullable=True)
    template = Column(Boolean, default=False)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    fahrzeuggruppe = relationship("FahrzeugGruppe", back_populates="checklisten")
    ersteller = relationship("Benutzer", back_populates="erstellte_checklisten")
    items = relationship("ChecklistItem", back_populates="checkliste", cascade="all, delete-orphan")
    ausfuehrungen = relationship("ChecklistAusfuehrung", back_populates="checkliste", cascade="all, delete-orphan")


class ChecklistItem(Base):
    __tablename__ = "checklist_items"

    id = Column(Integer, primary_key=True, index=True)
    checkliste_id = Column(Integer, ForeignKey("checklisten.id"), nullable=False)
    beschreibung = Column(String(500), nullable=False)
    pflicht = Column(Boolean, default=True)
    reihenfolge = Column(Integer, default=0)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    checkliste = relationship("Checkliste", back_populates="items")
    ergebnisse = relationship("ItemErgebnis", back_populates="item", cascade="all, delete-orphan")


class ChecklistAusfuehrung(Base):
    __tablename__ = "checklist_ausfuehrungen"

    id = Column(Integer, primary_key=True, index=True)
    checkliste_id = Column(Integer, ForeignKey("checklisten.id"), nullable=False)
    fahrzeug_id = Column(Integer, ForeignKey("fahrzeuge.id"), nullable=False)
    benutzer_id = Column(Integer, ForeignKey("benutzer.id"), nullable=False)
    status = Column(String(50), default="started")
    started_at = Column(DateTime, server_default=func.now(), nullable=False)
    completed_at = Column(DateTime, nullable=True)

    # Relationships
    checkliste = relationship("Checkliste", back_populates="ausfuehrungen")
    fahrzeug = relationship("Fahrzeug", back_populates="ausfuehrungen")
    benutzer = relationship("Benutzer", back_populates="ausfuehrungen")
    ergebnisse = relationship("ItemErgebnis", back_populates="ausfuehrung", cascade="all, delete-orphan")


class ItemErgebnis(Base):
    __tablename__ = "item_ergebnisse"

    id = Column(Integer, primary_key=True, index=True)
    ausfuehrung_id = Column(Integer, ForeignKey("checklist_ausfuehrungen.id"), nullable=False)
    item_id = Column(Integer, ForeignKey("checklist_items.id"), nullable=False)
    status = Column(String(50), default="ok")
    kommentar = Column(Text, nullable=True)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    ausfuehrung = relationship("ChecklistAusfuehrung", back_populates="ergebnisse")
    item = relationship("ChecklistItem", back_populates="ergebnisse")


class AuditLog(Base):
    __tablename__ = "audit_log"

    id = Column(Integer, primary_key=True, index=True)
    benutzer_id = Column(Integer, ForeignKey("benutzer.id"), nullable=True)
    aktion = Column(String(200), nullable=False)
    ressource_typ = Column(String(100), nullable=False)
    ressource_id = Column(Integer, nullable=True)
    alte_werte = Column(Text, nullable=True)
    neue_werte = Column(Text, nullable=True)
    timestamp = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    benutzer = relationship("Benutzer", back_populates="audit_logs")
