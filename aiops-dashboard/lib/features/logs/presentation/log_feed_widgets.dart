import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/state_views.dart';
import '../domain/log.dart';
import 'log_feed_bloc.dart';

/// "Live APM Log Feed: List ingested logs with PII redacted." Redaction
/// itself already happened on the backend (SanitizerService) before this
/// data was ever stored - this widget just displays what the API returns.
class LogFeedList extends StatelessWidget {
  const LogFeedList({super.key});

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
                const Text('Live APM Log Feed',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                IconButton(
                  icon: const Icon(Icons.refresh_rounded, size: 20, color: AppTheme.textSecondary),
                  onPressed: () =>
                      context.read<LogFeedBloc>().add(const LogFeedRequested()),
                ),
              ],
            ),
            const Divider(height: 20),
            SizedBox(
              height: 420,
              child: BlocBuilder<LogFeedBloc, LogFeedState>(
                builder: (context, state) {
                  if (state is LogFeedLoading || state is LogFeedInitial) {
                    return const LoadingView();
                  }
                  if (state is LogFeedFailure) {
                    return ErrorView(
                      message: state.message,
                      onRetry: () =>
                          context.read<LogFeedBloc>().add(const LogFeedRequested()),
                    );
                  }
                  final logs = (state as LogFeedLoaded).logs;
                  if (logs.isEmpty) {
                    return const EmptyView(message: 'No logs ingested yet.');
                  }
                  return ListView.separated(
                    itemCount: logs.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) => _LogTile(log: logs[index]),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LogTile extends StatelessWidget {
  final ApmLog log;
  const _LogTile({required this.log});

  @override
  Widget build(BuildContext context) {
    final isError = log.isError;
    final statusColor = isError ? const Color(0xFFFF5C5C) : const Color(0xFF4CD787);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 8,
            height: 8,
            margin: const EdgeInsets.only(top: 6),
            decoration: BoxDecoration(color: statusColor, shape: BoxShape.circle),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        log.errorType ?? log.logId,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    if (log.statusCode != null)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: statusColor.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          log.statusCode!,
                          style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.bold),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  log.affectedFeature ?? log.affectedApi ?? '-',
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                ),
                if (log.errorMessage != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    log.errorMessage!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                  ),
                ],
                const SizedBox(height: 4),
                Text(
                  DateFormat('MMM d, HH:mm:ss').format(log.timestamp),
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
