# Flutter specific rules
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }

# Keep your application classes
-keep class com.example.notificacao_app.** { *; }

# Keep click listeners and related classes
-keepclassmembers class * extends android.view.View {
    public void *(android.view.View);
    public void *(android.view.View, android.view.MotionEvent);
}

# Keep Flutter Gesture Detector
-keep class io.flutter.view.FlutterMain { *; }
-keep class io.flutter.view.FlutterView { *; }
-keep class io.flutter.plugin.platform.PlatformView { *; }
-keep class io.flutter.plugin.editing.** { *; } 