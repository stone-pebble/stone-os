# Stone Intent API

This package contains the BroadcastReceiver infrastructure that handles all Intent API calls for Stone Launcher.

## Overview

The Intent API is the "Headless" interface in Stone's "Head & Headless" architecture:

- **Head** = Touch UI (React Native) - Users interact by tapping
- **Headless** = Intent API (Broadcast Intents) - AI agents interact by sending Intents

Both interfaces call the **same underlying Controller classes**, ensuring consistent behavior regardless of how the functionality is accessed.

## Architecture Pattern

```
Touch UI Path:
React Native Component → Native Module → Controller → Android APIs

Intent API Path:
AI Agent → Intent Broadcast → StoneApiReceiver → Controller → Android APIs
                                     ↑
                              (This package)
```

## How Intent Handlers Work

### 1. Intent Format

All Stone Launcher Intents follow this naming convention:

```
Action: com.stone.launcher.action.{FEATURE_NAME}
Category: com.stone.launcher.category.API
Extras: Feature-specific parameters
```

Examples:
- `com.stone.launcher.action.SET_WIFI`
- `com.stone.launcher.action.SEND_SMS`
- `com.stone.launcher.action.CREATE_ALARM`

### 2. Result Format

Results are sent back via broadcast Intent:

```
Action: com.stone.launcher.result.{FEATURE_NAME}
Category: com.stone.launcher.category.API
Extras:
  - success: Boolean (always present)
  - (if success=true) feature-specific result data
  - (if success=false) error_message: String, error_code: String (optional)
```

## Adding a New Intent Handler

Follow these steps to add a new feature to the Intent API:

### Step 1: Implement the Controller

Create the business logic in a Controller class:

```kotlin
// android/app/src/main/java/com/stonelauncher/controllers/YourFeatureController.kt
package com.stonelauncher.controllers

import android.content.Context

class YourFeatureController(private val context: Context) {

    companion object {
        private const val TAG = "YourFeatureController"
    }

    fun doSomething(param: String): Result<ReturnType> {
        return try {
            Log.d(TAG, "Executing doSomething with param: $param")

            // Business logic here
            val result = performOperation(param)

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doSomething", e)
            Result.failure(e)
        }
    }
}
```

### Step 2: Add Handler to StoneApiReceiver

Add a handler method to `StoneApiReceiver.kt`:

```kotlin
private fun handleYourFeature(context: Context, intent: Intent): IntentResult {
    // Extract and validate parameters
    val param = intent.getStringExtra("param_name")
        ?: return IntentResult.error(
            message = "Missing required parameter: param_name",
            code = "MISSING_PARAMETER"
        )

    // Call controller
    val controller = YourFeatureController(context)
    val result = controller.doSomething(param)

    // Convert Result<T> to IntentResult
    return result.fold(
        onSuccess = { value ->
            IntentResult.success(mapOf("result_key" to value))
        },
        onFailure = { error ->
            IntentResult.error(
                message = error.message ?: "Unknown error",
                code = "OPERATION_FAILED"
            )
        }
    )
}
```

### Step 3: Register in onReceive()

Add a case to the `when` statement in `onReceive()`:

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    // ...
    val result = when (intent.action) {
        "com.stone.launcher.action.YOUR_FEATURE" -> handleYourFeature(context, intent)
        // ... other handlers
        else -> createErrorResult("Unknown action: ${intent.action}")
    }
    // ...
}
```

### Step 4: Register in AndroidManifest.xml

Add the Intent action to the receiver's intent-filter:

```xml
<receiver
    android:name=".api.StoneApiReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="com.stone.launcher.action.YOUR_FEATURE" />
        <!-- ... other actions ... -->
        <category android:name="com.stone.launcher.category.API" />
    </intent-filter>
</receiver>
```

### Step 5: Document in TOOLS.md

Add the Intent specification to `/docs/TOOLS.md`:

```markdown
### YOUR_FEATURE

**Action**: `com.stone.launcher.action.YOUR_FEATURE`

**Description**: Brief description of what this does

**Required Extras**:
- `param_name` (String): Description of parameter

**Optional Extras**:
- `optional_param` (Int): Description of optional parameter

**Required Permissions**:
- `android.permission.SOME_PERMISSION` (if applicable)

**Success Response**:
```
Action: com.stone.launcher.result.YOUR_FEATURE
Extras:
  - success: true
  - result_key: value
```

**Error Response**:
```
Action: com.stone.launcher.result.YOUR_FEATURE
Extras:
  - success: false
  - error_message: "Description of error"
  - error_code: "ERROR_CODE"
```

