from fastapi import APIRouter

router = APIRouter(tags=["Service"])


@router.get("/health")
def health():
    return {"status": "ok"}
