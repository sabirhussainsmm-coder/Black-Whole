package com.example.model

data class FormatOption(
  val id: String,
  val label: String,
  val resolution: String,
  val extension: String,
  val estimatedSize: String,
  val isAudioOnly: Boolean = false,
  val downloadUrl: String? = null
)

data class ExtractedMedia(
  val title: String,
  val author: String,
  val duration: String,
  val thumbnailUrl: String,
  val originalUrl: String,
  val platform: String,
  val formats: List<FormatOption>
)

data class ActiveDownloadState(
  val isDownloading: Boolean = false,
  val progress: Float = 0f,
  val speedMBs: Float = 0f,
  val downloadedSize: String = "0 MB",
  val totalSize: String = "0 MB",
  val statusMessage: String = "",
  val currentMedia: ExtractedMedia? = null,
  val selectedFormat: FormatOption? = null,
  val isPaused: Boolean = false,
  val isCompleted: Boolean = false
)

enum class SupportedPlatform(
  val displayName: String,
  val domainPattern: String,
  val badgeColor: Long,
  val sampleUrl: String
) {
  INSTAGRAM(
    displayName = "Instagram",
    domainPattern = "instagram.com",
    badgeColor = 0xFFE1306C,
    sampleUrl = "https://www.instagram.com/reel/C8xyzABC123/"
  ),
  TIKTOK(
    displayName = "TikTok",
    domainPattern = "tiktok.com",
    badgeColor = 0xFF00F2FE,
    sampleUrl = "https://www.tiktok.com/@creator/video/738291048291"
  ),
  TWITTER(
    displayName = "Twitter / X",
    domainPattern = "x.com",
    badgeColor = 0xFF1DA1F2,
    sampleUrl = "https://x.com/tech_insider/status/1792348129381"
  ),
  FACEBOOK(
    displayName = "Facebook",
    domainPattern = "facebook.com",
    badgeColor = 0xFF1877F2,
    sampleUrl = "https://www.facebook.com/watch/?v=9823471928"
  ),
  YOUTUBE(
    displayName = "YouTube",
    domainPattern = "youtube.com",
    badgeColor = 0xFFFF0000,
    sampleUrl = "https://youtu.be/dQw4w9WgXcQ"
  ),
  GENERIC(
    displayName = "Direct / Web",
    domainPattern = "",
    badgeColor = 0xFF8B5CF6,
    sampleUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
  )
}
