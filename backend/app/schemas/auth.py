from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


class TokenPayload(BaseModel):
    sub: str


class LoginRequest(BaseModel):
    username: str
    password: str


class LoginResponse(Token):
    user: "User"


class User(BaseModel):
    id: int
    username: str
    email: EmailStr
    rolle: str
    created_at: datetime

    class Config:
        from_attributes = True


class UserCreate(BaseModel):
    username: str
    email: EmailStr
    password: str
    rolle: str = "benutzer"


class UserUpdate(BaseModel):
    username: Optional[str] = None
    email: Optional[EmailStr] = None
    rolle: Optional[str] = None


class UserChangePassword(BaseModel):
    current_password: str
    new_password: str


class UserList(BaseModel):
    items: List[User]
    total: int
    page: int
    per_page: int
    total_pages: int


class ApiError(BaseModel):
    detail: str
    error_code: Optional[str] = None
    field_errors: Optional[dict] = None
