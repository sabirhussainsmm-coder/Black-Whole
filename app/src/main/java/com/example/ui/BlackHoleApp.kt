package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.model.ActiveDownloadState
import com.example.model.ExtractedMedia
import com.example.model.FormatOption
import com.example.model.SupportedPlatform
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlackHoleApp(
  viewModel: BlackHoleViewModel,
  modifier: Modifier = Modifier
) {
  val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
  val clipboardUrl by viewModel.clipboardDetectedUrl.collectAsStateWithLifecycle()
  val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
  val extractError by viewModel.extractError.collectAsStateWithLifecycle()
  val extractedMedia by viewModel.extractedMedia.collectAsStateWithLifecycle()
  val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
  val activeDownload by viewModel.activeDownload.collectAsStateWithLifecycle()
  val historyList by viewModel.historyList.collectAsStateWithLifecycle()
  val backendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()
  val useCustomBackend by viewModel.useCustomBackend.collectAsStateWithLifecycle()
  val showInterstitialAd by viewModel.showInterstitialAd.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Auto-detect clipboard on screen launch
  LaunchedEffect(Unit) {
    viewModel.checkClipboard()
  }

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CosmicBlack)
      .padding(top = statusBarPadding, bottom = navBarPadding)
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Top App Bar
      TopHeader(
        onSettingsClick = { showSettingsDialog = true }
      )

      // Navigation Tabs (Extractor vs History)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = CosmicSurface,
        contentColor = NeonCyan,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = NeonCyan,
            height = 3.dp
          )
        },
        divider = {
          HorizontalDivider(color = CosmicBorder, thickness = 1.dp)
        },
        modifier = Modifier.testTag("main_tabs")
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Extractor", fontWeight = FontWeight.SemiBold)
            }
          },
          modifier = Modifier.testTag("tab_extractor")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("History (${historyList.size})", fontWeight = FontWeight.SemiBold)
            }
          },
          modifier = Modifier.testTag("tab_history")
        )
      }

      // AdMob Top Banner Placeholder (Clean, non-intrusive banner)
      AdMobBannerWidget(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

      // Main Content Tabs
      if (selectedTab == 0) {
        ExtractorTabContent(
          urlInput = urlInput,
          clipboardUrl = clipboardUrl,
          isExtracting = isExtracting,
          extractError = extractError,
          extractedMedia = extractedMedia,
          selectedFormat = selectedFormat,
          activeDownload = activeDownload,
          onUrlChange = viewModel::onUrlChanged,
          onPaste = viewModel::pasteFromClipboard,
          onClear = viewModel::clearInput,
          onUseClipboard = viewModel::useDetectedClipboardUrl,
          onDismissClipboard = viewModel::dismissClipboardBanner,
          onPlatformSelect = { platform ->
            viewModel.onUrlChanged(platform.sampleUrl)
            viewModel.startExtraction(platform.sampleUrl)
          },
          onExtract = { viewModel.startExtraction() },
          onSelectFormat = viewModel::selectFormat,
          onStartDownload = viewModel::startDownload,
          onPauseResume = viewModel::togglePauseDownload,
          onCancelDownload = viewModel::cancelDownload
        )
      } else {
        HistoryTabContent(
          historyList = historyList,
          onDeleteItem = viewModel::deleteHistoryItem,
          onClearAll = viewModel::clearAllHistory
        )
      }
    }

    // Settings / Backend Configuration Dialog
    if (showSettingsDialog) {
      BackendConfigDialog(
        currentUrl = backendUrl,
        useCustom = useCustomBackend,
        onSave = { newUrl, enabled ->
          viewModel.setBackendConfig(newUrl, enabled)
          showSettingsDialog = false
        },
        onDismiss = { showSettingsDialog = false }
      )
    }

    // Interstitial Ad Trigger Dialog (simulates AdMob interstitial post-download)
    if (showInterstitialAd) {
      InterstitialAdOverlay(
        onDismiss = viewModel::dismissInterstitialAd
      )
    }
  }
}

