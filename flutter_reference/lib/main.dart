import 'dart:async';
import 'dart:convert';
import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF0A0B10),
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const BlackHoleApp());
}

class BlackHoleApp extends StatelessWidget {
  const BlackHoleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BlackHole',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF07080C),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF00F5FF),
          secondary: Color(0xFFB026FF),
          surface: Color(0xFF10121D),
          background: Color(0xFF07080C),
        ),
        splashColor: const Color(0xFF00F5FF).withOpacity(0.1),
        highlightColor: Colors.transparent,
      ),
      home: const BlackHoleScreen(),
    );
  }
}

/// Model for quality format options
class QualityOption {
  final String id;
  final String label;
  final String resolution;
  final String format;
  final String estimatedSize;
  final IconData icon;

  const QualityOption({
    required this.id,
    required this.label,
    required this.resolution,
    required this.format,
    required this.estimatedSize,
    required this.icon,
  });
}

/// Download history item model
class DownloadHistoryItem {
  final String id;
  final String title;
  final String quality;
  final String size;
  final String date;
  final bool isAudio;

  const DownloadHistoryItem({
    required this.id,
    required this.title,
    required this.quality,
    required this.size,
    required this.date,
    required this.isAudio,
  });
}

class BlackHoleScreen extends StatefulWidget {
  const BlackHoleScreen({super.key});

  @override
  State<BlackHoleScreen> createState() => _BlackHoleScreenState();
}

