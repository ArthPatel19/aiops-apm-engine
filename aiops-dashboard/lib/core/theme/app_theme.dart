import 'package:flutter/material.dart';

class AppTheme {
  AppTheme._();

  static const Color background = Color(0xFF0F1115);
  static const Color surface = Color(0xFF171A21);
  static const Color surfaceHighlight = Color(0xFF1F232C);
  static const Color primary = Color(0xFF4C8DFF);
  static const Color textPrimary = Color(0xFFEAECEF);
  static const Color textSecondary = Color(0xFF9AA3AF);
  static const Color border = Color(0xFF2A2F3A);

  static ThemeData get darkTheme {
    final base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: background,
      colorScheme: base.colorScheme.copyWith(
        primary: primary,
        surface: surface,
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: Color(0xFF262B35)),
        ),
        margin: EdgeInsets.zero,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: textPrimary,
        displayColor: textPrimary,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: background,
        elevation: 0,
        foregroundColor: textPrimary,
      ),
      dividerColor: const Color(0xFF262B35),
    );
  }
}
