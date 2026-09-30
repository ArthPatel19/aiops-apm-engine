import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/theme/severity_style.dart';
import '../domain/incident.dart';

class SeverityBadge extends StatelessWidget {
  final String severity;
  const SeverityBadge({super.key, required this.severity});

  @override
  Widget build(BuildContext context) {
    final color = SeverityStyle.colorFor(severity);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(SeverityStyle.iconFor(severity), size: 13, color: color),
          const SizedBox(width: 5),
          Text(
            severity,
            style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 11),
          ),
        ],
      ),
    );
  }
}

/// One row on the "Incident Board: Display grouped AI Incidents with
/// severity badges."
class IncidentCard extends StatelessWidget {
  final Incident incident;
  final VoidCallback onTap;

  const IncidentCard({super.key, required this.incident, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppTheme.surfaceHighlight,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFF262B35)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    incident.affectedFeature ?? incident.rootCause,
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                SeverityBadge(severity: incident.severity),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              incident.affectedApi ?? incident.fingerprint,
              style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                _pill(Icons.layers_rounded, '${incident.logCount} logs'),
                const SizedBox(width: 8),
                _pill(
                  incident.hasDiagnosis ? Icons.smart_toy_rounded : Icons.hourglass_bottom_rounded,
                  incident.hasDiagnosis
                      ? (incident.aiProvider == 'gemini' ? 'AI diagnosed' : 'Fallback diagnosis')
                      : 'Pending diagnosis',
                ),
                const Spacer(),
                Text(
                  DateFormat('MMM d, HH:mm').format(incident.lastSeenAt),
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _pill(IconData icon, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: AppTheme.textSecondary),
          const SizedBox(width: 4),
          Text(label, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }
}
