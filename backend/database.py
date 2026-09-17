import os
from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, Text, Index
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import declarative_base

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./blackhole.db")

# Convert standard postgres:// or postgresql:// to postgresql+asyncpg:// if provided
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql+asyncpg://", 1)
elif DATABASE_URL.startswith("postgresql://") and not DATABASE_URL.startswith("postgresql+asyncpg://"):
    DATABASE_URL = DATABASE_URL.replace("postgresql://", "postgresql+asyncpg://", 1)

# Concurrency-optimized engine settings
is_sqlite = DATABASE_URL.startswith("sqlite")
engine_kwargs = {}
if not is_sqlite:
    engine_kwargs = {
        "pool_size": 25,          # Persistent connection pool
        "max_overflow": 50,       # Bursts under heavy concurrency
        "pool_timeout": 30,       # Seconds to wait before timing out
        "pool_recycle": 1800,     # Recycle connections every 30m
        "pool_pre_ping": True     # Health check connection before checkout
    }

engine = create_async_engine(DATABASE_URL, **engine_kwargs)
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False
)

Base = declarative_base()

class ExtractionLog(Base):
    __tablename__ = "extraction_logs"

    id = Column(Integer, primary_key=True, index=True)
    source_url = Column(Text, nullable=False)
    platform = Column(String(50), nullable=False, index=True)
    title = Column(String(255), nullable=True)
    client_ip = Column(String(64), nullable=True, index=True)
    status = Column(String(32), default="SUCCESS")
    created_at = Column(DateTime, default=datetime.utcnow, index=True)

    __table_args__ = (
        Index("ix_extraction_platform_time", "platform", "created_at"),
    )

async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

async def get_db():
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()
