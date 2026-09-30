import '../../../core/constants/api_constants.dart';
import '../../../core/network/api_client.dart';
import '../domain/incident.dart';

/// Maps directly to IncidentResponseDto. Every field here traces back,
/// through the backend's pruning/clustering pipeline, to already-sanitized
/// log data - no secret ever reaches this model.
class IncidentModel extends Incident {
  const IncidentModel({
    required super.id,
    required super.fingerprint,
    required super.rootCause,
    required super.statusClass,
    required super.logCount,
    required super.severity,
    required super.healthPenalty,
    required super.firstSeenAt,
    required super.lastSeenAt,
    super.affectedFeature,
    super.affectedApi,
    super.sampleErrorType,
    super.sampleMessage,
    super.aiProvider,
    super.aiRootCauseSummary,
    super.aiRemediationSteps,
    super.aiJiraPayload,
    super.aiSlackPayload,
    super.aiDiagnosedAt,
    super.estimatedRawTokens,
    super.aiPromptTokens,
    super.sampleRawTokens,
    super.compactedContextTokens,
  });

  factory IncidentModel.fromJson(Map<String, dynamic> json) {
    DateTime parseDate(dynamic value) =>
        DateTime.tryParse(value as String? ?? '')?.toLocal() ?? DateTime.now();

    DateTime? parseNullableDate(dynamic value) =>
        value == null ? null : DateTime.tryParse(value as String)?.toLocal();

    return IncidentModel(
      id: (json['id'] as num?)?.toInt() ?? 0,
      fingerprint: json['fingerprint'] as String? ?? '',
      rootCause: json['root_cause'] as String? ?? 'UNKNOWN',
      statusClass: json['status_class'] as String? ?? '',
      logCount: (json['log_count'] as num?)?.toInt() ?? 0,
      severity: json['severity'] as String? ?? 'LOW',
      healthPenalty: (json['health_penalty'] as num?)?.toInt() ?? 0,
      firstSeenAt: parseDate(json['first_seen_at']),
      lastSeenAt: parseDate(json['last_seen_at']),
      affectedFeature: json['affected_feature'] as String?,
      affectedApi: json['affected_api'] as String?,
      sampleErrorType: json['sample_error_type'] as String?,
      sampleMessage: json['sample_message'] as String?,
      aiProvider: json['ai_provider'] as String?,
      aiRootCauseSummary: json['ai_root_cause_summary'] as String?,
      aiRemediationSteps: (json['ai_remediation_steps'] as List<dynamic>? ?? [])
          .map((e) => e.toString())
          .where((s) => s.trim().isNotEmpty)
          .toList(),
      aiJiraPayload: json['ai_jira_payload'] as String?,
      aiSlackPayload: json['ai_slack_payload'] as String?,
      aiDiagnosedAt: parseNullableDate(json['ai_diagnosed_at']),
      estimatedRawTokens: (json['estimated_raw_tokens'] as num?)?.toInt() ?? 0,
      aiPromptTokens: (json['ai_prompt_tokens'] as num?)?.toInt() ?? 0,
      sampleRawTokens: (json['sample_raw_tokens'] as num?)?.toInt() ?? 0,
      compactedContextTokens: (json['compacted_context_tokens'] as num?)?.toInt() ?? 0,
    );
  }
}

abstract class IncidentRemoteDataSource {
  Future<List<IncidentModel>> fetchIncidents();
  Future<IncidentModel> fetchIncidentById(int id);
  Future<IncidentModel> reanalyzeIncident(int id);
}

class IncidentRemoteDataSourceImpl implements IncidentRemoteDataSource {
  final ApiClient client;
  const IncidentRemoteDataSourceImpl(this.client);

  @override
  Future<List<IncidentModel>> fetchIncidents() async {
    final response = await client.get(ApiConstants.incidents);
    final data = response.data as List<dynamic>;
    return data.map((json) => IncidentModel.fromJson(json as Map<String, dynamic>)).toList()
      // Most severe / most recent first, so the board leads with what matters.
      ..sort((a, b) => b.lastSeenAt.compareTo(a.lastSeenAt));
  }

  @override
  Future<IncidentModel> fetchIncidentById(int id) async {
    final response = await client.get(ApiConstants.incidentById(id));
    return IncidentModel.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<IncidentModel> reanalyzeIncident(int id) async {
    final response = await client.post(ApiConstants.incidentAnalyze(id));
    return IncidentModel.fromJson(response.data as Map<String, dynamic>);
  }
}

class IncidentRepositoryImpl implements IncidentRepository {
  final IncidentRemoteDataSource remote;
  const IncidentRepositoryImpl(this.remote);

  @override
  Future<List<Incident>> getIncidents() => remote.fetchIncidents();

  @override
  Future<Incident> getIncidentById(int id) => remote.fetchIncidentById(id);

  @override
  Future<Incident> reanalyzeIncident(int id) => remote.reanalyzeIncident(id);
}
