import 'package:get_it/get_it.dart';

import 'core/network/api_client.dart';

import 'features/health/data/health_data.dart';
import 'features/health/domain/health.dart';
import 'features/health/presentation/health_bloc.dart';

import 'features/logs/data/log_data.dart';
import 'features/logs/domain/log.dart';
import 'features/logs/presentation/log_feed_bloc.dart';

import 'features/incidents/data/incident_data.dart';
import 'features/incidents/domain/incident.dart';
import 'features/incidents/presentation/incident_board_bloc.dart';
import 'features/incidents/presentation/incident_detail_bloc.dart';

final GetIt sl = GetIt.instance;

/// Registers every dependency exactly once. Called from main() before
/// runApp(). BLoCs are registered as factories (a fresh instance each time
/// they're requested) since each screen should own its own BLoC lifecycle;
/// everything below a BLoC is a singleton, since there's no reason to
/// rebuild a repository or data source per screen.
Future<void> initDependencies() async {
  // ---- Core -----------------------------------------------------------
  sl.registerLazySingleton<ApiClient>(() => ApiClient());

  // ---- Health feature ---------------------------------------------------
  sl.registerLazySingleton<HealthRemoteDataSource>(
        () => HealthRemoteDataSourceImpl(sl()),
  );
  sl.registerLazySingleton<HealthRepository>(
        () => HealthRepositoryImpl(sl()),
  );
  sl.registerLazySingleton(() => GetHealthStatus(sl()));
  sl.registerFactory(() => HealthBloc(getHealthStatus: sl()));

  // ---- Logs feature -------------------------------------------------------
  sl.registerLazySingleton<LogRemoteDataSource>(
        () => LogRemoteDataSourceImpl(sl()),
  );
  sl.registerLazySingleton<LogRepository>(
        () => LogRepositoryImpl(sl()),
  );
  sl.registerLazySingleton(() => GetLatestLogs(sl()));
  sl.registerFactory(() => LogFeedBloc(getLatestLogs: sl()));

  // ---- Incidents feature --------------------------------------------------
  sl.registerLazySingleton<IncidentRemoteDataSource>(
        () => IncidentRemoteDataSourceImpl(sl()),
  );
  sl.registerLazySingleton<IncidentRepository>(
        () => IncidentRepositoryImpl(sl()),
  );
  sl.registerLazySingleton(() => GetIncidents(sl()));
  sl.registerLazySingleton(() => GetIncidentDetail(sl()));
  sl.registerLazySingleton(() => ReanalyzeIncident(sl()));
  sl.registerFactory(() => IncidentBoardBloc(getIncidents: sl()));
  sl.registerFactory(() => IncidentDetailBloc(
    getIncidentDetail: sl(),
    reanalyzeIncident: sl(),
  ));
}
