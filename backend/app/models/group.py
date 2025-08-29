from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from ..db.session import Base


class Gruppe(Base):
    __tablename__ = "gruppen"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), unique=True, nullable=False)
    gruppenleiter_id = Column(Integer, ForeignKey("benutzer.id"), nullable=True)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    gruppenleiter = relationship("Benutzer", back_populates="geleitete_gruppen", foreign_keys=[gruppenleiter_id])
    fahrzeuggruppen = relationship("FahrzeugGruppe", back_populates="gruppe", cascade="all, delete-orphan")