@Composable
fun TopHeader(onSettingsClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(
            Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonPink, NeonCyan))
          )
          .border(1.5.dp, NeonCyan, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(CosmicBlack)
        )
      }
      Spacer(Modifier.width(12.dp))
      Column {
        Text(
          text = "BLACKHOLE",
          fontSize = 19.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          color = TextPrimary,
          fontFamily = FontFamily.SansSerif
        )
        Text(
          text = "Fast Media Extractor",
          fontSize = 11.sp,
          color = NeonCyan,
          letterSpacing = 0.5.sp
        )
      }
    }

    IconButton(
      onClick = onSettingsClick,
      modifier = Modifier
        .testTag("settings_button")
        .clip(CircleShape)
        .background(CosmicSurfaceVariant)
    ) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Backend Settings",
        tint = TextSecondary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
fun ExtractorTabContent(
  urlInput: String,
  clipboardUrl: String?,
  isExtracting: Boolean,
  extractError: String?,
  extractedMedia: ExtractedMedia?,
  selectedFormat: FormatOption?,
  activeDownload: ActiveDownloadState,
  onUrlChange: (String) -> Unit,
  onPaste: () -> Unit,
  onClear: () -> Unit,
  onUseClipboard: () -> Unit,
  onDismissClipboard: () -> Unit,
  onPlatformSelect: (SupportedPlatform) -> Unit,
  onExtract: () -> Unit,
  onSelectFormat: (FormatOption) -> Unit,
  onStartDownload: () -> Unit,
  onPauseResume: () -> Unit,
  onCancelDownload: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Clipboard Auto-Detect Floating Banner
    item {
      AnimatedVisibility(
        visible = clipboardUrl != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
      ) {
        if (clipboardUrl != null) {
          ClipboardDetectedCard(
            detectedUrl = clipboardUrl,
            onUse = onUseClipboard,
            onDismiss = onDismissClipboard
          )
        }
      }
    }

    // 2. Central Glowing BlackHole Portal / Action
    item {
      GlowingPortalHero(
        isExtracting = isExtracting,
        isDownloading = activeDownload.isDownloading,
        onPortalClick = {
          if (extractedMedia != null && !activeDownload.isDownloading) {
            onStartDownload()
          } else {
            onExtract()
          }
        }
      )
    }

    // 3. URL Input Bar with Paste & Clear
    item {
      UrlInputBar(
        urlInput = urlInput,
        onUrlChange = onUrlChange,
        onPaste = onPaste,
        onClear = onClear,
        onExtract = onExtract,
        isExtracting = isExtracting
      )
    }

    // Quick Platform Presets / Supported Badges
    item {
      PlatformSelectorRow(onPlatformSelect = onPlatformSelect)
    }

    // Error feedback
    if (extractError != null) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1621)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = extractError,
            color = Color(0xFFFF8080),
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
    }

    // 4. Download Progress Card (if active or just completed)
    if (activeDownload.isDownloading || activeDownload.isCompleted) {
      item {
        DownloadProgressCard(
          activeDownload = activeDownload,
          onPauseResume = onPauseResume,
          onCancel = onCancelDownload
        )
      }
    }

    // 5. Extracted Media Preview & Format Selector
    if (extractedMedia != null) {
      item {
        MediaDetailsCard(media = extractedMedia)
      }

      item {
        FormatSelectorCard(
          formats = extractedMedia.formats,
          selectedFormat = selectedFormat,
          onSelectFormat = onSelectFormat
        )
      }

      item {
        Button(
          onClick = onStartDownload,
          enabled = !activeDownload.isDownloading && selectedFormat != null,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("download_now_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (activeDownload.isDownloading) "Downloading..." else "Download ${selectedFormat?.label ?: "Media"}",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun ClipboardDetectedCard(
  detectedUrl: String,
  onUse: () -> Unit,
  onDismiss: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("clipboard_detect_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(NeonCyan.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
      }
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Link detected in clipboard",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = NeonCyan
        )
        Text(
          text = detectedUrl,
          fontSize = 12.sp,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(Modifier.width(8.dp))
      Button(
        onClick = onUse,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp).testTag("clipboard_use_button")
      ) {
        Text("Extract", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextTertiary, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
fun GlowingPortalHero(
  isExtracting: Boolean,
  isDownloading: Boolean,
  onPortalClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "blackhole_spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (isExtracting || isDownloading) 3000 else 12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(130.dp)
        .clickable(onClick = onPortalClick)
        .testTag("blackhole_portal_button"),
      contentAlignment = Alignment.Center
    ) {
      // Outer Cosmic Accretion Glow Disk
      Box(
        modifier = Modifier
          .size(126.dp)
          .rotate(rotation)
          .clip(CircleShape)
          .background(
            Brush.sweepGradient(
              listOf(
                NeonCyan,
                NeonPurple.copy(alpha = 0.9f),
                NeonPink,
                Color.Transparent,
                NeonCyan
              )
            )
          )
      )

      // Middle Ring
      Box(
        modifier = Modifier
          .size(108.dp)
          .rotate(-rotation * 1.5f)
          .clip(CircleShape)
          .background(
            Brush.sweepGradient(
              listOf(
                Color.Transparent,
                NeonPurple,
                NeonCyan.copy(alpha = 0.8f),
                Color.Transparent
              )
            )
          )
      )

      // Inner Event Horizon (Black Void)
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(CosmicBlack)
          .border(2.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (isExtracting) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = NeonCyan,
            strokeWidth = 3.dp
          )
        } else {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (isDownloading) Icons.Default.Download else Icons.Default.Link,
              contentDescription = "Portal Action",
              tint = if (isDownloading) NeonGreen else NeonCyan,
              modifier = Modifier.size(30.dp)
            )
            Text(
              text = if (isDownloading) "SAVING" else "EXTRACT",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              color = TextPrimary
            )
          }
        }
      }
    }

    Spacer(Modifier.height(8.dp))
    Text(
      text = if (isExtracting) "Analyzing stream protocols & codecs..." else "Paste link or tap portal to download",
      fontSize = 12.sp,
      color = TextSecondary
    )
  }
}

