import re
import asyncio
import logging
from typing import List, Optional
from fastapi import FastAPI, HTTPException, Request, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import yt_dlp
from yt_dlp.utils import DownloadError, ExtractorError

from models import ExtractRequest, ExtractResponse, FormatOption, ErrorResponse
from database import init_db, get_db, ExtractionLog, AsyncSession

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("blackhole-backend")

app = FastAPI(
    title="BlackHole Media Extractor API",
    description="High-performance, async media extraction service powered by yt-dlp for Instagram, TikTok, Facebook, Twitter/X, and YouTube.",
    version="1.0.0"
)

# Enable CORS for Flutter mobile apps, Web browsers, and localhost testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def on_startup():
    try:
        await init_db()
        logger.info("Database initialized successfully.")
    except Exception as e:
        logger.warning(f"Database initialization deferred: {e}")

def detect_platform(url: str) -> str:
    url_lower = url.lower()
    if "instagram.com" in url_lower:
        return "Instagram"
    elif "tiktok.com" in url_lower:
        return "TikTok"
    elif "twitter.com" in url_lower or "x.com" in url_lower:
        return "Twitter / X"
    elif "facebook.com" in url_lower or "fb.watch" in url_lower:
        return "Facebook"
    elif "youtube.com" in url_lower or "youtu.be" in url_lower:
        return "YouTube"
    return "Web Stream"

def human_readable_size(size_bytes: Optional[int]) -> str:
    if not size_bytes or size_bytes <= 0:
        return "Approx. 15-30 MB"
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0
    return f"{size_bytes:.1f} GB"

def format_seconds(seconds: Optional[int]) -> str:
    if not seconds:
        return "0:00"
    m, s = divmod(seconds, 60)
    h, m = divmod(m, 60)
    if h > 0:
        return f"{h}:{m:02d}:{s:02d}"
    return f"{m}:{s:02d}"

def extract_with_ytdlp(url: str) -> dict:
    """
    Synchronous yt-dlp extraction with tuned parameters for high speed and anti-bot evasions.
    """
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'extract_flat': False,
        'socket_timeout': 15,
        'noplaylist': True,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.5'
        }
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        return ydl.sanitize_info(info)

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "BlackHole Media Extractor", "yt_dlp_version": yt_dlp.version.__version__}

@app.post("/extract", response_model=ExtractResponse, responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def extract_media(
    payload: ExtractRequest,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    url = payload.url.strip()
    if not url.startswith("http://") and not url.startswith("https://"):
        raise HTTPException(status_code=400, detail="Invalid URL format. Please include http:// or https://")

    platform = detect_platform(url)
    client_ip = request.client.host if request.client else "unknown"

    try:
        # Run CPU-bound extraction inside an executor thread to keep FastAPI event loop unblocked
        loop = asyncio.get_event_loop()
        info = await loop.run_in_executor(None, extract_with_ytdlp, url)
    except (DownloadError, ExtractorError) as e:
        logger.error(f"yt-dlp extraction error for {url}: {e}")
        # Log failure asynchronously
        try:
            log_entry = ExtractionLog(source_url=url, platform=platform, client_ip=client_ip, status="FAILED")
            db.add(log_entry)
            await db.commit()
        except Exception:
            pass
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unable to extract media from the provided URL. The link may be private, expired, or rate-limited: {str(e)}"
        )
    except Exception as e:
        logger.exception(f"Unexpected extraction error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Internal extraction engine error: {str(e)}"
        )

    # Parse and build format options
    title = info.get("title") or "Extracted Media"
    uploader = info.get("uploader") or info.get("channel") or platform
    duration = info.get("duration") or 0
    thumbnail = info.get("thumbnail") or ""

    formats_raw = info.get("formats") or []
    extracted_formats: List[FormatOption] = []

    # Filter and categorize formats (1080p, 720p, 480p, best audio)
    seen_resolutions = set()

    for f in formats_raw:
        stream_url = f.get("url")
        if not stream_url:
            continue

        vcodec = f.get("vcodec", "none")
        acodec = f.get("acodec", "none")
        height = f.get("height")
        ext = f.get("ext", "mp4")
        filesize = f.get("filesize") or f.get("filesize_approx")

        # Audio stream
        if vcodec == "none" and acodec != "none":
            if "audio_stream" not in seen_resolutions:
                seen_resolutions.add("audio_stream")
                extracted_formats.append(
                    FormatOption(
                        format_id=str(f.get("format_id", "audio")),
                        label="MP3 / M4A High Quality Audio",
                        resolution=f"{f.get('abr', 192)} kbps",
                        ext="mp3" if ext in ["mp3", "m4a"] else ext,
                        filesize_bytes=filesize,
                        filesize_human=human_readable_size(filesize),
                        is_audio_only=True,
                        url=stream_url
                    )
                )

        # Video stream with audio or combined
        if height and height >= 360 and vcodec != "none":
            res_key = f"{height}p"
            if res_key not in seen_resolutions:
                seen_resolutions.add(res_key)
                label = f"MP4 {res_key} HD" if height >= 720 else f"MP4 {res_key} SD"
                if height >= 1080:
                    label = "MP4 1080p Full HD"

                extracted_formats.append(
                    FormatOption(
                        format_id=str(f.get("format_id", f"{height}p")),
                        label=label,
                        resolution=f"{f.get('width', 'auto')}x{height}",
                        ext="mp4",
                        filesize_bytes=filesize,
                        filesize_human=human_readable_size(filesize),
                        is_audio_only=False,
                        url=stream_url
                    )
                )

    # Fallback to direct url if no explicit formats listed
    if not extracted_formats and info.get("url"):
        extracted_formats.append(
            FormatOption(
                format_id="direct",
                label="MP4 Direct Stream",
                resolution="Original HD",
                ext="mp4",
                filesize_bytes=info.get("filesize"),
                filesize_human=human_readable_size(info.get("filesize")),
                is_audio_only=False,
                url=info.get("url")
            )
        )

    # Sort formats with highest resolution first, audio at bottom
    extracted_formats.sort(key=lambda x: (x.is_audio_only, -int(re.search(r'\d+', x.resolution).group(0)) if re.search(r'\d+', x.resolution) else 0))

    # Async log success into database
    try:
        log_entry = ExtractionLog(source_url=url, platform=platform, title=title[:250], client_ip=client_ip, status="SUCCESS")
        db.add(log_entry)
        await db.commit()
    except Exception as e:
        logger.warning(f"Failed to record extraction log: {e}")

    return ExtractResponse(
        success=True,
        title=title,
        uploader=uploader,
        duration=duration,
        duration_string=format_seconds(duration),
        thumbnail=thumbnail,
        platform=platform,
        original_url=url,
        formats=extracted_formats
    )
