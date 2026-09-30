import 'package:flutter/foundation.dart';

/// Central place for backend connection settings.
///
/// Android emulators cannot reach the host machine via `localhost` - they
/// need the special alias `10.0.2.2`. Web, iOS simulator, and desktop can
/// all use `localhost` directly. We branch on [defaultTargetPlatform] instead
/// of `dart:io`'s `Platform` class, because `dart:io` is not safe to use on
/// Flutter Web builds.
class ApiConstants {
  ApiConstants._();

  static const int backendPort = 8080;

  static String get baseUrl {
    if (kIsWeb) {
      return 'http://localhost:$backendPort/api';
    }
    if (defaultTargetPlatform == TargetPlatform.android) {
      // 10.0.2.2 is the Android emulator's alias for the host machine.
      // If you are running on a REAL Android device, replace this with
      // your computer's LAN IP address, e.g. http://192.168.1.20:8080/api
      return 'http://10.0.2.2:$backendPort/api';
    }
    return 'http://localhost:$backendPort/api';
  }

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);

  // Endpoints (relative to baseUrl)
  static const String health = '/health';
  static const String logs = '/logs';
  static const String incidents = '/incidents';

  static String incidentById(int id) => '/incidents/$id';
  static String incidentAnalyze(int id) => '/incidents/$id/analyze';
}
