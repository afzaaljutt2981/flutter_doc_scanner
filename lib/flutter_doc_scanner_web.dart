// In order to *not* need this ignore, consider extracting the "web" version
// of your plugin as a separate package, instead of inlining it in the same
// package as the core of your plugin.
// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html show window;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

import 'flutter_doc_scanner_platform_interface.dart';

/// A web implementation of the FlutterDocScannerPlatform of the FlutterDocScanner plugin.
class FlutterDocScannerWeb extends FlutterDocScannerPlatform {

   /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_doc_scanner');
  /// Constructs a FlutterDocScannerWeb
  FlutterDocScannerWeb();

  static void registerWith(Registrar registrar) {
    FlutterDocScannerPlatform.instance = FlutterDocScannerWeb();
  }

  /// Returns a [String] containing the version of the platform.
  @override
  Future<String?> getPlatformVersion() async {
    final version = html.window.navigator.userAgent;
    return version;
  }

  @override
  Future<String?> getScanDocuments([int page = 5]) async {
    final data = html.window.navigator.userAgent;
    return data;
  }

  @override
  Future<String?> getScanDocumentsUri([int page = 5]) async {
    final data = html.window.navigator.userAgent;
    return data;
  }

    /// **NEW IMPLEMENTATION FOR ADVANCED SCANNING UI**
  @override
  Future<List<String>?> startAdvancedDocumentScan() async {
    try {
      // Invoke a new method on the native side to launch the advanced scanner.
      // The native side will handle the UI and return the result.
      final List<dynamic>? result =
          await methodChannel.invokeMethod('startAdvancedDocumentScan');
      if (result != null) {
        return result.cast<String>(); // Cast the dynamic list to List<String>
      }
      return null;
    } on PlatformException catch (e) {
      // Handle potential errors from the native side, e.g., camera permission denied.
      debugPrint("Failed to start advanced document scan: '${e.message}'.");
      return null;
    }
  }
}
