from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from ..db.session import Base


class Gruppe(Base):
    __tablename__ = "gruppen"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), unique=True, nullable=False)
    gruppenleiter_id = Column(Integer, ForeignKey("benutzer.id"), nullable=True)
    fahrzeuggruppe_id = Column(Integer, ForeignKey("fahrzeuggruppen.id"), nullable=True)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    benutzer = relationship("Benutzer", primaryjoin="Gruppe.id == Benutzer.gruppe_id", back_populates="gruppe")
    gruppenleiter = relationship("Benutzer", primaryjoin="Gruppe.gruppenleiter_id == Benutzer.id", back_populates="geleitete_gruppen")
    fahrzeuggruppe = relationship("FahrzeugGruppe", primaryjoin="Gruppe.fahrzeuggruppe_id == FahrzeugGruppe.id", back_populates="gruppe", uselist=False)
