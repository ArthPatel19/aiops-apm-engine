import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/network/api_client.dart';
import '../domain/health.dart';

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

abstract class HealthEvent extends Equatable {
  const HealthEvent();
  @override
  List<Object?> get props => [];
}

class HealthRequested extends HealthEvent {
  const HealthRequested();
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

abstract class HealthState extends Equatable {
  const HealthState();
  @override
  List<Object?> get props => [];
}

class HealthInitial extends HealthState {
  const HealthInitial();
}

class HealthLoading extends HealthState {
  const HealthLoading();
}

class HealthLoaded extends HealthState {
  final HealthStatus status;
  const HealthLoaded(this.status);
  @override
  List<Object?> get props => [status];
}

class HealthLoadFailure extends HealthState {
  final String message;
  const HealthLoadFailure(this.message);
  @override
  List<Object?> get props => [message];
}

// ---------------------------------------------------------------------------
// BLoC
// ---------------------------------------------------------------------------

class HealthBloc extends Bloc<HealthEvent, HealthState> {
  final GetHealthStatus getHealthStatus;

  HealthBloc({required this.getHealthStatus}) : super(const HealthInitial()) {
    on<HealthRequested>(_onRequested);
  }

  Future<void> _onRequested(
    HealthRequested event,
    Emitter<HealthState> emit,
  ) async {
    emit(const HealthLoading());
    try {
      final status = await getHealthStatus();
      emit(HealthLoaded(status));
    } on ApiFailure catch (f) {
      emit(HealthLoadFailure(f.message));
    } catch (e) {
      emit(HealthLoadFailure('Unexpected error: $e'));
    }
  }
}
