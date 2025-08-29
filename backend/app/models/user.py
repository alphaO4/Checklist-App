from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from ..db.session import Base


class Benutzer(Base):
    __tablename__ = "benutzer"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(100), unique=True, index=True, nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    rolle = Column(String(50), default="benutzer")
    gruppe_id = Column(Integer, ForeignKey("gruppen.id"), nullable=True)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    gruppe = relationship("Gruppe", primaryjoin="Benutzer.gruppe_id == Gruppe.id", back_populates="benutzer")
    geleitete_gruppen = relationship("Gruppe", primaryjoin="Benutzer.id == Gruppe.gruppenleiter_id", back_populates="gruppenleiter")
    erstellte_checklisten = relationship("Checkliste", back_populates="ersteller")
    ausfuehrungen = relationship("ChecklistAusfuehrung", back_populates="benutzer")
    audit_logs = relationship("AuditLog", back_populates="benutzer")
