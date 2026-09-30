import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/theme/severity_style.dart';
import '../../../core/widgets/state_views.dart';
import 'health_bloc.dart';

/// "Health Overview Card: Display overall system health score and error
/// count." - straight from the assignment brief.
class HealthOverviewCard extends StatelessWidget {
  const HealthOverviewCard({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<HealthBloc, HealthState>(
      builder: (context, state) {
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: _buildContent(context, state),
          ),
        );
      },
    );
  }

  Widget _buildContent(BuildContext context, HealthState state) {
    if (state is HealthLoading || state is HealthInitial) {
      return const SizedBox(height: 96, child: LoadingView());
    }
    if (state is HealthLoadFailure) {
      return SizedBox(
        height: 96,
        child: ErrorView(
          message: state.message,
          onRetry: () => context.read<HealthBloc>().add(const HealthRequested()),
        ),
      );
    }

    final status = (state as HealthLoaded).status;
    final color = SeverityStyle.healthColor(status.score);

    return Row(
      children: [
        SizedBox(
          width: 84,
          height: 84,
          child: Stack(
            alignment: Alignment.center,
            children: [
              CircularProgressIndicator(
                value: status.score.clamp(0, 100) / 100,
                strokeWidth: 7,
                backgroundColor: AppTheme.surfaceHighlight,
                valueColor: AlwaysStoppedAnimation(color),
              ),
              Text(
                '${status.score}',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 24),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'System Health',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 4),
              Text(
                _healthLabel(status.score),
                style: TextStyle(color: color, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _statChip(
                    icon: Icons.report_gmailerrorred_rounded,
                    label: 'Active incidents',
                    value: '${status.incidentCount}',
                  ),
                  const SizedBox(width: 20),
                  _statChip(
                    icon: Icons.remove_circle_outline_rounded,
                    label: 'Penalty points',
                    value: '${status.totalPenalty}',
                  ),
                ],
              ),
            ],
          ),
        ),
        IconButton(
          icon: const Icon(Icons.refresh_rounded, color: AppTheme.textSecondary),
          onPressed: () => context.read<HealthBloc>().add(const HealthRequested()),
        ),
      ],
    );
  }

  Widget _statChip({required IconData icon, required String label, required String value}) {
    return Row(
      children: [
        Icon(icon, size: 16, color: AppTheme.textSecondary),
        const SizedBox(width: 6),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(width: 4),
        Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
      ],
    );
  }

  String _healthLabel(int score) {
    if (score >= 80) return 'Healthy';
    if (score >= 50) return 'Degraded';
    return 'Critical';
  }
}
