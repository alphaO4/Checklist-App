from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime


class SyncActionBase(BaseModel):
    action_type: str  # create_checklist, update_item, complete_checklist, etc.
    resource_type: str  # checklist, item_result, ausfuehrung
    resource_id: Optional[str] = None
    data: dict
    timestamp: datetime
    client_id: str


class SyncActionCreate(SyncActionBase):
    pass


class SyncAction(SyncActionBase):
    id: int
    processed: bool = False
    error_message: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class SyncBatchRequest(BaseModel):
    actions: List[SyncActionCreate]
    client_id: str


class SyncBatchResponse(BaseModel):
    processed: int
    failed: int
    errors: List[dict] = []
