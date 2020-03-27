import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

class FlutterSecureStorage {
  const FlutterSecureStorage();

  static const MethodChannel _channel =
      const MethodChannel('plugins.indoor.solutions/flutter_secure_storage');

  /// Encrypts and saves the [key] with the given [value].
  ///
  /// If the key was already in the storage, its associated value is changed.
  /// [value] and [key] shoudn't be null.
  /// [iOptions] optional iOS options
  /// [aOptions] optional Android options
  /// Can throw a [PlatformException].
  Future<void> write(
          {@required String key,
          @required String value,
          IOSOptions iOptions,
          AndroidOptions aOptions}) async =>
      _channel.invokeMethod('write', <String, dynamic>{
        'key': key,
        'value': value,
        'options': _selectOptions(iOptions, aOptions)
      });

  /// Decrypts and returns the value for the given [key] or null if [key] is not in the storage.
  ///
  /// [key] shoudn't be null.
  /// [iOptions] optional iOS options
  /// [aOptions] optional Android options
  /// Can throw a [PlatformException].
  Future<String> read(
      {@required String key,
      IOSOptions iOptions,
      AndroidOptions aOptions}) async {
    final String value = await _channel.invokeMethod('read', <String, dynamic>{
      'key': key,
      'options': _selectOptions(iOptions, aOptions)
    });
    return value;
  }

  /// Deletes associated value for the given [key].
  ///
  /// [key] shoudn't be null.
  /// [iOptions] optional iOS options
  /// [aOptions] optional Android options
  /// Can throw a [PlatformException].
  Future<void> delete(
          {@required String key,
          IOSOptions iOptions,
          AndroidOptions aOptions}) =>
      _channel.invokeMethod('delete', <String, dynamic>{
        'key': key,
        'options': _selectOptions(iOptions, aOptions)
      });

  /// Decrypts and returns all keys with associated values.
  ///
  /// [iOptions] optional iOS options
  /// [aOptions] optional Android options
  /// Can throw a [PlatformException].
  Future<Map<String, String>> readAll(
      {IOSOptions iOptions, AndroidOptions aOptions}) async {
    final Map results = await _channel.invokeMethod('readAll',
        <String, dynamic>{'options': _selectOptions(iOptions, aOptions)});
    return results.cast<String, String>();
  }

  /// Deletes all keys with associated values.
  ///
  /// [iOptions] optional iOS options
  /// [aOptions] optional Android options
  /// Can throw a [PlatformException].
  Future<void> deleteAll({IOSOptions iOptions, AndroidOptions aOptions}) =>
      _channel.invokeMethod('deleteAll',
          <String, dynamic>{'options': _selectOptions(iOptions, aOptions)});

  /// Select correct options based on current platform
  Map<String, String> _selectOptions(
      IOSOptions iOptions, AndroidOptions aOptions) {
    return Platform.isIOS ? iOptions?.params : aOptions?.params;
  }
}

abstract class Options {
  Map<String, String> get params => _toMap();

  Map<String, String> _toMap() {
    throw Exception('Missing implementation');
  }
}

class IOSOptions extends Options {
  IOSOptions({String groupId}) : _groupId = groupId;

  final String _groupId;

  @override
  Map<String, String> _toMap() {
    return <String, String>{'groupId': _groupId};
  }
}

class AndroidOptions extends Options {
  @override
  Map<String, String> _toMap() {
    return <String, String>{};
  }
}
