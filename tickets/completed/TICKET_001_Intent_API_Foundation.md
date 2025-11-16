# Ticket #001: Implement BroadcastReceiver Intent API Foundation

**Status**: Not Started
**Priority**: Critical (Blocks all other features)
**Assigned To**: Unassigned
**Dependencies**: None

---

## Objective

Create the foundational BroadcastReceiver that will handle all Intent API calls from AI agents, enabling "headless" control of Stone Launcher.

---

## Background

Stone Launcher follows the "Head & Headless" architecture pattern where:
- **Head** = Touch UI (React Native)
- **Headless** = Intent API (for AI agents)

Both must call the same underlying business logic. This ticket implements the Intent API foundation that all features will use.

**Reference**: See LAUNCHER_ARCHITECTURE.md section "The 'Head & Headless' Architecture"

---

## Requirements

### Functional Requirements
- [ ] Receive broadcast Intents with custom actions
- [ ] Parse Intent extras (string, int, boolean, long)
- [ ] Route to appropriate handler based on action
- [ ] Send response broadcasts with results
- [ ] Handle errors gracefully

### Technical Requirements
- [ ] Register receiver in AndroidManifest.xml
- [ ] Support dynamic handler registration
- [ ] Thread-safe operation
- [ ] Proper logging for debugging

---

## Implementation Plan

### Step 1: Create StoneApiReceiver Class

```kotlin
// android/app/src/main/java/com/stonelauncher/api/StoneApiReceiver.kt
package com.stonelauncher.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StoneApiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "StoneApiReceiver"
        const val CATEGORY = "com.stone.launcher.category.API"

        // Result action format
        fun getResultAction(action: String): String {
            return action.replace(".action.", ".result.")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        try {
            val result = when (intent.action) {
                "com.stone.launcher.action.SET_WIFI" -> handleSetWifi(context, intent)
                "com.stone.launcher.action.GET_WIFI_STATE" -> handleGetWifiState(context, intent)
                // Add more handlers as features are implemented
                else -> createErrorResult("Unknown action: ${intent.action}")
            }

            sendResult(context, intent.action ?: "", result)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent", e)
            sendResult(
                context,
                intent.action ?: "",
                createErrorResult("Internal error: ${e.message}")
            )
        }
    }

    private fun handleSetWifi(context: Context, intent: Intent): IntentResult {
        // Placeholder - will be implemented in TICKET_002
        return IntentResult.success(mapOf("wifi_enabled" to false))
    }

    private fun handleGetWifiState(context: Context, intent: Intent): IntentResult {
        // Placeholder - will be implemented in TICKET_002
        return IntentResult.success(mapOf("wifi_enabled" to false))
    }

    private fun sendResult(context: Context, originalAction: String, result: IntentResult) {
        val resultIntent = Intent(getResultAction(originalAction)).apply {
            addCategory(CATEGORY)
            putExtra("success", result.success)

            if (result.success) {
                result.data.forEach { (key, value) ->
                    when (value) {
                        is String -> putExtra(key, value)
                        is Int -> putExtra(key, value)
                        is Boolean -> putExtra(key, value)
                        is Long -> putExtra(key, value)
                        is Double -> putExtra(key, value)
                        is Float -> putExtra(key, value)
                    }
                }
            } else {
                putExtra("error_message", result.errorMessage)
                result.errorCode?.let { putExtra("error_code", it) }
            }
        }

        context.sendBroadcast(resultIntent)
        Log.d(TAG, "Sent result for ${originalAction}: success=${result.success}")
    }

    private fun createErrorResult(message: String, code: String? = null): IntentResult {
        return IntentResult.error(message, code)
    }
}

// Data class for internal results
data class IntentResult(
    val success: Boolean,
    val data: Map<String, Any> = emptyMap(),
    val errorMessage: String? = null,
    val errorCode: String? = null
) {
    companion object {
        fun success(data: Map<String, Any> = emptyMap()) = IntentResult(
            success = true,
            data = data
        )

        fun error(message: String, code: String? = null) = IntentResult(
            success = false,
            errorMessage = message,
            errorCode = code
        )
    }
}
```

