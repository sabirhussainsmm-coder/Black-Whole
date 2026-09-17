from pydantic import BaseModel, HttpUrl, Field
from typing import List, Optional

class ExtractRequest(BaseModel):
    url: str = Field(..., description="Target media URL from Instagram, TikTok, Twitter/X, Facebook, YouTube, etc.")

class FormatOption(BaseModel):
    format_id: str
    label: str
    resolution: str
    ext: str
    filesize_bytes: Optional[int] = None
    filesize_human: str
    is_audio_only: bool = False
    url: str

class ExtractResponse(BaseModel):
    success: bool = True
    title: str
    uploader: str
    duration: Optional[int] = 0
    duration_string: str
    thumbnail: str
    platform: str
    original_url: str
    formats: List[FormatOption]

class ErrorResponse(BaseModel):
    success: bool = False
    error: str
    detail: Optional[str] = None
