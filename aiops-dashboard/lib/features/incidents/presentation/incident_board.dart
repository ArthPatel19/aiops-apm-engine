import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/state_views.dart';
import '../domain/incident.dart';
import 'incident_board_bloc.dart';
import 'incident_detail_view.dart';
import 'incident_widgets.dart';

class IncidentBoard extends StatelessWidget {
  const IncidentBoard({super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Incident Board',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                IconButton(
                  icon: const Icon(Icons.refresh_rounded, size: 20, color: AppTheme.textSecondary),
                  onPressed: () =>
                      context.read<IncidentBoardBloc>().add(const IncidentBoardRequested()),
                ),
              ],
            ),
            const Divider(height: 20),
            BlocBuilder<IncidentBoardBloc, IncidentBoardState>(
              builder: (context, state) {
                if (state is IncidentBoardLoading || state is IncidentBoardInitial) {
                  return const SizedBox(height: 300, child: LoadingView());
                }
                if (state is IncidentBoardFailure) {
                  return SizedBox(
                    height: 300,
                    child: ErrorView(
                      message: state.message,
                      onRetry: () =>
                          context.read<IncidentBoardBloc>().add(const IncidentBoardRequested()),
                    ),
                  );
                }
                final incidents = (state as IncidentBoardLoaded).incidents;
                if (incidents.isEmpty) {
                  return const SizedBox(
                    height: 200,
                    child: EmptyView(
                      message: 'No incidents yet. Send some logs to /api/logs to see them here.',
                      icon: Icons.check_circle_outline_rounded,
                    ),
                  );
                }
                return ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: incidents.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 10),
                  itemBuilder: (context, index) {
                    final incident = incidents[index];
                    return IncidentCard(
                      incident: incident,
                      onTap: () => _openDetail(context, incident),
                    );
                  },
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  void _openDetail(BuildContext context, Incident incident) {
    if (isWideScreen(context)) {
      showDialog(
        context: context,
        builder: (_) => Dialog(
          insetPadding: const EdgeInsets.symmetric(horizontal: 120, vertical: 60),
          child: IncidentDetailView(incidentId: incident.id),
        ),
      );
    } else {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => Scaffold(
            appBar: AppBar(title: const Text('Incident Details')),
            body: IncidentDetailView(incidentId: incident.id),
          ),
        ),
      );
    }
  }
}
