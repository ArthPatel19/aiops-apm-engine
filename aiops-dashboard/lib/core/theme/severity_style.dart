import 'package:flutter/material.dart';

/// Central place that turns a raw backend string ("CRITICAL", "HIGH", ...)
/// into a color. Keeping this in one spot means the Incident Board, the
/// Incident Detail screen, and the Health Card all stay visually consistent
/// even though they're built independently.
class SeverityStyle {
  SeverityStyle._();

  static Color colorFor(String severity) {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
        return const Color(0xFFFF5C5C);
      case 'HIGH':
        return const Color(0xFFFF9F43);
      case 'MEDIUM':
        return const Color(0xFFFFD166);
      case 'LOW':
      default:
        return const Color(0xFF4CD787);
    }
  }

  static Color healthColor(int score) {
    if (score >= 80) return const Color(0xFF4CD787);
    if (score >= 50) return const Color(0xFFFFD166);
    return const Color(0xFFFF5C5C);
  }

  static IconData iconFor(String severity) {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
        return Icons.error_rounded;
      case 'HIGH':
        return Icons.warning_amber_rounded;
      case 'MEDIUM':
        return Icons.info_rounded;
      default:
        return Icons.check_circle_rounded;
    }
  }
}
