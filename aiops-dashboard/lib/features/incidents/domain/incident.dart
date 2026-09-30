import 'package:equatable/equatable.dart';

class Incident extends Equatable {
  final int id;
  final String fingerprint;
  final String rootCause;
  final String statusClass;
  final int logCount;
  final String severity;
  final int healthPenalty;
  final DateTime firstSeenAt;
  final DateTime lastSeenAt;
  final String? affectedFeature;
  final String? affectedApi;
  final String? sampleErrorType;
  final String? sampleMessage;

  // AI diagnostic fields
  final String? aiProvider;
  final String? aiRootCauseSummary;
  final List<String> aiRemediationSteps;
  final String? aiJiraPayload;
  final String? aiSlackPayload;
  final DateTime? aiDiagnosedAt;

  // Token-efficiency fields
  final int estimatedRawTokens;
  final int aiPromptTokens;
  final int sampleRawTokens;
  final int compactedContextTokens;

  const Incident({
    required this.id,
    required this.fingerprint,
    required this.rootCause,
    required this.statusClass,
    required this.logCount,
    required this.severity,
    required this.healthPenalty,
    required this.firstSeenAt,
    required this.lastSeenAt,
    this.affectedFeature,
    this.affectedApi,
    this.sampleErrorType,
    this.sampleMessage,
    this.aiProvider,
    this.aiRootCauseSummary,
    this.aiRemediationSteps = const [],
    this.aiJiraPayload,
    this.aiSlackPayload,
    this.aiDiagnosedAt,
    this.estimatedRawTokens = 0,
    this.aiPromptTokens = 0,
    this.sampleRawTokens = 0,
    this.compactedContextTokens = 0,
  });

  bool get hasDiagnosis => aiRootCauseSummary != null && aiRootCauseSummary!.isNotEmpty;

  @override
  List<Object?> get props => [id, fingerprint, logCount, severity, aiDiagnosedAt];
}

abstract class IncidentRepository {
  Future<List<Incident>> getIncidents();
  Future<Incident> getIncidentById(int id);
  Future<Incident> reanalyzeIncident(int id);
}

class GetIncidents {
  final IncidentRepository repository;
  const GetIncidents(this.repository);
  Future<List<Incident>> call() => repository.getIncidents();
}

class GetIncidentDetail {
  final IncidentRepository repository;
  const GetIncidentDetail(this.repository);
  Future<Incident> call(int id) => repository.getIncidentById(id);
}

class ReanalyzeIncident {
  final IncidentRepository repository;
  const ReanalyzeIncident(this.repository);
  Future<Incident> call(int id) => repository.reanalyzeIncident(id);
}