@Composable
fun UrlInputBar(
  urlInput: String,
  onUrlChange: (String) -> Unit,
  onPaste: () -> Unit,
  onClear: () -> Unit,
  onExtract: () -> Unit,
  isExtracting: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CosmicBorder, NeonPurple.copy(alpha = 0.4f))))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = urlInput,
        onValueChange = onUrlChange,
        placeholder = {
          Text("Paste Instagram, TikTok, Twitter/X URL...", color = TextTertiary, fontSize = 13.sp)
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          cursorColor = NeonCyan
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("url_text_input")
      )

      if (urlInput.isNotEmpty()) {
        IconButton(
          onClick = onClear,
          modifier = Modifier.testTag("clear_url_button")
        ) {
          Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
      } else {
        IconButton(
          onClick = onPaste,
          modifier = Modifier.testTag("paste_url_button")
        ) {
          Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan, modifier = Modifier.size(20.dp))
        }
      }

      Button(
        onClick = onExtract,
        enabled = !isExtracting && urlInput.isNotBlank(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = NeonPurple,
          contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.testTag("extract_submit_button")
      ) {
        if (isExtracting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Extract", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
fun PlatformSelectorRow(onPlatformSelect: (SupportedPlatform) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "SUPPORTED SOURCES & QUICK SAMPLES",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      color = TextTertiary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(SupportedPlatform.values()) { platform ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = CosmicSurfaceVariant,
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(platform.badgeColor).copy(alpha = 0.6f), CosmicBorder))),
          modifier = Modifier
            .clickable { onPlatformSelect(platform) }
            .testTag("platform_chip_${platform.name.lowercase()}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(platform.badgeColor))
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = platform.displayName,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = TextPrimary
            )
          }
        }
      }
    }
  }
}

