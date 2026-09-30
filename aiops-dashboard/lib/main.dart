import 'package:flutter/material.dart';

import 'core/theme/app_theme.dart';
import 'injection_container.dart' as di;
import 'presentation/dashboard_page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await di.initDependencies();
  runApp(const ApmDashboardApp());
}

class ApmDashboardApp extends StatelessWidget {
  const ApmDashboardApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AI-Ops APM Engine',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: const DashboardPage(),
    );
  }
}
