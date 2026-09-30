import 'package:dio/dio.dart';

import '../constants/api_constants.dart';

/// A single, app-wide failure type so every BLoC can show a consistent,
/// human-readable error message regardless of what went wrong underneath
/// (network unreachable, timeout, 4xx/5xx from the backend, bad JSON, etc).
class ApiFailure {
  final String message;
  const ApiFailure(this.message);

  factory ApiFailure.fromDioException(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return const ApiFailure(
            'The backend took too long to respond. Is it running?');
      case DioExceptionType.connectionError:
        return const ApiFailure(
            'Could not reach the backend. Check that Spring Boot is running '
            'and the API URL is correct.');
      case DioExceptionType.badResponse:
        final status = e.response?.statusCode;
        return ApiFailure('Backend returned an error (HTTP $status).');
      default:
        return ApiFailure('Unexpected network error: ${e.message}');
    }
  }

  @override
  String toString() => message;
}

/// Thin wrapper around Dio, configured once with the backend's base URL.
/// Every data source in the app talks to the backend only through this.
class ApiClient {
  late final Dio dio;

  ApiClient({String? baseUrlOverride}) {
    dio = Dio(
      BaseOptions(
        baseUrl: baseUrlOverride ?? ApiConstants.baseUrl,
        connectTimeout: ApiConstants.connectTimeout,
        receiveTimeout: ApiConstants.receiveTimeout,
        headers: {'Content-Type': 'application/json'},
      ),
    );
  }

  Future<Response<dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      return await dio.get(path, queryParameters: queryParameters);
    } on DioException catch (e) {
      throw ApiFailure.fromDioException(e);
    }
  }

  Future<Response<dynamic>> post(String path, {dynamic data}) async {
    try {
      return await dio.post(path, data: data);
    } on DioException catch (e) {
      throw ApiFailure.fromDioException(e);
    }
  }
}
