# Stone Launcher ProGuard Rules

# Keep BroadcastReceiver for Intent API
-keep class com.stonelauncher.api.StoneApiReceiver { *; }
-keep class com.stonelauncher.api.IntentResult { *; }

# Keep all Controllers (they're called via reflection from Intent handlers)
-keep class com.stonelauncher.controllers.** { *; }

# Keep MainActivity
-keep class com.stonelauncher.MainActivity { *; }
