from sqlalchemy import Column, Integer, String, DateTime, func
from sqlalchemy.orm import relationship
from ..db.session import Base


class Benutzer(Base):
    __tablename__ = "benutzer"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(100), unique=True, index=True, nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    rolle = Column(String(50), default="benutzer")
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    geleitete_gruppen = relationship("Gruppe", back_populates="gruppenleiter", foreign_keys="Gruppe.gruppenleiter_id")
    erstellte_checklisten = relationship("Checkliste", back_populates="ersteller")
    ausfuehrungen = relationship("ChecklistAusfuehrung", back_populates="benutzer")
    audit_logs = relationship("AuditLog", back_populates="benutzer")
