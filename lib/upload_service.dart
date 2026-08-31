import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';

enum UploadStatus { pending, uploading, success, error }

class UploadTaskItem {
  final String id;
  final String videoPath;
  final String csvPath;
  final int durationSeconds;
  final DateTime createdAt;
  double videoProgress;
  double csvProgress;
  UploadStatus status;
  String errorMessage;

  UploadTaskItem({
    required this.id,
    required this.videoPath,
    required this.csvPath,
    required this.durationSeconds,
    required this.createdAt,
    this.videoProgress = 0.0,
    this.csvProgress = 0.0,
    this.status = UploadStatus.pending,
    this.errorMessage = '',
  });
}

class UploadQueueManager extends ChangeNotifier {
  static final UploadQueueManager instance = UploadQueueManager._internal();
  UploadQueueManager._internal();

  final List<UploadTaskItem> tasks = [];
  final UploadService _service = UploadService.instance;

  void addAndStartUpload({
    required String videoPath,
    required String csvPath,
    required int durationSeconds,
  }) {
    final task = UploadTaskItem(
      id: const Uuid().v4(),
      videoPath: videoPath,
      csvPath: csvPath,
      durationSeconds: durationSeconds,
      createdAt: DateTime.now(),
    );

    tasks.insert(0, task);
    notifyListeners();

    _executeUpload(task);
  }

  void retryUpload(UploadTaskItem task) {
    task.status = UploadStatus.pending;
    task.errorMessage = '';
    task.videoProgress = 0.0;
    task.csvProgress = 0.0;
    notifyListeners();

    _executeUpload(task);
  }

  Future<void> _executeUpload(UploadTaskItem task) async {
    task.status = UploadStatus.uploading;
    notifyListeners();

    try {
      await _service.startUploadProcess(
        categoryId: 1,
        videoFile: File(task.videoPath),
        csvFile: File(task.csvPath),
        durationSeconds: task.durationSeconds,
        frequencyHz: 100,
        onVideoProgress: (prog) {
          task.videoProgress = prog;
          notifyListeners();
        },
        onCsvProgress: (prog) {
          task.csvProgress = prog;
          notifyListeners();
        },
      );

      task.status = UploadStatus.success;
      notifyListeners();
    } catch (e) {
      task.status = UploadStatus.error;
      task.errorMessage = e.toString().replaceAll('Exception: ', '');
      notifyListeners();
    }
  }
}

class UploadService {
  static final UploadService instance = UploadService._internal();
  UploadService._internal();

  final Dio _dio = Dio();
  final Uuid _uuid = const Uuid();
  
  // Base URL default (Dapat disesuaikan)
  // Untuk Emulator: 10.0.2.2 | Untuk HP Fisik via ADB Reverse: 127.0.0.1 atau IP Wi-Fi PC
  String baseUrl = 'http://127.0.0.1:8000/api'; 
  String bearerToken = ''; 

  Future<void> startUploadProcess({
    required int categoryId,
    required File videoFile,
    required File csvFile,
    required int durationSeconds,
    required int frequencyHz,
    required Function(double) onVideoProgress,
    required Function(double) onCsvProgress,
  }) async {
    final recordingUuid = _uuid.v4();

    // TUGAS 2: PIVOT TO LOCAL STORAGE
    // Menggunakan FormData untuk mengirim Video dan CSV sekaligus
    final formData = FormData.fromMap({
      'category_id': categoryId,
      'duration_seconds': durationSeconds,
      'recording_uuid': recordingUuid,
      'video': await MultipartFile.fromFile(
        videoFile.path,
        filename: 'video.mp4',
      ),
      'sensor': await MultipartFile.fromFile(
        csvFile.path,
        filename: 'sensor.csv',
      ),
    });

    try {
      await _dio.post(
        '$baseUrl/mobile/recordings/upload',
        data: formData,
        options: Options(
          headers: {
            'Authorization': 'Bearer $bearerToken',
            'Accept': 'application/json',
          },
          // Longgar timeout untuk file besar
          sendTimeout: const Duration(minutes: 5),
          receiveTimeout: const Duration(minutes: 5),
        ),
        onSendProgress: (count, total) {
          if (total != -1) {
            double progress = count / total;
            // Karena upload sekaligus, kita gabung progresnya
            onVideoProgress(progress);
            onCsvProgress(progress);
          }
        },
      );
    } on DioException catch (e) {
      if (e.type == DioExceptionType.connectionTimeout || e.type == DioExceptionType.sendTimeout || e.type == DioExceptionType.receiveTimeout) {
        throw Exception('Koneksi terputus (Timeout). Server lambat atau ukuran file terlalu besar.');
      }
      throw Exception('Gagal mengupload ke Server Lokal: ${e.message}');
    } catch (e) {
      throw Exception('Terjadi kesalahan: $e');
    }
  }
}
