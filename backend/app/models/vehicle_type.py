from sqlalchemy import Column, Integer, String, DateTime, func, Boolean
from sqlalchemy.orm import relationship
from ..db.session import Base


class FahrzeugTyp(Base):
    __tablename__ = "fahrzeugtypen"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), unique=True, nullable=False)
    beschreibung = Column(String(200), nullable=True)
    aktiv = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, server_default=func.now(), nullable=False)

    # Relationships
    fahrzeuge = relationship("Fahrzeug", back_populates="fahrzeugtyp")
