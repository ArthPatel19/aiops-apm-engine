import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/theme/severity_style.dart';
import '../../../core/widgets/state_views.dart';
import '../domain/incident.dart';
import 'incident_detail_bloc.dart';
import 'incident_widgets.dart';

/// "AI Incident Details Screen/Modal: Tap an incident to view the
/// AI-generated root-cause summary, suggested fix, and copy-pasteable
/// Jira/Slack payloads." - this widget is that screen.
class IncidentDetailView extends StatelessWidget {
  final int incidentId;
  const IncidentDetailView({super.key, required this.incidentId});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => GetIt.instance<IncidentDetailBloc>()
        ..add(IncidentDetailRequested(incidentId)),
      child: _IncidentDetailBody(incidentId: incidentId),
    );
  }
}

class _IncidentDetailBody extends StatelessWidget {
  final int incidentId;
  const _IncidentDetailBody({required this.incidentId});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<IncidentDetailBloc, IncidentDetailState>(
      builder: (context, state) {
        if (state is IncidentDetailLoading) {
          return const SizedBox(height: 400, child: LoadingView());
        }
        if (state is IncidentDetailFailure) {
          return SizedBox(
            height: 300,
            child: ErrorView(
              message: state.message,
              onRetry: () => context
                  .read<IncidentDetailBloc>()
                  .add(IncidentDetailRequested(incidentId)),
            ),
          );
        }

        final loaded = state as IncidentDetailLoaded;
        final incident = loaded.incident;

        return SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _header(context, incident, loaded.isReanalyzing),
              const SizedBox(height: 20),
              _metaRow(incident),
              const Divider(height: 32),
              _rootCauseSection(incident),
              const SizedBox(height: 20),
              _remediationSection(incident),
              const SizedBox(height: 20),
              _payloadSection(
                context,
                title: 'Jira Ticket',
                icon: Icons.bug_report_rounded,
                jsonPayload: incident.aiJiraPayload,
              ),
              const SizedBox(height: 16),
              _payloadSection(
                context,
                title: 'Slack Alert',
                icon: Icons.chat_bubble_rounded,
                jsonPayload: incident.aiSlackPayload,
              ),
              const SizedBox(height: 20),
              _tokenEfficiencySection(incident),
            ],
          ),
        );
      },
    );
  }

  Widget _header(BuildContext context, Incident incident, bool isReanalyzing) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                incident.affectedFeature ?? incident.rootCause,
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                incident.affectedApi ?? incident.fingerprint,
                style: const TextStyle(color: AppTheme.textSecondary),
              ),
            ],
          ),
        ),
        SeverityBadge(severity: incident.severity),
        const SizedBox(width: 8),
        IconButton(
          tooltip: 'Re-run AI diagnosis',
          icon: isReanalyzing
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.smart_toy_outlined),
          onPressed: isReanalyzing
              ? null
              : () => context
                  .read<IncidentDetailBloc>()
                  .add(IncidentReanalyzeRequested(incident.id)),
        ),
      ],
    );
  }

  Widget _metaRow(Incident incident) {
    return Wrap(
      spacing: 20,
      runSpacing: 10,
      children: [
        _metaItem('Occurrences', '${incident.logCount}'),
        _metaItem('Root cause', incident.rootCause),
        _metaItem('First seen', DateFormat('MMM d, HH:mm').format(incident.firstSeenAt)),
        _metaItem('Last seen', DateFormat('MMM d, HH:mm').format(incident.lastSeenAt)),
        _metaItem('Diagnosed by', incident.aiProvider ?? 'not yet analyzed'),
      ],
    );
  }

  Widget _metaItem(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
      ],
    );
  }

  Widget _sectionTitle(String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 16, color: AppTheme.primary),
        const SizedBox(width: 8),
        Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
      ],
    );
  }

  Widget _rootCauseSection(Incident incident) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Root Cause Analysis', Icons.psychology_alt_rounded),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AppTheme.surfaceHighlight,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Text(
            incident.hasDiagnosis
                ? incident.aiRootCauseSummary!
                : 'This incident has not been diagnosed yet. Tap the AI icon above to run diagnosis now.',
            style: const TextStyle(height: 1.4),
          ),
        ),
      ],
    );
  }

  Widget _remediationSection(Incident incident) {
    if (incident.aiRemediationSteps.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Recommended Remediation', Icons.build_circle_rounded),
        const SizedBox(height: 8),
        ...incident.aiRemediationSteps.asMap().entries.map(
              (entry) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 2),
                      width: 20,
                      height: 20,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.15),
                        shape: BoxShape.circle,
                      ),
                      child: Text(
                        '${entry.key + 1}',
                        style: const TextStyle(
                            fontSize: 11, color: AppTheme.primary, fontWeight: FontWeight.bold),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(child: Text(entry.value, style: const TextStyle(height: 1.4))),
                  ],
                ),
              ),
            ),
      ],
    );
  }

  Widget _payloadSection(
    BuildContext context, {
    required String title,
    required IconData icon,
    required String? jsonPayload,
  }) {
    final hasPayload = jsonPayload != null && jsonPayload.trim().isNotEmpty;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _sectionTitle(title, icon),
            if (hasPayload)
              TextButton.icon(
                onPressed: () => _copyToClipboard(context, jsonPayload, title),
                icon: const Icon(Icons.copy_rounded, size: 15),
                label: const Text('Copy'),
              ),
          ],
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AppTheme.surfaceHighlight,
            borderRadius: BorderRadius.circular(10),
          ),
          child: SelectableText(
            hasPayload ? jsonPayload : 'Not available yet.',
            style: const TextStyle(
              fontFamily: 'monospace',
              fontSize: 12.5,
              color: AppTheme.textSecondary,
              height: 1.5,
            ),
          ),
        ),
      ],
    );
  }

  void _copyToClipboard(BuildContext context, String text, String label) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$label payload copied to clipboard'), duration: const Duration(seconds: 2)),
    );
  }

  /// Displays the four token metrics honestly, as different measurements
  /// rather than one misleading before/after number - see the project's
  /// README for why they are not directly comparable.
  Widget _tokenEfficiencySection(Incident incident) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Token Efficiency', Icons.bolt_rounded),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AppTheme.surfaceHighlight,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Column(
            children: [
              _tokenRow('Cumulative raw volume for this incident (est.)',
                  '${incident.estimatedRawTokens} tok'),
              _tokenRow('Representative sample, raw (est.)', '${incident.sampleRawTokens} tok'),
              _tokenRow('Representative sample, pruned (est.)',
                  '${incident.compactedContextTokens} tok'),
              _tokenRow('Actual Gemini prompt (measured)', '${incident.aiPromptTokens} tok',
                  highlight: true),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  '${incident.logCount} log(s) produced exactly 1 AI call for this incident.',
                  style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _tokenRow(String label, String value, {bool highlight = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: highlight ? AppTheme.primary : AppTheme.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
