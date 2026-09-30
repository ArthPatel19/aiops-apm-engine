import 'package:equatable/equatable.dart';

class ApmLog extends Equatable {
  final int id;
  final String logId;
  final String? errorType;
  final String? statusCode;
  final String? errorPath;
  final String? affectedFeature;
  final String? affectedApi;
  final bool isError;
  final DateTime timestamp;
  final String? errorMessage;

  const ApmLog({
    required this.id,
    required this.logId,
    required this.isError,
    required this.timestamp,
    this.errorType,
    this.statusCode,
    this.errorPath,
    this.affectedFeature,
    this.affectedApi,
    this.errorMessage,
  });

  @override
  List<Object?> get props => [id, logId, timestamp];
}

abstract class LogRepository {
  /// [page] and [size] mirror the backend's GET /api/logs?page=&size=
  Future<List<ApmLog>> getLatestLogs({int page = 0, int size = 50});
}

class GetLatestLogs {
  final LogRepository repository;
  const GetLatestLogs(this.repository);

  Future<List<ApmLog>> call({int page = 0, int size = 50}) =>
      repository.getLatestLogs(page: page, size: size);
}