@Composable
fun MediaDetailsCard(media: ExtractedMedia) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.3f), CosmicBorder))),
    modifier = Modifier.fillMaxWidth().testTag("media_preview_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(CosmicSurfaceElevated)
      ) {
        AsyncImage(
          model = media.thumbnailUrl,
          contentDescription = "Thumbnail",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        // Duration Tag
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text(text = media.duration, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NeonPurple.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(text = media.platform, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = media.title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "By ${media.author}",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
fun FormatSelectorCard(
  formats: List<FormatOption>,
  selectedFormat: FormatOption?,
  onSelectFormat: (FormatOption) -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
    modifier = Modifier.fillMaxWidth().testTag("format_selector_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = "SELECT DOWNLOAD FORMAT & RESOLUTION",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextTertiary
      )
      Spacer(Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        formats.forEach { format ->
          val isSelected = selectedFormat?.id == format.id
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) CosmicSurfaceElevated else CosmicSurfaceVariant,
            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectFormat(format) }
              .testTag("format_item_${format.id}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (format.isAudioOnly) NeonPink.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (format.isAudioOnly) Icons.Default.Audiotrack else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (format.isAudioOnly) NeonPink else NeonCyan,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                  Text(
                    text = format.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) NeonCyan else TextPrimary
                  )
                  Text(
                    text = "${format.resolution} • ${format.extension.uppercase()}",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = format.estimatedSize,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) NeonCyan else TextTertiary
                )
                Spacer(Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                      width = 1.5.dp,
                      color = if (isSelected) NeonCyan else CosmicBorder,
                      shape = CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DownloadProgressCard(
  activeDownload: ActiveDownloadState,
  onPauseResume: () -> Unit,
  onCancel: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (activeDownload.isCompleted) listOf(NeonGreen, NeonCyan) else listOf(NeonCyan, NeonPurple)
      )
    ),
    modifier = Modifier.fillMaxWidth().testTag("download_progress_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (activeDownload.isCompleted) Icons.Default.CheckCircle else Icons.Default.Download,
            contentDescription = null,
            tint = if (activeDownload.isCompleted) NeonGreen else NeonCyan,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (activeDownload.isCompleted) "Download Completed" else if (activeDownload.isPaused) "Paused" else "Downloading Media",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        }

        if (!activeDownload.isCompleted) {
          Row {
            IconButton(onClick = onPauseResume, modifier = Modifier.size(32.dp).testTag("pause_resume_button")) {
              Icon(
                imageVector = if (activeDownload.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Pause / Resume",
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp).testTag("cancel_download_button")) {
              Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      LinearProgressIndicator(
        progress = { activeDownload.progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = if (activeDownload.isCompleted) NeonGreen else NeonCyan,
        trackColor = CosmicSurfaceVariant
      )

      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${(activeDownload.progress * 100).toInt()}% • ${activeDownload.downloadedSize} / ${activeDownload.totalSize}",
          fontSize = 12.sp,
          color = TextSecondary
        )
        if (!activeDownload.isCompleted && !activeDownload.isPaused) {
          Text(
            text = String.format("%.1f MB/s", activeDownload.speedMBs),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
          )
        }
      }

      Spacer(Modifier.height(4.dp))
      Text(
        text = activeDownload.statusMessage,
        fontSize = 11.sp,
        color = TextTertiary
      )
    }
  }
}

@Composable
fun HistoryTabContent(
  historyList: List<DownloadEntity>,
  onDeleteItem: (Long) -> Unit,
  onClearAll: () -> Unit
) {
  if (historyList.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(CosmicSurfaceElevated),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
      }
      Spacer(Modifier.height(16.dp))
      Text(
        text = "No Downloads Yet",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Extracted videos and audio files will appear here for instant offline access.",
        fontSize = 13.sp,
        color = TextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "SAVED FILES (${historyList.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TextTertiary
          )
          TextButton(onClick = onClearAll, modifier = Modifier.testTag("clear_history_button")) {
            Text("Clear All", fontSize = 12.sp, color = NeonPink)
          }
        }
      }

      items(historyList, key = { it.id }) { item ->
        HistoryItemRow(item = item, onDelete = { onDeleteItem(item.id) })
      }
    }
  }
}