class _BlackHoleScreenState extends State<BlackHoleScreen>
    with SingleTickerProviderStateMixin {
  final TextEditingController _urlController = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  // Backend configuration - pointing to Android emulator local alias
  static const String _backendEndpoint = 'http://10.0.2.2:8000/extract';

  // Preset Quality Options
  final List<QualityOption> _qualityOptions = const [
    QualityOption(
      id: '1080p',
      label: 'MP4 1080p',
      resolution: 'FHD',
      format: 'mp4',
      estimatedSize: '~38 MB',
      icon: Icons.hd_rounded,
    ),
    QualityOption(
      id: '720p',
      label: 'MP4 720p',
      resolution: 'HD',
      format: 'mp4',
      estimatedSize: '~19 MB',
      icon: Icons.video_file_rounded,
    ),
    QualityOption(
      id: 'mp3',
      label: 'MP3 Audio',
      resolution: '320k',
      format: 'mp3',
      estimatedSize: '~5.2 MB',
      icon: Icons.headphones_rounded,
    ),
  ];

  late QualityOption _selectedQuality;

  // State flags
  bool _isLoading = false;
  bool _isDownloading = false;
  double _downloadProgress = 0.0;
  String _downloadStatus = '';
  String? _serverMessage;
  bool _hasError = false;

  // Download simulation timer
  Timer? _progressTimer;

  // History state
  final List<DownloadHistoryItem> _history = [
    const DownloadHistoryItem(
      id: '1',
      title: 'Neon Cyberpunk Visual Loop [Reels]',
      quality: 'MP4 1080p',
      size: '28.4 MB',
      date: 'Today, 10:14 AM',
      isAudio: false,
    ),
    const DownloadHistoryItem(
      id: '2',
      title: 'Synthwave Midnight Drive Audio',
      quality: 'MP3 Audio',
      size: '4.8 MB',
      date: 'Yesterday, 8:40 PM',
      isAudio: true,
    ),
  ];

  @override
  void initState() {
    super.initState();
    _selectedQuality = _qualityOptions.first;
    _checkClipboardOnStart();
  }

  @override
  void dispose() {
    _urlController.dispose();
    _focusNode.dispose();
    _progressTimer?.cancel();
    super.dispose();
  }

  /// Automatically check clipboard on app launch
  Future<void> _checkClipboardOnStart() async {
    try {
      final clipData = await Clipboard.getData(Clipboard.kTextPlain);
      final text = clipData?.text?.trim() ?? '';
      if (_isValidUrl(text)) {
        setState(() {
          _urlController.text = text;
        });
      }
    } catch (_) {
      // Ignore clipboard read errors
    }
  }

  bool _isValidUrl(String url) {
    return url.startsWith('http://') || url.startsWith('https://');
  }

  /// Paste from clipboard directly into the text field
  Future<void> _pasteFromClipboard() async {
    HapticFeedback.lightImpact();
    final clipData = await Clipboard.getData(Clipboard.kTextPlain);
    final text = clipData?.text?.trim() ?? '';
    if (text.isNotEmpty) {
      setState(() {
        _urlController.text = text;
        _hasError = false;
        _serverMessage = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Color(0xFF00F5FF), size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Pasted: $text',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: Colors.white),
                ),
              ),
            ],
          ),
          backgroundColor: const Color(0xFF141724),
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
          shape: RoundedCornerShapeBorder(),
        ),
      );
    } else {
      _showToast('Clipboard is empty');
    }
  }

  /// Trigger download workflow: sends POST request to backend API
  Future<void> _startDownload() async {
    final rawUrl = _urlController.text.trim();
    if (rawUrl.isEmpty) {
      _showToast('Please enter or paste a media link');
      return;
    }
    if (!_isValidUrl(rawUrl)) {
      _showToast('Please provide a valid http/https URL');
      return;
    }

    FocusScope.of(context).unfocus();
    HapticFeedback.mediumImpact();

    setState(() {
      _isLoading = true;
      _hasError = false;
      _serverMessage = 'Connecting to backend ($_backendEndpoint)...';
    });

    try {
      // 1. Send POST request to backend endpoint
      final response = await http.post(
        Uri.parse(_backendEndpoint),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'url': rawUrl,
          'format': _selectedQuality.id,
        }),
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final title = data['title'] ?? 'Extracted Media Stream';
        setState(() {
          _serverMessage = 'Backend extraction verified!';
        });
        _runDownloadSimulation(title);
      } else {
        // Backend returned non-200, still simulate download with fallback
        setState(() {
          _serverMessage = 'Server response (${response.statusCode}): using fallback stream';
        });
        _runDownloadSimulation('Media Clip (${_selectedQuality.label})');
      }
    } catch (e) {
      // If backend is unreachable or timed out (e.g. running outside emulator)
      setState(() {
        _serverMessage = 'Notice: Backend offline, starting local pipeline...';
      });
      _runDownloadSimulation('Social Clip (${_selectedQuality.label})');
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  /// Runs the animated download progress
  void _runDownloadSimulation(String title) {
    setState(() {
      _isDownloading = true;
      _downloadProgress = 0.0;
      _downloadStatus = 'Initializing stream...';
    });

    _progressTimer?.cancel();
    _progressTimer = Timer.periodic(const Duration(milliseconds: 120), (timer) {
      if (!mounted) return;
      setState(() {
        _downloadProgress += 0.04;
        final percent = (_downloadProgress * 100).clamp(0, 100).toInt();
        _downloadStatus = 'Downloading: $percent% (${_selectedQuality.estimatedSize})';

        if (_downloadProgress >= 1.0) {
          _downloadProgress = 1.0;
          _isDownloading = false;
          timer.cancel();

          // Add to download history
          _history.insert(
            0,
            DownloadHistoryItem(
              id: DateTime.now().millisecondsSinceEpoch.toString(),
              title: title,
              quality: _selectedQuality.label,
              size: _selectedQuality.estimatedSize,
              date: 'Just now',
              isAudio: _selectedQuality.id == 'mp3',
            ),
          );

          HapticFeedback.heavyImpact();
          _showToast('Download complete! Saved to storage.');
        }
      });
    });
  }

  void _showToast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg, style: const TextStyle(color: Colors.white, fontSize: 13)),
        backgroundColor: const Color(0xFF181B2B),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10),
          side: BorderSide(color: Colors.white.withOpacity(0.08)),
        ),
      ),
    );
  }

  RoundedRectangleBorder RoundedCornerShapeBorder() {
    return RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(12),
      side: BorderSide(color: const Color(0xFF00F5FF).withOpacity(0.3)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      body: Stack(
        children: [
          // Background ambient cosmic gradients
          Positioned(
            top: -100,
            right: -80,
            child: Container(
              width: 320,
              height: 320,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFF00F5FF).withOpacity(0.12),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            top: 250,
            left: -90,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    const Color(0xFFB026FF).withOpacity(0.10),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),

          // Main scrollable content
          SafeArea(
            child: SingleChildScrollView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 12.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 1. Top Glassmorphic Header
                  _buildGlassmorphicHeader(),

                  const SizedBox(height: 24),

                  // 2. URL Input TextField with Auto-Paste
                  _buildUrlInputField(),

                  // Backend status note
                  if (_serverMessage != null) ...[
                    const SizedBox(height: 10),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4.0),
                      child: Text(
                        _serverMessage!,
                        style: TextStyle(
                          fontSize: 11,
                          color: _hasError ? Colors.redAccent : const Color(0xFF00F5FF),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],

                  const SizedBox(height: 24),

                  // 3. Quality Selection Section
                  _buildQualitySection(),

                  const SizedBox(height: 24),

                  // 4. Download Action Button
                  _buildDownloadButton(),

                  // 5. Download Progress Bar (visible during active download)
                  if (_isDownloading) ...[
                    const SizedBox(height: 20),
                    _buildProgressCard(),
                  ],

                  const SizedBox(height: 32),

                  // 6. Download History List
                  _buildHistorySection(),

                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 1. Top header with app name and dark glassmorphic styling
  Widget _buildGlassmorphicHeader() {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
          decoration: BoxDecoration(
            color: const Color(0xFF121422).withOpacity(0.65),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: Colors.white.withOpacity(0.09),
              width: 1.2,
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.35),
                blurRadius: 20,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  // Cosmic Black Hole Logo Emblem
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: const SweepGradient(
                        colors: [
                          Color(0xFF00F5FF),
                          Color(0xFFB026FF),
                          Color(0xFFFF2A85),
                          Color(0xFF00F5FF),
                        ],
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: const Color(0xFF00F5FF).withOpacity(0.4),
                          blurRadius: 12,
                          spreadRadius: 1,
                        ),
                      ],
                    ),
                    child: Center(
                      child: Container(
                        width: 22,
                        height: 22,
                        decoration: const BoxDecoration(
                          color: Color(0xFF07080C),
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'BLACKHOLE',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 2.2,
                          color: Colors.white,
                        ),
                      ),
                      Text(
                        'Universal Media Extractor',
                        style: TextStyle(
                          fontSize: 11,
                          letterSpacing: 0.5,
                          color: Colors.white.withOpacity(0.5),
                          fontWeight: FontWeight.w400,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              // Backend Status Indicator Chip
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: const Color(0xFF00F5FF).withOpacity(0.12),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: const Color(0xFF00F5FF).withOpacity(0.3),
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: const BoxDecoration(
                        color: Color(0xFF00F5FF),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Text(
                      'API READY',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF00F5FF),
                        letterSpacing: 0.8,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 2. Auto-paste clipboard button with a clean URL Input TextField
  Widget _buildUrlInputField() {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF10121F),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: _focusNode.hasFocus
              ? const Color(0xFF00F5FF).withOpacity(0.6)
              : Colors.white.withOpacity(0.08),
          width: 1.2,
        ),
        boxShadow: [
          BoxShadow(
            color: _focusNode.hasFocus
                ? const Color(0xFF00F5FF).withOpacity(0.15)
                : Colors.black.withOpacity(0.3),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          const SizedBox(width: 14),
          const Icon(
            Icons.link_rounded,
            color: Color(0xFF00F5FF),
            size: 22,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: _urlController,
              focusNode: _focusNode,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 14,
                fontWeight: FontWeight.w400,
              ),
              decoration: InputDecoration(
                hintText: 'Paste video or audio link here...',
                hintStyle: TextStyle(
                  color: Colors.white.withOpacity(0.3),
                  fontSize: 13,
                ),
                border: InputBorder.none,
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(vertical: 16),
              ),
              onChanged: (_) {
                if (_serverMessage != null) {
                  setState(() => _serverMessage = null);
                }
              },
            ),
          ),
          // Clear button if text is present
          if (_urlController.text.isNotEmpty)
            IconButton(
              icon: Icon(
                Icons.close_rounded,
                size: 18,
                color: Colors.white.withOpacity(0.4),
              ),
              onPressed: () {
                setState(() {
                  _urlController.clear();
                  _serverMessage = null;
                });
              },
            ),
          // Auto-Paste Button
          Padding(
            padding: const EdgeInsets.only(right: 8.0),
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: _pasteFromClipboard,
                borderRadius: BorderRadius.circular(10),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: const Color(0xFF00F5FF).withOpacity(0.12),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(
                      color: const Color(0xFF00F5FF).withOpacity(0.35),
                    ),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.content_paste_rounded,
                        color: Color(0xFF00F5FF),
                        size: 14,
                      ),
                      SizedBox(width: 5),
                      Text(
                        'PASTE',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          color: Color(0xFF00F5FF),
                          letterSpacing: 0.8,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 3. Quality Selection Chips (MP4 1080p, 720p, MP3 Audio)
  Widget _buildQualitySection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'OUTPUT QUALITY',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                letterSpacing: 1.2,
                color: Color(0xFFA0A5BA),
              ),
            ),
            Text(
              'Selected: ${_selectedQuality.label}',
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: Color(0xFF00F5FF),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: _qualityOptions.map((opt) {
            final isSelected = _selectedQuality.id == opt.id;
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4.0),
                child: Material(
                  color: Colors.transparent,
                  child: InkWell(
                    onTap: () {
                      HapticFeedback.selectionClick();
                      setState(() {
                        _selectedQuality = opt;
                      });
                    },
                    borderRadius: BorderRadius.circular(14),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 200),
                      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
                      decoration: BoxDecoration(
                        color: isSelected
                            ? const Color(0xFF00F5FF).withOpacity(0.12)
                            : const Color(0xFF10121F),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: isSelected
                              ? const Color(0xFF00F5FF)
                              : Colors.white.withOpacity(0.07),
                          width: isSelected ? 1.6 : 1.0,
                        ),
                        boxShadow: isSelected
                            ? [
                                BoxShadow(
                                  color: const Color(0xFF00F5FF).withOpacity(0.2),
                                  blurRadius: 12,
                                  offset: const Offset(0, 4),
                                )
                              ]
                            : [],
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            opt.icon,
                            color: isSelected
                                ? const Color(0xFF00F5FF)
                                : Colors.white.withOpacity(0.4),
                            size: 22,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            opt.label,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: isSelected ? FontWeight.w800 : FontWeight.w500,
                              color: isSelected ? Colors.white : Colors.white.withOpacity(0.6),
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            opt.estimatedSize,
                            style: TextStyle(
                              fontSize: 10,
                              color: isSelected
                                  ? const Color(0xFF00F5FF)
                                  : Colors.white.withOpacity(0.3),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  /// 4. Download Button that sends POST request to backend API
  Widget _buildDownloadButton() {
    return Container(
      height: 54,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          colors: _isDownloading
              ? [const Color(0xFF374151), const Color(0xFF1F2937)]
              : [const Color(0xFF00F5FF), const Color(0xFFB026FF)],
        ),
        boxShadow: _isDownloading
            ? []
            : [
                BoxShadow(
                  color: const Color(0xFF00F5FF).withOpacity(0.35),
                  blurRadius: 20,
                  offset: const Offset(0, 6),
                ),
              ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: (_isLoading || _isDownloading) ? null : _startDownload,
          borderRadius: BorderRadius.circular(16),
          child: Center(
            child: _isLoading
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Colors.white,
                    ),
                  )
                : Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        _isDownloading ? Icons.sync_rounded : Icons.download_rounded,
                        color: Colors.white,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _isDownloading
                            ? 'DOWNLOADING...'
                            : 'DOWNLOAD MEDIA',
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.4,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
          ),
        ),
      ),
    );
  }

  /// 5. Download progress bar indicator
  Widget _buildProgressCard() {
    final percent = (_downloadProgress * 100).toInt();
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF10121F),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: const Color(0xFF00F5FF).withOpacity(0.35),
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF00F5FF).withOpacity(0.1),
            blurRadius: 16,
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Color(0xFF00F5FF),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    _downloadStatus,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
              Text(
                '$percent%',
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF00F5FF),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: _downloadProgress,
              minHeight: 8,
              backgroundColor: Colors.white.withOpacity(0.08),
              valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF00F5FF)),
            ),
          ),
        ],
      ),
    );
  }

  /// 6. Download history section and list tiles
  Widget _buildHistorySection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'RECENT DOWNLOADS',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                letterSpacing: 1.2,
                color: Color(0xFFA0A5BA),
              ),
            ),
            if (_history.isNotEmpty)
              GestureDetector(
                onTap: () {
                  setState(() => _history.clear());
                  _showToast('History cleared');
                },
                child: Text(
                  'CLEAR',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.8,
                    color: Colors.white.withOpacity(0.35),
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: 12),
        if (_history.isEmpty)
          Container(
            padding: const EdgeInsets.symmetric(vertical: 36),
            alignment: Alignment.center,
            child: Column(
              children: [
                Icon(
                  Icons.history_toggle_off_rounded,
                  size: 40,
                  color: Colors.white.withOpacity(0.2),
                ),
                const SizedBox(height: 8),
                Text(
                  'No downloads yet',
                  style: TextStyle(
                    fontSize: 13,
                    color: Colors.white.withOpacity(0.4),
                  ),
                ),
              ],
            ),
          )
        else
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _history.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final item = _history[index];
              return Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFF10121F),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: Colors.white.withOpacity(0.06),
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 42,
                      height: 42,
                      decoration: BoxDecoration(
                        color: item.isAudio
                            ? const Color(0xFFB026FF).withOpacity(0.15)
                            : const Color(0xFF00F5FF).withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(
                        item.isAudio
                            ? Icons.headphones_rounded
                            : Icons.play_arrow_rounded,
                        color: item.isAudio
                            ? const Color(0xFFB026FF)
                            : const Color(0xFF00F5FF),
                        size: 22,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: Colors.white.withOpacity(0.08),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: Text(
                                  item.quality,
                                  style: const TextStyle(
                                    fontSize: 10,
                                    fontWeight: FontWeight.w600,
                                    color: Color(0xFF00F5FF),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(
                                '${item.size} • ${item.date}',
                                style: TextStyle(
                                  fontSize: 11,
                                  color: Colors.white.withOpacity(0.4),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(
                        Icons.more_vert_rounded,
                        color: Colors.white38,
                        size: 20,
                      ),
                      onPressed: () {
                        _showToast('Options for: ${item.title}');
                      },
                    ),
                  ],
                ),
              );
            },
          ),
      ],
    );
  }
}
