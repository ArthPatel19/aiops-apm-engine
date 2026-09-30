// import 'package:flutter/material.dart';
// import 'package:flutter_bloc/flutter_bloc.dart';
// import 'package:get_it/get_it.dart';
//
// import '../core/widgets/state_views.dart';
// import '../features/health/presentation/health_bloc.dart';
// import '../features/health/presentation/health_overview_card.dart';
// import '../features/incidents/presentation/incident_board.dart';
// import '../features/incidents/presentation/incident_board_bloc.dart';
// import '../features/logs/presentation/log_feed_bloc.dart';
// import '../features/logs/presentation/log_feed_widgets.dart';
//
// /// The single screen that satisfies the assignment's Dashboard UI
// /// requirement: Health Overview Card, Live APM Log Feed, and Incident
// /// Board all visible together. Two columns on wide (web/tablet) screens,
// /// stacked on narrow (mobile) screens - one codebase, both targets.
// class DashboardPage extends StatelessWidget {
//   const DashboardPage({super.key});
//
//   // @override
//   // Widget build(BuildContext context) {
//   //   return MultiBlocProvider(
//   //     providers: [
//   //       BlocProvider(
//   //         create: (_) => GetIt.instance<HealthBloc>()..add(const HealthRequested()),
//   //       ),
//   //       BlocProvider(
//   //         create: (_) => GetIt.instance<LogFeedBloc>()..add(const LogFeedRequested()),
//   //       ),
//   //       BlocProvider(
//   //         create: (_) =>
//   //             GetIt.instance<IncidentBoardBloc>()..add(const IncidentBoardRequested()),
//   //       ),
//   //     ],
//   //     child: Scaffold(
//   //       appBar: AppBar(
//   //         title: const Text('AI-Ops APM Engine'),
//   //         actions: [
//   //           IconButton(
//   //             tooltip: 'Refresh everything',
//   //             icon: const Icon(Icons.sync_rounded),
//   //             onPressed: () => _refreshAll(context),
//   //           ),
//   //           const SizedBox(width: 8),
//   //         ],
//   //       ),
//   //       body: RefreshIndicator(
//   //         onRefresh: () async => _refreshAll(context),
//   //         child: SingleChildScrollView(
//   //           physics: const AlwaysScrollableScrollPhysics(),
//   //           padding: const EdgeInsets.all(16),
//   //           child: isWideScreen(context) ? _wideLayout() : _narrowLayout(),
//   //         ),
//   //       ),
//   //     ),
//   //   );
//   // }
//
//
//   @override
//   Widget build(BuildContext context) {
//     return MultiBlocProvider(
//       providers: [
//         BlocProvider(
//           create: (_) =>
//           GetIt.instance<HealthBloc>()..add(const HealthRequested()),
//         ),
//         BlocProvider(
//           create: (_) =>
//           GetIt.instance<LogFeedBloc>()..add(const LogFeedRequested()),
//         ),
//         BlocProvider(
//           create: (_) =>
//           GetIt.instance<IncidentBoardBloc>()
//             ..add(const IncidentBoardRequested()),
//         ),
//       ],
//       child: Builder(
//         builder: (context) {
//           return Scaffold(
//             appBar: AppBar(
//               title: const Text('AI-Ops APM Engine'),
//               actions: [
//                 IconButton(
//                   tooltip: 'Refresh everything',
//                   icon: const Icon(Icons.sync_rounded),
//                   onPressed: () => _refreshAll(context),
//                 ),
//                 const SizedBox(width: 8),
//               ],
//             ),
//             body: RefreshIndicator(
//               onRefresh: () async => _refreshAll(context),
//               child: SingleChildScrollView(
//                 physics: const AlwaysScrollableScrollPhysics(),
//                 padding: const EdgeInsets.all(16),
//                 child: isWideScreen(context)
//                     ? _wideLayout()
//                     : _narrowLayout(),
//               ),
//             ),
//           );
//         },
//       ),
//     );
//   }
//
//   void _refreshAll(BuildContext context) {
//     context.read<HealthBloc>().add(const HealthRequested());
//     context.read<LogFeedBloc>().add(const LogFeedRequested());
//     context.read<IncidentBoardBloc>().add(const IncidentBoardRequested());
//   }
//
//   Widget _wideLayout() {
//     return Column(
//       crossAxisAlignment: CrossAxisAlignment.stretch,
//       children: [
//         const HealthOverviewCard(),
//         const SizedBox(height: 16),
//         Row(
//           crossAxisAlignment: CrossAxisAlignment.start,
//           children: const [
//             Expanded(
//               flex: 5,
//               child: LogFeedList(),
//             ),
//             SizedBox(width: 16),
//             Expanded(
//               flex: 6,
//               child: IncidentBoard(),
//             ),
//           ],
//         ),
//       ],
//     );
//   }
//
//   Widget _narrowLayout() {
//     return const Column(
//       crossAxisAlignment: CrossAxisAlignment.stretch,
//       children: [
//         HealthOverviewCard(),
//         SizedBox(height: 16),
//         IncidentBoard(),
//         SizedBox(height: 16),
//         LogFeedList(),
//       ],
//     );
//   }
// }