@Composable
fun HistoryItemRow(
  item: DownloadEntity,
  onDelete: () -> Unit
) {
  val dateStr = remember(item.timestamp) {
    val formatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    formatter.format(Date(item.timestamp))
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CosmicBorder, CosmicBorder))),
    modifier = Modifier.fillMaxWidth().testTag("history_item_${item.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(CosmicSurfaceElevated)
      ) {
        AsyncImage(
          model = item.thumbnailUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(NeonCyan.copy(alpha = 0.15f))
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(text = item.formatLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
          }
          Spacer(Modifier.width(6.dp))
          Text(text = item.platform, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = item.title,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = "$dateStr • ${item.fileSize}",
          fontSize = 11.sp,
          color = TextTertiary
        )
      }

      IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_history_${item.id}")) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
      }
    }
  }
}

// -------------------------------------------------------------
// ADMOB / ADSTERRA AD WIDGETS & COMPLIANT TRIGGERS
// -------------------------------------------------------------

@Composable
fun AdMobBannerWidget(modifier: Modifier = Modifier) {
  // Google AdMob Adaptive / Standard Banner Placeholder (320x50 standard)
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CosmicBorder, CosmicBorder))),
    modifier = modifier
      .fillMaxWidth()
      .height(50.dp)
      .testTag("admob_banner_placeholder")
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF4285F4).copy(alpha = 0.2f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
          Text("AD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
        }
        Spacer(Modifier.width(10.dp))
        Column {
          Text("AdMob Adaptive Banner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("Ca-app-pub-3940256099942544/6300978111", fontSize = 9.sp, color = TextTertiary)
        }
      }

      Text("320x50", fontSize = 10.sp, color = TextTertiary)
    }
  }
}

@Composable
fun InterstitialAdOverlay(onDismiss: () -> Unit) {
  var countdown by remember { mutableIntStateOf(3) }

  LaunchedEffect(Unit) {
    while (countdown > 0) {
      kotlinx.coroutines.delay(1000)
      countdown--
    }
  }

  Dialog(onDismissRequest = {
    if (countdown <= 0) onDismiss()
  }) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
      border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("interstitial_ad_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF34A853).copy(alpha = 0.2f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("SPONSORED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF34A853))
          }

          if (countdown > 0) {
            Text(
              text = "Skip in ${countdown}s",
              fontSize = 12.sp,
              color = TextTertiary
            )
          } else {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(28.dp).testTag("close_interstitial_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close Ad", tint = TextPrimary)
            }
          }
        }

        Spacer(Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              Brush.radialGradient(listOf(NeonPurple.copy(alpha = 0.3f), CosmicSurfaceVariant))
            )
            .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
              text = "High Speed Media Downloader",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "AdMob / Adsterra compliant interstitial placeholder",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          enabled = countdown <= 0,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("continue_to_app_button")
        ) {
          Text(
            text = if (countdown > 0) "Reward in ${countdown}s..." else "Continue to App",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun BackendConfigDialog(
  currentUrl: String,
  useCustom: Boolean,
  onSave: (String, Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  var urlText by remember { mutableStateOf(currentUrl) }
  var useCustomServer by remember { mutableStateOf(useCustom) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CosmicSurface,
    title = {
      Text("Backend Extraction Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    },
    text = {
      Column {
        Text(
          text = "Configure Python FastAPI backend endpoint. When disabled, BlackHole runs its built-in client stream extractor.",
          fontSize = 12.sp,
          color = TextSecondary
        )
        Spacer(Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Use Custom FastAPI Server", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
          Switch(
            checked = useCustomServer,
            onCheckedChange = { useCustomServer = it },
            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = CosmicSurfaceElevated),
            modifier = Modifier.testTag("backend_toggle_switch")
          )
        }

        if (useCustomServer) {
          Spacer(Modifier.height(12.dp))
          OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("FastAPI Host URL") },
            placeholder = { Text("https://my-backend.onrender.com") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = CosmicBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().testTag("backend_url_input")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(urlText, useCustomServer) },
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
        modifier = Modifier.testTag("save_backend_config_button")
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
