import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/network/api_client.dart';
import '../domain/incident.dart';

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

abstract class IncidentDetailEvent extends Equatable {
  const IncidentDetailEvent();
  @override
  List<Object?> get props => [];
}

class IncidentDetailRequested extends IncidentDetailEvent {
  final int incidentId;
  const IncidentDetailRequested(this.incidentId);
  @override
  List<Object?> get props => [incidentId];
}

class IncidentReanalyzeRequested extends IncidentDetailEvent {
  final int incidentId;
  const IncidentReanalyzeRequested(this.incidentId);
  @override
  List<Object?> get props => [incidentId];
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

abstract class IncidentDetailState extends Equatable {
  const IncidentDetailState();
  @override
  List<Object?> get props => [];
}

class IncidentDetailLoading extends IncidentDetailState {
  const IncidentDetailLoading();
}

/// [isReanalyzing] lets the UI show a small inline spinner on the "Re-run AI
/// diagnosis" button without replacing the whole screen with a loading view.
class IncidentDetailLoaded extends IncidentDetailState {
  final Incident incident;
  final bool isReanalyzing;

  const IncidentDetailLoaded(this.incident, {this.isReanalyzing = false});

  @override
  List<Object?> get props => [incident, isReanalyzing];
}

class IncidentDetailFailure extends IncidentDetailState {
  final String message;
  const IncidentDetailFailure(this.message);
  @override
  List<Object?> get props => [message];
}

// ---------------------------------------------------------------------------
// BLoC
// ---------------------------------------------------------------------------

class IncidentDetailBloc extends Bloc<IncidentDetailEvent, IncidentDetailState> {
  final GetIncidentDetail getIncidentDetail;
  final ReanalyzeIncident reanalyzeIncident;

  IncidentDetailBloc({
    required this.getIncidentDetail,
    required this.reanalyzeIncident,
  }) : super(const IncidentDetailLoading()) {
    on<IncidentDetailRequested>(_onRequested);
    on<IncidentReanalyzeRequested>(_onReanalyzeRequested);
  }

  Future<void> _onRequested(
    IncidentDetailRequested event,
    Emitter<IncidentDetailState> emit,
  ) async {
    emit(const IncidentDetailLoading());
    try {
      final incident = await getIncidentDetail(event.incidentId);
      emit(IncidentDetailLoaded(incident));
    } on ApiFailure catch (f) {
      emit(IncidentDetailFailure(f.message));
    } catch (e) {
      emit(IncidentDetailFailure('Unexpected error: $e'));
    }
  }

  Future<void> _onReanalyzeRequested(
    IncidentReanalyzeRequested event,
    Emitter<IncidentDetailState> emit,
  ) async {
    final current = state;
    if (current is IncidentDetailLoaded) {
      emit(IncidentDetailLoaded(current.incident, isReanalyzing: true));
    }
    try {
      final updated = await reanalyzeIncident(event.incidentId);
      emit(IncidentDetailLoaded(updated));
    } on ApiFailure catch (f) {
      emit(IncidentDetailFailure(f.message));
    } catch (e) {
      emit(IncidentDetailFailure('Unexpected error: $e'));
    }
  }
}
