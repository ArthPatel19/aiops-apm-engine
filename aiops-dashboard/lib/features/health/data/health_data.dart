import '../../../core/constants/api_constants.dart';
import '../../../core/network/api_client.dart';
import '../domain/health.dart';

/// Knows how to build a [HealthStatus] out of the backend's exact JSON
/// shape ({"score":65,"incident_count":2,"total_penalty":35}). If the
/// backend's field names ever change, this is the only file that needs
/// to change - the entity and everything above it stays the same.
class HealthStatusModel extends HealthStatus {
  const HealthStatusModel({
    required super.score,
    required super.incidentCount,
    required super.totalPenalty,
  });

  factory HealthStatusModel.fromJson(Map<String, dynamic> json) {
    return HealthStatusModel(
      score: (json['score'] as num?)?.toInt() ?? 0,
      incidentCount: (json['incident_count'] as num?)?.toInt() ?? 0,
      totalPenalty: (json['total_penalty'] as num?)?.toInt() ?? 0,
    );
  }
}

abstract class HealthRemoteDataSource {
  Future<HealthStatusModel> fetchHealth();
}

class HealthRemoteDataSourceImpl implements HealthRemoteDataSource {
  final ApiClient client;
  const HealthRemoteDataSourceImpl(this.client);

  @override
  Future<HealthStatusModel> fetchHealth() async {
    final response = await client.get(ApiConstants.health);
    return HealthStatusModel.fromJson(response.data as Map<String, dynamic>);
  }
}

class HealthRepositoryImpl implements HealthRepository {
  final HealthRemoteDataSource remote;
  const HealthRepositoryImpl(this.remote);

  @override
  Future<HealthStatus> getHealthStatus() => remote.fetchHealth();
}