**Example**:
```bash
adb shell am broadcast \
  -a com.stone.launcher.action.YOUR_FEATURE \
  --es param_name "value"
```
```

### Step 6: Add Test Case

Create a test in `test_intent_api.sh`:

```bash
echo "Testing YOUR_FEATURE..."
adb shell am broadcast \
  -a com.stone.launcher.action.YOUR_FEATURE \
  --es param_name "test_value"

# Wait for result
sleep 1

# Verify in logs
adb logcat -d | grep "YOUR_FEATURE" | tail -5
```

## Testing Intent API Calls

### Using adb

```bash
# Send Intent
adb shell am broadcast \
  -a com.stone.launcher.action.GET_WIFI_STATE

# Monitor logs for results
adb logcat -s StoneApiReceiver:*

# Send Intent with parameters
adb shell am broadcast \
  -a com.stone.launcher.action.SET_WIFI \
  --ez enabled true
```

### Parameter Types in adb

- String: `--es key "value"`
- Integer: `--ei key 123`
- Boolean: `--ez key true`
- Long: `--el key 123456789`
- Float: `--ef key 3.14`
- Double: `--ed key 3.14159`

### Listening for Results

To receive result broadcasts, create a BroadcastReceiver:

```kotlin
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val success = intent.getBooleanExtra("success", false)
        if (success) {
            val wifiEnabled = intent.getBooleanExtra("wifi_enabled", false)
            Log.d(TAG, "WiFi state: $wifiEnabled")
        } else {
            val error = intent.getStringExtra("error_message")
            Log.e(TAG, "Error: $error")
        }
    }
}

val filter = IntentFilter("com.stone.launcher.result.GET_WIFI_STATE")
context.registerReceiver(receiver, filter)
```

## Best Practices

### 1. Keep Handlers Thin

Handlers in `StoneApiReceiver` should be thin wrappers:
- Extract parameters from Intent
- Validate required parameters
- Call Controller method
- Convert Controller result to IntentResult

**ALL business logic belongs in Controller classes.**

### 2. Use Result<T> for Controller Methods

Controllers should return `Result<T>` for operations that can fail:

```kotlin
fun doSomething(): Result<Data> {
    return try {
        val data = performRiskyOperation()
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. Validate Parameters Early

Check for required parameters before calling the controller:

```kotlin
private fun handleFeature(context: Context, intent: Intent): IntentResult {
    val required = intent.getStringExtra("required_param")
        ?: return IntentResult.error("Missing required parameter: required_param")

    // Only proceed if all required parameters are present
    val controller = FeatureController(context)
    // ...
}
```

### 4. Provide Clear Error Messages

Error messages should be actionable and specific:

```kotlin
// Good
IntentResult.error("Missing required parameter: phone_number", "MISSING_PARAMETER")

// Bad
IntentResult.error("Error", null)
```

### 5. Log Everything for Debugging

Use Android Log for debugging:

```kotlin
Log.d(TAG, "handleFeature called with param: $param")
Log.e(TAG, "Failed to perform operation", exception)
```

## Error Code Conventions

Use these standard error codes:

- `UNKNOWN_ACTION` - Intent action not recognized
- `MISSING_PARAMETER` - Required Intent extra is missing
- `INVALID_PARAMETER` - Parameter value is invalid
- `PERMISSION_DENIED` - Required permission not granted
- `OPERATION_FAILED` - Operation failed (controller returned failure)
- `INTERNAL_ERROR` - Unexpected exception caught

## Common Gotchas

### Intent Extras Are Nullable

Always check for null and provide defaults or return errors:

```kotlin
// Wrong - will crash if extra is missing
val value = intent.getStringExtra("key")!!

// Right - handle missing extras gracefully
val value = intent.getStringExtra("key")
    ?: return IntentResult.error("Missing parameter: key")
```

### Type Mismatches

Ensure the type you extract matches what's sent:

```bash
# Sent as string
adb shell am broadcast --es count "5"

# Wrong - will return 0
val count = intent.getIntExtra("count", 0)

// Right - get as string and parse
val count = intent.getStringExtra("count")?.toIntOrNull() ?: 0
```

### Exported Receiver Security

The receiver is `exported="true"` to allow external apps to send Intents. Be cautious:

- Validate all input parameters
- Don't trust Intent data
- Check permissions for sensitive operations
- Rate limit if needed (future enhancement)

## Future Enhancements

Planned improvements for the Intent API:

- [ ] Rate limiting to prevent abuse
- [ ] Authentication for sensitive operations
- [ ] Intent queue for long-running operations
- [ ] Batch operation support
- [ ] Callback mechanism for async results

## References

- [Android BroadcastReceiver Guide](https://developer.android.com/guide/components/broadcasts)
- `/docs/LAUNCHER_ARCHITECTURE.md` - "Head & Headless" architecture
- `/docs/LAUNCHER_REQUIREMENTS.md` - Implementation patterns
- `/docs/TOOLS.md` - Complete Intent API specification
