import '../../../core/constants/api_constants.dart';
import '../../../core/network/api_client.dart';
import '../domain/log.dart';

/// Maps directly to LogResponseDto from the Spring Boot backend. Every
/// text field here (errorMessage, etc.) has already been through the
/// backend's PII/secret sanitizer before it ever reaches this app - the
/// Flutter layer never sees a raw secret.
class ApmLogModel extends ApmLog {
  const ApmLogModel({
    required super.id,
    required super.logId,
    required super.isError,
    required super.timestamp,
    super.errorType,
    super.statusCode,
    super.errorPath,
    super.affectedFeature,
    super.affectedApi,
    super.errorMessage,
  });

  factory ApmLogModel.fromJson(Map<String, dynamic> json) {
    return ApmLogModel(
      id: (json['id'] as num?)?.toInt() ?? 0,
      logId: json['log_id'] as String? ?? '',
      isError: json['is_error'] as bool? ?? false,
      timestamp: DateTime.tryParse(json['timestamp'] as String? ?? '')?.toLocal() ??
          DateTime.now(),
      errorType: json['error_type'] as String?,
      statusCode: json['status_code'] as String?,
      errorPath: json['error_path'] as String?,
      affectedFeature: json['affected_feature'] as String?,
      affectedApi: json['affected_api'] as String?,
      errorMessage: json['error_message'] as String?,
    );
  }
}

abstract class LogRemoteDataSource {
  Future<List<ApmLogModel>> fetchLogs({required int page, required int size});
}

class LogRemoteDataSourceImpl implements LogRemoteDataSource {
  final ApiClient client;
  const LogRemoteDataSourceImpl(this.client);

  @override
  Future<List<ApmLogModel>> fetchLogs({required int page, required int size}) async {
    final response = await client.get(
      ApiConstants.logs,
      queryParameters: {'page': page, 'size': size},
    );
    final data = response.data as List<dynamic>;
    return data.map((json) => ApmLogModel.fromJson(json as Map<String, dynamic>)).toList();
  }
}

class LogRepositoryImpl implements LogRepository {
  final LogRemoteDataSource remote;
  const LogRepositoryImpl(this.remote);

  @override
  Future<List<ApmLog>> getLatestLogs({int page = 0, int size = 50}) =>
      remote.fetchLogs(page: page, size: size);
}