### Step 2: Register in AndroidManifest.xml

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest>
    <application>
        <!-- ... other components ... -->

        <!-- Stone Launcher Intent API Receiver -->
        <receiver
            android:name=".api.StoneApiReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.stone.launcher.action.SET_WIFI" />
                <action android:name="com.stone.launcher.action.GET_WIFI_STATE" />
                <!-- Add more actions as features are implemented -->
                <category android:name="com.stone.launcher.category.API" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

### Step 3: Create Test Script

```bash
#!/bin/bash
# test_intent_api.sh

echo "Testing Stone Launcher Intent API..."

# Test unknown action (should return error)
echo "1. Testing unknown action..."
adb shell am broadcast -a com.stone.launcher.action.UNKNOWN_ACTION

# Listen for result (in separate terminal)
# adb shell "logcat -s StoneApiReceiver:* | grep -A 5 'Received intent'"

echo "Done. Check logcat for results."
```

### Step 4: Documentation

Create `android/app/src/main/java/com/stonelauncher/api/README.md`:

```markdown
# Stone Intent API

This package contains the BroadcastReceiver that handles all Intent API calls.

## Adding a New Intent Handler

1. Add the action to the intent-filter in AndroidManifest.xml
2. Add a case to the when() statement in onReceive()
3. Implement the handler function:
   ```kotlin
   private fun handleYourFeature(context: Context, intent: Intent): IntentResult {
       // Extract parameters
       val param = intent.getStringExtra("param_name") ?: return IntentResult.error("Missing param")

       // Call controller
       val controller = YourController(context)
       val result = controller.doSomething(param)

       // Return result
       return result.fold(
           onSuccess = { IntentResult.success(mapOf("result_key" to it)) },
           onFailure = { IntentResult.error(it.message ?: "Unknown error") }
       )
   }
   ```
4. Document in TOOLS.md
5. Add test case
```

---

## Files to Create/Modify

```
android/app/src/main/java/com/stonelauncher/api/StoneApiReceiver.kt (NEW)
android/app/src/main/java/com/stonelauncher/api/IntentResult.kt (NEW)
android/app/src/main/java/com/stonelauncher/api/README.md (NEW)
android/app/src/main/AndroidManifest.xml (MODIFY)
test_intent_api.sh (NEW)
```

---

## Testing Criteria

- [ ] Can receive broadcast intents
- [ ] Returns success result for valid actions
- [ ] Returns error result for unknown actions
- [ ] Handles missing Intent extras gracefully
- [ ] Logs all operations for debugging
- [ ] Results can be received by listening apps
- [ ] Works with adb shell am broadcast commands

### Test Commands

```bash
# Send test intent
adb shell am broadcast -a com.stone.launcher.action.GET_WIFI_STATE

# Monitor logcat
adb logcat -s StoneApiReceiver:*

# Listen for results (requires listener app or adb bridge)
```

---

## Acceptance Criteria

- [ ] BroadcastReceiver registered and working
- [ ] Can handle multiple Intent actions
- [ ] Sends properly formatted result broadcasts
- [ ] Error handling works correctly
- [ ] Code is well-documented
- [ ] Test script created and passes
- [ ] README created for future developers

---

## Resources

- [Android BroadcastReceiver Guide](https://developer.android.com/guide/components/broadcasts)
- LAUNCHER_ARCHITECTURE.md - "Head & Headless" section
- LAUNCHER_REQUIREMENTS.md - "The 'Head & Headless' Pattern"
- TOOLS.md - Intent API specification

---

## Notes

- This is the foundation for all Intent API features
- Keep it simple - complexity belongs in Controllers, not the receiver
- Each handler should be a thin wrapper that calls Controller logic
- All business logic must go in Controller classes (shared with Native Modules)
- **IMPORTANT**: The receiver should NEVER contain business logic directly

---

## Next Steps After Completion

After this ticket is done, you can work on:
- TICKET_002: Implement WiFi Control (first real feature using this API)
- TICKET_005: Create Permission Management System
