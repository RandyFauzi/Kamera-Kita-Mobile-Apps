import 'dart:io';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'upload_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp, // Mengunci UI agar seperti apk kamera standar
  ]);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
  ));
  runApp(const KameraKitaApp());
}

class KameraKitaApp extends StatelessWidget {
  const KameraKitaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'KameraKita AI',
      theme: ThemeData.light().copyWith(
        scaffoldBackgroundColor: const Color(0xFFF8FAFC), // Slate 900
        colorScheme: const ColorScheme.light(
          primary: Color(0xFF0284C7), // Primary Cyan Blue #0284c7
          secondary: Color(0xFFFDE047), // Cuan Yellow #fde047
          surface: Color(0xFFFFFFFF), // Slate 800
        ),
      ),
      home: const MainScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  // Indeks 2 adalah 'Rekam' (Kamera Utama di Tengah)
  int _currentIndex = 2;

  final List<Widget> _pages = [
    const HomeDashboardTab(), // 0. Dashboard Ringkasan
    const Center(             // 1. Riwayat Rekaman
      child: Text(
        "Riwayat Rekaman Belum Ada",
        style: TextStyle(fontSize: 14, color: const Color(0xFF64748B), fontFamily: 'monospace'),
      ),
    ),
    const RecordTab(),        // 2. TENGAN: Kamera & Fitur Rekam
    const UploadsTab(),       // 3. Status Upload Cloud
    const ProfileTab(),       // 4. Pengaturan Profil & Server
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _pages,
      ),
      // BOTOM NAV DENGAN DESIGN SAMA PERSIS DENGAN WEB KAMERAKITA AI
      bottomNavigationBar: Builder(
        builder: (context) {
          final double bottomPadding = MediaQuery.of(context).padding.bottom;
          return Container(
            height: 80 + bottomPadding,
            padding: EdgeInsets.only(bottom: bottomPadding),
            decoration: const BoxDecoration(
              color: Color(0xFFF8FAFC), // Slate 900 Web Theme
              border: Border(top: BorderSide(color: Color(0xFFFFFFFF), width: 1)),
            ),
            child: Stack(
              alignment: Alignment.center,
              clipBehavior: Clip.none,
              children: [
                // Row Item Navigasi (Kiri & Kanan)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      // 0. Dashboard
                      _buildNavItem(
                        icon: Icons.grid_view_rounded,
                        label: 'Dashboard',
                        index: 0,
                      ),
                      // 1. Rekaman
                      _buildNavItem(
                        icon: Icons.history_rounded,
                        label: 'Riwayat',
                        index: 1,
                      ),
                      
                      // Spacer untuk Tombol Mengambang (Rekam) di Tengah
                      const SizedBox(width: 60),

                      // 3. Uploads
                      _buildNavItem(
                        icon: Icons.cloud_upload_rounded,
                        label: 'Uploads',
                        index: 3,
                      ),
                      // 4. Pengaturan
                      _buildNavItem(
                        icon: Icons.person_rounded,
                        label: 'Profil',
                        index: 4,
                      ),
                    ],
                  ),
                ),

                // TOMBOL TENGAH MENGAMBANG (CENTER FAB) UNTUK REKAM (ADAPTIF SYSTEM NAV)
                Positioned(
                  top: -24,
                  child: GestureDetector(
                    onTap: () {
                      setState(() => _currentIndex = 2);
                    },
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        // Glowing Glow Ambient Web Effect
                        Container(
                          width: 68,
                          height: 68,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: const LinearGradient(
                              colors: [Color(0xFFFF4D6D), Color(0xFF3B82F6)],
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: const Color(0xFF0284C7).withOpacity(0.6),
                                blurRadius: 20,
                                spreadRadius: 2,
                              ),
                            ],
                          ),
                        ),
                        // Fab Button Gradient
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 250),
                          width: 60,
                          height: 60,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: const LinearGradient(
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                              colors: [Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF4F46E5)],
                            ),
                            border: Border.all(color: const Color(0xFF0F172A).withOpacity(0.4), width: 2),
                            boxShadow: const [
                              BoxShadow(color: Colors.black38, blurRadius: 8, offset: Offset(0, 4)),
                            ],
                          ),
                          child: Icon(
                            _currentIndex == 2 ? Icons.videocam_rounded : Icons.camera_alt_rounded,
                            color: Colors.white,
                            size: 28,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildNavItem({
    required IconData icon,
    required String label,
    required int index,
  }) {
    bool isActive = _currentIndex == index;
    return InkWell(
      onTap: () => setState(() => _currentIndex = index),
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 250),
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: isActive ? const Color(0xFF0284C7).withOpacity(0.2) : Colors.transparent,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                icon,
                size: 22,
                color: isActive ? const Color(0xFF0284C7) : const Color(0xFF94A3B8),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                fontSize: 10,
                fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
                color: isActive ? const Color(0xFF0284C7) : const Color(0xFF94A3B8),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// === TAB DASHBOARD RINGKASAN (MODIFIED WEB LOOK) ===
class HomeDashboardTab extends StatelessWidget {
  const HomeDashboardTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF1F5F9),
      appBar: AppBar(
        backgroundColor: const Color(0xFFF1F5F9),
        elevation: 0,
        title: RichText(
          text: const TextSpan(
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18),
            children: [
              TextSpan(text: 'KameraKita', style: TextStyle(color: Color(0xFF0F172A))),
              TextSpan(text: 'AI', style: TextStyle(color: Color(0xFF2563EB))),
            ],
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // --- GREETING SECTION ---
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text("Halo, Mitra! 👋", style: TextStyle(color: Color(0xFF0F172A), fontSize: 24, fontWeight: FontWeight.w900)),
                    SizedBox(height: 4),
                    Text("Siap mengumpulkan dataset hari ini?", style: TextStyle(color: Color(0xFF64748B), fontSize: 13, fontWeight: FontWeight.w500)),
                  ],
                ),
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: const Color(0xFFDBEAFE),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFFBFDBFE)),
                  ),
                  child: const Icon(Icons.person_rounded, color: Color(0xFF2563EB), size: 28),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Container(
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 20, offset: const Offset(0, 4)),
                ],
              ),
              child: Column(
                children: [
                  Stack(
                    children: [
                      Positioned(
                        top: -20,
                        right: -20,
                        child: Container(
                          width: 120,
                          height: 120,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: RadialGradient(
                              colors: [
                                const Color(0xFF3B82F6).withOpacity(0.2),
                                const Color(0xFFF97316).withOpacity(0.1),
                                Colors.transparent,
                              ],
                            ),
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              "ESTIMASI PENDAPATAN (RP50.000/JAM)",
                              style: TextStyle(color: Color(0xFF64748B), fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 1.2),
                            ),
                            const SizedBox(height: 8),
                            const Text(
                              "Rp0",
                              style: TextStyle(color: Color(0xFF0F172A), fontSize: 36, fontWeight: FontWeight.w900),
                            ),
                            const SizedBox(height: 24),
                            Row(
                              children: [
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: const [
                                    Text("TOTAL JAM", style: TextStyle(color: Color(0xFF64748B), fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: 1.0)),
                                    SizedBox(height: 4),
                                    Text("0m", style: TextStyle(color: Color(0xFF0F172A), fontSize: 16, fontWeight: FontWeight.bold)),
                                  ],
                                ),
                                Container(
                                  height: 30,
                                  width: 1,
                                  color: Colors.grey.withOpacity(0.3),
                                  margin: const EdgeInsets.symmetric(horizontal: 24),
                                ),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: const [
                                    Text("KETERANGAN", style: TextStyle(color: Color(0xFF64748B), fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: 1.0)),
                                    SizedBox(height: 4),
                                    Text("Gaji berdasar jam approved", style: TextStyle(color: Color(0xFF0F172A), fontSize: 15, fontWeight: FontWeight.bold)),
                                  ],
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8FAFC),
                      borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(28), bottomRight: Radius.circular(28)),
                      border: Border(top: BorderSide(color: Colors.grey.withOpacity(0.15))),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: const [
                        Text("Kerjakan Tugas", style: TextStyle(color: Color(0xFF2563EB), fontSize: 14, fontWeight: FontWeight.w800)),
                        Icon(Icons.arrow_forward_rounded, color: Color(0xFF2563EB), size: 20),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
  
            const Text(
              "STATISTIK PROGRES VIDEO",
              style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.5, fontFamily: 'monospace'),
            ),
            const SizedBox(height: 12),
  
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 20, offset: const Offset(0, 4)),
                ],
              ),
              child: Column(
                children: [
                  const Text("Statistik Anda", style: TextStyle(color: Color(0xFF475569), fontSize: 14, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      Column(
                        children: const [
                          Text("Dikirim", style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.bold)),
                          SizedBox(height: 8),
                          Text("0", style: TextStyle(color: Color(0xFF0F172A), fontSize: 24, fontWeight: FontWeight.w900)),
                        ],
                      ),
                      Container(height: 40, width: 1, color: Colors.grey.withOpacity(0.2)),
                      Column(
                        children: const [
                          Text("Approved", style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.bold)),
                          SizedBox(height: 8),
                          Text("0", style: TextStyle(color: Color(0xFF0F172A), fontSize: 24, fontWeight: FontWeight.w900)),
                        ],
                      ),
                      Container(height: 40, width: 1, color: Colors.grey.withOpacity(0.2)),
                      Column(
                        children: const [
                          Text("Tingkat", style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.bold)),
                          SizedBox(height: 8),
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: const [
                              Text("0", style: TextStyle(color: Color(0xFF0F172A), fontSize: 24, fontWeight: FontWeight.w900)),
                              Padding(
                                padding: EdgeInsets.only(bottom: 4, left: 2),
                                child: Text("%", style: TextStyle(color: Color(0xFF0F172A), fontSize: 14, fontWeight: FontWeight.bold)),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1F5F9),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: Colors.grey.withOpacity(0.2)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: const [
                          Icon(Icons.star_rounded, color: Color(0xFFE11D48), size: 16),
                          SizedBox(width: 6),
                          Text("Selalu perhatikan kualitas", style: TextStyle(color: Color(0xFF2563EB), fontSize: 12, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 20, offset: const Offset(0, 4)),
                ],
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: const Color(0xFFEFF6FF),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Icon(Icons.play_arrow_rounded, color: Color(0xFF2563EB), size: 28),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: const [
                            Text("Ayo Mulai! Semangat kerjakan", style: TextStyle(color: Color(0xFF0F172A), fontSize: 15, fontWeight: FontWeight.bold)),
                            SizedBox(height: 2),
                            Text("video pertama Anda hari ini.", style: TextStyle(color: Color(0xFF64748B), fontSize: 13)),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  Stack(
                    alignment: Alignment.center,
                    children: [
                      Container(
                        height: 8,
                        width: double.infinity,
                        decoration: BoxDecoration(color: const Color(0xFFF1F5F9), borderRadius: BorderRadius.circular(10)),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Container(width: 8, height: 8, decoration: const BoxDecoration(color: Color(0xFF0EA5E9), shape: BoxShape.circle)),
                          Container(width: 6, height: 6, decoration: const BoxDecoration(color: Color(0xFFCBD5E1), shape: BoxShape.circle)),
                          Container(width: 6, height: 6, decoration: const BoxDecoration(color: Color(0xFFCBD5E1), shape: BoxShape.circle)),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: const [
                      Text("0 jam diselesaikan", style: TextStyle(color: Color(0xFF64748B), fontSize: 12, fontWeight: FontWeight.w600)),
                      Text("Total tertinggi 6 jam", style: TextStyle(color: Color(0xFF64748B), fontSize: 12, fontWeight: FontWeight.w600)),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: Colors.grey.withOpacity(0.1)),
                boxShadow: [
                  BoxShadow(color: Colors.black.withOpacity(0.02), blurRadius: 10, offset: const Offset(0, 2)),
                ],
              ),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: const Color(0xFFF0F9FF),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: const Color(0xFFBAE6FD).withOpacity(0.5)),
                    ),
                    child: const Icon(Icons.emoji_events_outlined, color: Color(0xFF0284C7), size: 24),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text("Papan Peringkat", style: TextStyle(color: Color(0xFF0F172A), fontSize: 16, fontWeight: FontWeight.w900)),
                        SizedBox(height: 2),
                        Text("Lihat posisi Anda di antara mitra lainnya!", style: TextStyle(color: Color(0xFF64748B), fontSize: 12)),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: const BoxDecoration(
                      color: Color(0xFFF8FAFC),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.chevron_right_rounded, color: Color(0xFF475569)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: Colors.grey.withOpacity(0.1)),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: const [
                      Text("Riwayat Laporan Video Terakhir", style: TextStyle(color: Color(0xFF0F172A), fontSize: 14, fontWeight: FontWeight.bold)),
                      Text("Lihat semua", style: TextStyle(color: Color(0xFF2563EB), fontSize: 13, fontWeight: FontWeight.w800)),
                    ],
                  ),
                  const SizedBox(height: 40),
                  const Text("Belum ada riwayat laporan video dikirim.", style: TextStyle(color: Color(0xFF475569), fontSize: 13)),
                  const SizedBox(height: 40),
                ],
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}


class RecordTab extends StatefulWidget {
  const RecordTab({super.key});

  @override
  State<RecordTab> createState() => _RecordTabState();
}

class _RecordTabState extends State<RecordTab> {
  static const platform = MethodChannel('kamerakita.ai/camera_sensor');
  static const handEventChannel = EventChannel('kamerakita.ai/hand_detection');
  static const orientationChannel = EventChannel('kamerakita.ai/orientation');

  int? _textureId;
  bool _isRecording = false;
  bool _isCalibrating = false;
  int _calibrationCountdown = 3;
  int _handCount = 0;
  DateTime? _recordingStartTime;
  
  String _orientation = 'UNKNOWN';

  @override
  void initState() {
    super.initState();
    _startCameraPreview();
    
    handEventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (mounted) {
        setState(() {
          _handCount = event as int;
        });
      }
    });

    orientationChannel.receiveBroadcastStream().listen((dynamic event) {
      if (mounted) {
        setState(() {
          _orientation = event.toString();
        });
      }
    });
  }

  Future<void> _startCameraPreview() async {
    try {
      final int textureId = await platform.invokeMethod('startCamera');
      if (mounted) {
        setState(() => _textureId = textureId);
      }
    } catch (e) {
      print("Camera Error: $e");
    }
  }

  Future<void> _stopRecording() async {
    try {
      final Map<dynamic, dynamic> resultData = await platform.invokeMethod('stopRecording');
      
      if (!resultData.containsKey('payload')) {
         throw Exception("Invalid payload from Native");
      }

      final payloadStr = resultData['payload'];
      final payload = jsonDecode(payloadStr);

      final mp4Path = payload['encrypted_video_path'];
      final csvPath = payload['encrypted_imu_path'];
      
      // Phase 1 Mock Edge AI
      double handPercentage = 100.0; // Assume 100 for now, as HandAnalyzer metadata needs wire up
      
      // Check Orientation Integrity
      final captureMeta = payload['capture'];
      if (captureMeta != null && captureMeta['orientation_integrity'] == 'FAILED_ORIENTATION_POLICY') {
         handPercentage = 0.0; // Force reject
      }

      final durationSec = _recordingStartTime != null
          ? DateTime.now().difference(_recordingStartTime!).inSeconds
          : 5;

      setState(() {
        _isRecording = false;
      });

      // --- EDGE AI SMART VALIDATION & INTEGRITY ---
      if (handPercentage < 90.0 || (captureMeta != null && captureMeta['orientation_integrity'] == 'FAILED_ORIENTATION_POLICY')) {
        // Hapus file secara lokal
        try {
          if (mp4Path != null) File(mp4Path).deleteSync();
          if (csvPath != null) File(csvPath).deleteSync();
        } catch (e) {
          debugPrint("Gagal menghapus file reject: $e");
        }

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Row(
                children: [
                  const Icon(Icons.error_outline, color: Colors.white, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      captureMeta != null && captureMeta['orientation_integrity'] == 'FAILED_ORIENTATION_POLICY' 
                        ? 'Ditolak: Terjadi rotasi perangkat ke Portrait saat merekam!'
                        : 'Ditolak: Tangan terlihat hanya ${handPercentage.toStringAsFixed(1)}%. Minimal 90%!',
                      style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
                    ),
                  ),
                ],
              ),
              backgroundColor: const Color(0xFFEF4444), // Merah untuk peringatan
              duration: const Duration(seconds: 4),
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
          );
        }
        return; // Hentikan proses, jangan di-upload!
      }

      // Jika LOLOS QC, LANGSUNG JALANKAN UPLOAD DI LATAR BELAKANG
      UploadQueueManager.instance.addAndStartUpload(
        videoPath: mp4Path,
        csvPath: csvPath,
        durationSeconds: durationSec > 0 ? durationSec : 5,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle_rounded, color: Colors.white, size: 20),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Lolos QC & Enkripsi. Mengunggah di latar belakang...',
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
                  ),
                ),
              ],
            ),
            backgroundColor: const Color(0xFF10B981), // Emerald 500
            duration: const Duration(seconds: 3),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        );
      }
    } catch (e) {
      print("Stop Recording Error: $e");
      setState(() {
        _isRecording = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error menyimpan rekaman: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_textureId == null) {
      return const Center(
        child: CircularProgressIndicator(color: Color(0xFF0F172A)),
      );
    }

    final isLandscape = _orientation == 'LANDSCAPE_LEFT' || _orientation == 'LANDSCAPE_RIGHT';

    return Stack(
      children: [
        // Camera Feed
        Positioned.fill(
          child: Container(
            color: Colors.black,
            child: Center(
              child: AspectRatio(
                aspectRatio: 9.0 / 16.0, // Rasio layar portrait (1080x1920)
                child: RotatedBox(
                  quarterTurns: 1, // Memutar sensor murni tanpa merusak matriks OpenGL (Anti-Jelly)
                  child: Texture(textureId: _textureId!),
                ),
              ),
            ),
          ),
        ),

        // Live Hand Tracking Overlay
        if (_handCount > 0)
          Positioned(
            top: 40,
            left: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withOpacity(0.9), // Emerald
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: Colors.white.withOpacity(0.3)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.back_hand_rounded, color: Colors.white, size: 16),
                  const SizedBox(width: 6),
                  Text(
                    "Tangan Terdeteksi: $_handCount",
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ],
              ),
            ),
          ),
          
        // Orientation Enforcer Overlay
        if (!isLandscape)
          Positioned.fill(
            child: Container(
              color: Colors.black87,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.screen_rotation_rounded, color: Colors.white, size: 64),
                  const SizedBox(height: 24),
                  const Text("ROTATE YOUR PHONE", style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900, letterSpacing: 2.0)),
                  const SizedBox(height: 8),
                  const Text("Landscape recording is required", style: TextStyle(color: Color(0xFF94A3B8), fontSize: 14)),
                ],
              ),
            ),
          ),
          
        if (isLandscape && !_isRecording)
          Positioned(
            top: 40,
            right: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xFF0F172A).withOpacity(0.8),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: const Color(0xFF334155)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: const [
                  Icon(Icons.landscape_rounded, color: Color(0xFF38BDF8), size: 16),
                  SizedBox(width: 6),
                  Text(
                    "READY Landscape Mode",
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ],
              ),
            ),
          ),

        // Calibration UI
        if (_isCalibrating)
          Positioned.fill(
            child: Container(
              color: Colors.black87,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text("KALIBRASI SENSOR", style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold, letterSpacing: 2)),
                  const SizedBox(height: 20),
                  Text("$_calibrationCountdown", style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 80, fontWeight: FontWeight.w900)),
                  const SizedBox(height: 20),
                  const Text("Posisikan kamera ke objek", style: TextStyle(color: Colors.white70, fontSize: 14)),
                ],
              ),
            ),
          ),

        // Control Panel Bawah
        if (!_isCalibrating)
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: Container(
              padding: const EdgeInsets.only(bottom: 24, top: 40),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.bottomCenter,
                  end: Alignment.topCenter,
                  colors: [Colors.black.withOpacity(0.8), Colors.transparent],
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  // Flash Toggle (Mock)
                  IconButton(
                    icon: const Icon(Icons.flash_off_rounded, color: Colors.white),
                    onPressed: () {},
                  ),
                  
                  // Record Button
                  GestureDetector(
                    onTap: () async {
                      if (!isLandscape) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Harap putar HP ke mode Landscape!'), backgroundColor: Colors.red),
                        );
                        return;
                      }

                      if (_isRecording) {
                        await _stopRecording();
                      } else {
                        // Start Calibration
                        setState(() {
                          _isCalibrating = true;
                          _calibrationCountdown = 3;
                        });
                        
                        for (int i = 3; i > 0; i--) {
                          setState(() => _calibrationCountdown = i);
                          await Future.delayed(const Duration(seconds: 1));
                        }
                        
                        setState(() => _isCalibrating = false);

                        try {
                          await platform.invokeMethod('startRecording');
                          setState(() {
                            _isRecording = true;
                            _recordingStartTime = DateTime.now();
                          });
                        } catch (e) {
                          print("Start Recording Error: $e");
                          if (e.toString().contains("ORIENTATION_INVALID")) {
                             ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('Gagal merekam: Orientasi tidak valid!'), backgroundColor: Colors.red),
                             );
                          }
                        }
                      }
                    },
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 300),
                      width: 72,
                      height: 72,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 4),
                        color: _isRecording ? Colors.transparent : (isLandscape ? const Color(0xFFEF4444) : Colors.grey),
                      ),
                      child: Center(
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 300),
                          width: _isRecording ? 28 : (isLandscape ? 60 : 60),
                          height: _isRecording ? 28 : (isLandscape ? 60 : 60),
                          decoration: BoxDecoration(
                            color: isLandscape ? const Color(0xFFEF4444) : Colors.grey,
                            borderRadius: BorderRadius.circular(_isRecording ? 8 : 30),
                          ),
                        ),
                      ),
                    ),
                  ),
                  
                  // Switch Camera Toggle (Mock)
                  IconButton(
                    icon: const Icon(Icons.cameraswitch_rounded, color: Colors.white),
                    onPressed: () {},
                  ),
                ],
              ),
            ),
          ),
          
        // Recording Indicator
        if (_isRecording)
          Positioned(
            top: 40,
            left: 0,
            right: 0,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: const Color(0xFFEF4444).withOpacity(0.9),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    children: const [
                      Icon(Icons.fiber_manual_record, color: Colors.white, size: 12),
                      SizedBox(width: 8),
                      Text("MEREKAM...", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)),
                    ],
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}





class UploadsTab extends StatefulWidget {
  const UploadsTab({Key? key}) : super(key: key);
  @override
  _UploadsTabState createState() => _UploadsTabState();
}

class _UploadsTabState extends State<UploadsTab> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Uploads')),
      body: const Center(child: Text('Upload Queue')),
    );
  }
}

class ProfileTab extends StatelessWidget {
  const ProfileTab({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: const Center(child: Text('Profile Settings')),
    );
  }
}
