from fastapi import APIRouter, WebSocket, WebSocketDisconnect

router = APIRouter()


@router.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    await ws.accept()
    try:
        while True:
            _ = await ws.receive_text()
            await ws.send_text("ack")
    except WebSocketDisconnect:
        pass
