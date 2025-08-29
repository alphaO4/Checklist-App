from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from fastapi.security import OAuth2PasswordRequestForm
from typing import Optional

from ...db.session import get_db
from ...models.user import Benutzer
from ...models.group import Gruppe
from ...schemas.auth import (
    LoginRequest, Token, LoginResponse, User, UserCreate, UserUpdate, 
    UserChangePassword, UserList, ApiError
)
from ...core.security import verify_password, create_access_token, hash_password
from ...core.deps import get_current_user

router = APIRouter()


def check_admin_role(current_user: Benutzer):
    """Helper to check if user has admin role"""
    if getattr(current_user, "rolle", "") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin privileges required"
        )


@router.post("/token", response_model=Token)
def token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(Benutzer).filter(Benutzer.username == form_data.username).first()
    if not user or not verify_password(form_data.password, user.password_hash):  # type: ignore[arg-type]
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail="Ungültige Anmeldedaten"
        )
    token = create_access_token(subject=str(user.id))
    return Token(access_token=token)


@router.post("/login", response_model=LoginResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(Benutzer).filter(Benutzer.username == data.username).first()
    if not user or not verify_password(data.password, user.password_hash):  # type: ignore[arg-type]
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, 
            detail="Ungültige Anmeldedaten"
        )
    token = create_access_token(subject=str(user.id))
    return LoginResponse(
        access_token=token,
        user=User.model_validate(user)
    )


@router.get("/me", response_model=User)
def me(current_user: Benutzer = Depends(get_current_user)):
    return User.model_validate(current_user)


@router.post("/logout")
def logout(current_user: Benutzer = Depends(get_current_user)):
    """Logout endpoint (token invalidation would be handled client-side)"""
    return {"message": "Erfolgreich abgemeldet"}


@router.post("/users", response_model=User)
def create_user(
    user_data: UserCreate, 
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Create a new user (admin only)"""
    check_admin_role(current_user)
    
    # Validate role
    valid_roles = ["benutzer", "gruppenleiter", "organisator", "admin"]
    if user_data.rolle not in valid_roles:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Ungültige Rolle. Erlaubt: {', '.join(valid_roles)}"
        )
    
    # Check password strength (basic validation)
    if len(user_data.password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Passwort muss mindestens 8 Zeichen lang sein"
        )
    
    # Check if gruppe exists if provided
    if user_data.gruppe_id:
        gruppe = db.get(Gruppe, user_data.gruppe_id)
        if not gruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Gruppe nicht gefunden"
            )
    
    try:
        db_user = Benutzer(
            username=user_data.username,
            email=user_data.email,
            password_hash=hash_password(user_data.password),
            rolle=user_data.rolle,
            gruppe_id=user_data.gruppe_id
        )
        db.add(db_user)
        db.commit()
        db.refresh(db_user)
        return User.model_validate(db_user)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Benutzername oder E-Mail bereits vergeben"
        )


@router.get("/users", response_model=UserList)
def list_users(
    page: int = Query(1, ge=1),
    per_page: int = Query(50, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """List all users with pagination (admin only)"""
    check_admin_role(current_user)
    
    offset = (page - 1) * per_page
    
    total = db.query(Benutzer).count()
    users = db.query(Benutzer).offset(offset).limit(per_page).all()
    
    return UserList(
        items=[User.model_validate(user) for user in users],
        total=total,
        page=page,
        per_page=per_page,
        total_pages=(total + per_page - 1) // per_page
    )


@router.get("/users/{user_id}", response_model=User)
def get_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Get user by ID (admin only)"""
    check_admin_role(current_user)
    
    user = db.get(Benutzer, user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Benutzer nicht gefunden"
        )
    return User.model_validate(user)


@router.put("/users/{user_id}", response_model=User)
def update_user(
    user_id: int,
    user_data: UserUpdate,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Update user (admin only)"""
    check_admin_role(current_user)
    
    user = db.get(Benutzer, user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Benutzer nicht gefunden"
        )
    
    # Validate role if provided
    if user_data.rolle:
        valid_roles = ["benutzer", "gruppenleiter", "organisator", "admin"]
        if user_data.rolle not in valid_roles:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Ungültige Rolle. Erlaubt: {', '.join(valid_roles)}"
            )
    
    # Check if gruppe exists if provided
    if user_data.gruppe_id:
        gruppe = db.get(Gruppe, user_data.gruppe_id)
        if not gruppe:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Gruppe nicht gefunden"
            )
    
    try:
        update_data = user_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(user, field, value)
        
        db.commit()
        db.refresh(user)
        return User.model_validate(user)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Benutzername oder E-Mail bereits vergeben"
        )


@router.delete("/users/{user_id}")
def delete_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Delete user (admin only)"""
    check_admin_role(current_user)
    
    if user_id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Kann sich nicht selbst löschen"
        )
    
    user = db.get(Benutzer, user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Benutzer nicht gefunden"
        )
    
    db.delete(user)
    db.commit()
    return {"detail": "Benutzer gelöscht"}


@router.post("/change-password")
def change_password(
    password_data: UserChangePassword,
    db: Session = Depends(get_db),
    current_user: Benutzer = Depends(get_current_user)
):
    """Change current user's password"""
    if not verify_password(password_data.current_password, current_user.password_hash):  # type: ignore[arg-type]
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Aktuelles Passwort ist falsch"
        )
    
    if len(password_data.new_password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Neues Passwort muss mindestens 8 Zeichen lang sein"
        )
    
    # Assign the hashed password directly for clarity and type safety.
    # If you encounter SQLAlchemy mapping issues, use setattr() instead and document the reason.
    current_user.password_hash = hash_password(password_data.new_password)
    db.commit()
    return {"detail": "Passwort erfolgreich geändert"}
