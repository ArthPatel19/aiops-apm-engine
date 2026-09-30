import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/network/api_client.dart';
import '../domain/incident.dart';

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

abstract class IncidentBoardEvent extends Equatable {
  const IncidentBoardEvent();
  @override
  List<Object?> get props => [];
}

class IncidentBoardRequested extends IncidentBoardEvent {
  const IncidentBoardRequested();
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

abstract class IncidentBoardState extends Equatable {
  const IncidentBoardState();
  @override
  List<Object?> get props => [];
}

class IncidentBoardInitial extends IncidentBoardState {
  const IncidentBoardInitial();
}

class IncidentBoardLoading extends IncidentBoardState {
  const IncidentBoardLoading();
}

class IncidentBoardLoaded extends IncidentBoardState {
  final List<Incident> incidents;
  const IncidentBoardLoaded(this.incidents);
  @override
  List<Object?> get props => [incidents];
}

class IncidentBoardFailure extends IncidentBoardState {
  final String message;
  const IncidentBoardFailure(this.message);
  @override
  List<Object?> get props => [message];
}

// ---------------------------------------------------------------------------
// BLoC
// ---------------------------------------------------------------------------

class IncidentBoardBloc extends Bloc<IncidentBoardEvent, IncidentBoardState> {
  final GetIncidents getIncidents;

  IncidentBoardBloc({required this.getIncidents}) : super(const IncidentBoardInitial()) {
    on<IncidentBoardRequested>(_onRequested);
  }

  Future<void> _onRequested(
    IncidentBoardRequested event,
    Emitter<IncidentBoardState> emit,
  ) async {
    emit(const IncidentBoardLoading());
    try {
      final incidents = await getIncidents();
      emit(IncidentBoardLoaded(incidents));
    } on ApiFailure catch (f) {
      emit(IncidentBoardFailure(f.message));
    } catch (e) {
      emit(IncidentBoardFailure('Unexpected error: $e'));
    }
  }
}
