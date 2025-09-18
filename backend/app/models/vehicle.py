from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from ..db.session import Base


class FahrzeugGruppe(Base):
    __tablename__ = "fahrzeuggruppen"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), nullable=False)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    gruppe = relationship("Gruppe", primaryjoin="FahrzeugGruppe.id == Gruppe.fahrzeuggruppe_id", back_populates="fahrzeuggruppe")
    fahrzeuge = relationship("Fahrzeug", back_populates="fahrzeuggruppe", cascade="all, delete-orphan")
    checklisten = relationship("Checkliste", back_populates="fahrzeuggruppe", cascade="all, delete-orphan")
    
    @property
    def active_checklists(self):
        """Get all active (non-template) checklists for this vehicle group"""
        return [c for c in self.checklisten if not c.template]
    
    @property
    def template_checklists(self):
        """Get all template checklists for this vehicle group"""
        return [c for c in self.checklisten if c.template]


class Fahrzeug(Base):
    __tablename__ = "fahrzeuge"

    id = Column(Integer, primary_key=True, index=True)
    kennzeichen = Column(String(50), unique=True, nullable=False)
    fahrzeugtyp_id = Column(Integer, ForeignKey("fahrzeugtypen.id"), nullable=False)
    fahrzeuggruppe_id = Column(Integer, ForeignKey("fahrzeuggruppen.id"), nullable=False)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    fahrzeugtyp = relationship("FahrzeugTyp", back_populates="fahrzeuge")
    fahrzeuggruppe = relationship("FahrzeugGruppe", back_populates="fahrzeuge")
    tuv_termine = relationship("TuvTermin", back_populates="fahrzeug", cascade="all, delete-orphan")
    ausfuehrungen = relationship("ChecklistAusfuehrung", back_populates="fahrzeug", cascade="all, delete-orphan")
    
    @property
    def available_checklists(self):
        """Get all checklists available for this vehicle through its fahrzeuggruppe"""
        if self.fahrzeuggruppe:
            return self.fahrzeuggruppe.checklisten
        return []
