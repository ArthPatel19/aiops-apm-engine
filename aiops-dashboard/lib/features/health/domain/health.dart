import 'package:equatable/equatable.dart';

/// Pure domain entity - no JSON, no Dio, nothing that knows the backend
/// exists. This is what the UI and BLoC layers work with.
class HealthStatus extends Equatable {
  final int score;
  final int incidentCount;
  final int totalPenalty;

  const HealthStatus({
    required this.score,
    required this.incidentCount,
    required this.totalPenalty,
  });

  @override
  List<Object?> get props => [score, incidentCount, totalPenalty];
}

/// The presentation layer depends on this abstraction, never on the
/// concrete Dio-based implementation - that's what makes this "clean
/// architecture": the data layer could be swapped for a fake/mock in tests
/// without touching the BLoC at all.
abstract class HealthRepository {
  Future<HealthStatus> getHealthStatus();
}

/// A use case is a single, named operation the app can perform. For a
/// dashboard this small, a use case is a thin wrapper around one repository
/// call - but it gives every feature a consistent shape, and it's the right
/// place to add caching, retries, or combining multiple calls later without
/// the BLoC needing to change.
class GetHealthStatus {
  final HealthRepository repository;
  const GetHealthStatus(this.repository);

  Future<HealthStatus> call() => repository.getHealthStatus();
}
