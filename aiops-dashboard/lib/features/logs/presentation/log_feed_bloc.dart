import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/network/api_client.dart';
import '../domain/log.dart';

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

abstract class LogFeedEvent extends Equatable {
  const LogFeedEvent();
  @override
  List<Object?> get props => [];
}

class LogFeedRequested extends LogFeedEvent {
  const LogFeedRequested();
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

abstract class LogFeedState extends Equatable {
  const LogFeedState();
  @override
  List<Object?> get props => [];
}

class LogFeedInitial extends LogFeedState {
  const LogFeedInitial();
}

class LogFeedLoading extends LogFeedState {
  const LogFeedLoading();
}

class LogFeedLoaded extends LogFeedState {
  final List<ApmLog> logs;
  const LogFeedLoaded(this.logs);
  @override
  List<Object?> get props => [logs];
}

class LogFeedFailure extends LogFeedState {
  final String message;
  const LogFeedFailure(this.message);
  @override
  List<Object?> get props => [message];
}

// ---------------------------------------------------------------------------
// BLoC
// ---------------------------------------------------------------------------

class LogFeedBloc extends Bloc<LogFeedEvent, LogFeedState> {
  final GetLatestLogs getLatestLogs;

  LogFeedBloc({required this.getLatestLogs}) : super(const LogFeedInitial()) {
    on<LogFeedRequested>(_onRequested);
  }

  Future<void> _onRequested(
    LogFeedRequested event,
    Emitter<LogFeedState> emit,
  ) async {
    emit(const LogFeedLoading());
    try {
      final logs = await getLatestLogs(size: 50);
      emit(LogFeedLoaded(logs));
    } on ApiFailure catch (f) {
      emit(LogFeedFailure(f.message));
    } catch (e) {
      emit(LogFeedFailure('Unexpected error: $e'));
    }
  }
}