import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';

import '../core/theme/app_theme.dart';
import '../core/widgets/state_views.dart';
import '../features/health/presentation/health_bloc.dart';
import '../features/health/presentation/health_overview_card.dart';
import '../features/incidents/presentation/incident_board.dart';
import '../features/incidents/presentation/incident_board_bloc.dart';
import '../features/logs/presentation/log_feed_bloc.dart';
import '../features/logs/presentation/log_feed_widgets.dart';

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (_) =>
          GetIt.instance<HealthBloc>()..add(const HealthRequested()),
        ),
        BlocProvider(
          create: (_) =>
          GetIt.instance<LogFeedBloc>()..add(const LogFeedRequested()),
        ),
        BlocProvider(
          create: (_) => GetIt.instance<IncidentBoardBloc>()
            ..add(const IncidentBoardRequested()),
        ),
      ],

      // IMPORTANT:
      // This Builder gives us a BuildContext below MultiBlocProvider.
      // Therefore AppBar refresh can safely access the BLoCs.
      child: Builder(
        builder: (context) {
          return Scaffold(
            backgroundColor: AppTheme.background,

            appBar: AppBar(
              backgroundColor: AppTheme.background,
              elevation: 0,
              automaticallyImplyLeading: false,
              toolbarHeight: 76,

              titleSpacing: 24,

              title: const _DashboardHeader(),

              actions: [
                IconButton(
                  tooltip: 'Refresh dashboard',
                  onPressed: () => _refreshAll(context),
                  icon: const Icon(
                    Icons.refresh_rounded,
                    size: 23,
                  ),
                ),
                const SizedBox(width: 16),
              ],
            ),

            body: RefreshIndicator(
              onRefresh: () async {
                _refreshAll(context);

                // Small delay gives the RefreshIndicator time to complete
                // its animation while the BLoCs perform their requests.
                await Future<void>.delayed(
                  const Duration(milliseconds: 400),
                );
              },

              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(
                  24,
                  8,
                  24,
                  32,
                ),
                child: isWideScreen(context)
                    ? _wideLayout()
                    : _narrowLayout(),
              ),
            ),
          );
        },
      ),
    );
  }

  void _refreshAll(BuildContext context) {
    context.read<HealthBloc>().add(
      const HealthRequested(),
    );

    context.read<LogFeedBloc>().add(
      const LogFeedRequested(),
    );

    context.read<IncidentBoardBloc>().add(
      const IncidentBoardRequested(),
    );
  }

  Widget _wideLayout() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const HealthOverviewCard(),

        const SizedBox(height: 20),

        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Expanded(
              flex: 5,
              child: LogFeedList(),
            ),

            SizedBox(width: 20),

            Expanded(
              flex: 6,
              child: IncidentBoard(),
            ),
          ],
        ),
      ],
    );
  }

  Widget _narrowLayout() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        HealthOverviewCard(),

        SizedBox(height: 20),

        IncidentBoard(),

        SizedBox(height: 20),

        LogFeedList(),
      ],
    );
  }
}

/// Professional dashboard header.
///
/// Kept as a private widget because it belongs only to the dashboard shell.
class _DashboardHeader extends StatelessWidget {
  const _DashboardHeader();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // Application mark
        Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            color: AppTheme.primary.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: AppTheme.primary.withValues(alpha: 0.25),
            ),
          ),
          child: const Icon(
            Icons.monitor_heart_outlined,
            color: AppTheme.primary,
            size: 21,
          ),
        ),

        const SizedBox(width: 12),

        // Title + subtitle
        const Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'AI-Ops APM Engine',
              style: TextStyle(
                fontSize: 19,
                fontWeight: FontWeight.w700,
                letterSpacing: -0.3,
              ),
            ),

            SizedBox(height: 3),

            Text(
              'Application Performance Monitoring',
              style: TextStyle(
                fontSize: 11,
                color: AppTheme.textSecondary,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),

        const Spacer(),

        // Dashboard status
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: 11,
            vertical: 7,
          ),
          decoration: BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: AppTheme.border,
            ),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _StatusDot(),
              SizedBox(width: 7),
              Text(
                'Monitoring',
                style: TextStyle(
                  fontSize: 12,
                  color: AppTheme.textSecondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _StatusDot extends StatelessWidget {
  const _StatusDot();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 7,
      height: 7,
      decoration: const BoxDecoration(
        color: Color(0xFF4CD787),
        shape: BoxShape.circle,
      ),
    );
  }
}