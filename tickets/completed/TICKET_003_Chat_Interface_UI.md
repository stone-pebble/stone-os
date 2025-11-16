# Ticket #021: Chat Interface UI (Native Kotlin)

**Status**: Not Started
**Priority**: HIGH
**Dependencies**: TICKET_020 (Native Kotlin Launcher UI)

---

## Objective

Port the Stone.tsx chat interface to native Kotlin. This ticket focuses on the UI components only - no LiveKit integration yet. We need the chat bubbles, input field, and loading states working in native Android views.

---

## Background

The web prototype has a clean chat interface in Stone.tsx. We need to recreate this in native Kotlin as a Fragment or Activity that can be accessed via swipe gesture from the main launcher.

**Reference Design**: `/Users/samuellarson/Pebble/Github/stone-web-app-proto/ui/src/pages/Stone.tsx`
**Chat Component**: `/Users/samuellarson/Pebble/Github/stone-web-app-proto/ui/src/components/AgentChatInterface.tsx`

---

## Requirements

### Visual Design
- [ ] Black background
- [ ] Chat bubbles (user messages on right, agent on left)
- [ ] Minimal input field at bottom
- [ ] Loading animation (three dots or similar)
- [ ] Smooth scrolling chat history
- [ ] Full screen chat interface

### UI Components
- [ ] Message bubbles (RecyclerView with custom adapter)
- [ ] Input field with send button
- [ ] Typing indicator
- [ ] Connection status indicator
- [ ] Voice input button (placeholder for now)

### Functionality (UI Only)
- [ ] Add messages to chat (hardcoded for testing)
- [ ] Scroll to bottom on new message
- [ ] Show typing indicator
- [ ] Clear input after sending
- [ ] Swipe down to go back to launcher

---

## Implementation Plan

### Step 1: Create ChatActivity
```kotlin
// app/src/main/java/com/stone/launcher/chat/ChatActivity.kt
package com.stone.launcher.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.ImageButton
import android.view.GestureDetector
import android.view.MotionEvent

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var voiceButton: ImageButton
    private lateinit var gestureDetector: GestureDetector

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setupViews()
        setupGestures()

        // Add welcome message
        addMessage(ChatMessage(
            text = "hello",
            isUser = false,
            timestamp = System.currentTimeMillis()
        ))
    }

    private fun setupViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        inputField = findViewById(R.id.inputField)
        sendButton = findViewById(R.id.sendButton)
        voiceButton = findViewById(R.id.voiceButton)

        // Setup RecyclerView
        chatAdapter = ChatAdapter(messages)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = chatAdapter
        }

        // Send button click
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Voice button (placeholder)
        voiceButton.setOnClickListener {
            Toast.makeText(this, "Voice input (coming soon)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isNotEmpty()) {
            // Add user message
            addMessage(ChatMessage(text, true, System.currentTimeMillis()))
            inputField.text.clear()

            // Simulate agent response (for testing)
            simulateAgentResponse()
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun simulateAgentResponse() {
        // Show typing indicator
        showTypingIndicator()

        // Simulate delay and response
        messagesRecyclerView.postDelayed({
            hideTypingIndicator()
            addMessage(ChatMessage(
                "I understand. How can I help you with that?",
                false,
                System.currentTimeMillis()
            ))
        }, 2000)
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false

                val deltaY = e2.y - e1.y
                if (deltaY > 100) {
                    // Swipe down - go back
                    finish()
                    return true
                }

                return false
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
```

### Step 2: Create Chat Layout
```xml
<!-- app/src/main/res/layout/activity_chat.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/messagesRecyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:padding="16dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@+id/inputContainer"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <LinearLayout
        android:id="@+id/inputContainer"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#111111"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageButton
            android:id="@+id/voiceButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_mic"
            android:tint="#FFFFFF" />

        <EditText
            android:id="@+id/inputField"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="what can i do for you?"
            android:textColorHint="#666666"
            android:textColor="#FFFFFF"
            android:background="@null"
            android:padding="12dp"
            android:fontFamily="serif" />

        <ImageButton
            android:id="@+id/sendButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_send"
            android:tint="#FFFFFF" />

    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Step 3: Create Message Model & Adapter
```kotlin
// app/src/main/java/com/stone/launcher/chat/ChatMessage.kt
package com.stone.launcher.chat

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isTyping: Boolean = false
)

// app/src/main/java/com/stone/launcher/chat/ChatAdapter.kt
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_AGENT = 2
        const val VIEW_TYPE_TYPING = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isTyping -> VIEW_TYPE_TYPING
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AGENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
            )
            VIEW_TYPE_AGENT -> AgentMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_agent, parent, false)
            )
            else -> TypingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_typing, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AgentMessageViewHolder -> holder.bind(message)
            is TypingViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = messages.size
}
```

### Step 4: Message Bubble Layouts
```xml
<!-- app/src/main/res/layout/item_message_user.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="end"
    android:padding="4dp">

    <TextView
        android:id="@+id/messageText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:maxWidth="280dp"
        android:background="@drawable/bubble_user"
        android:padding="12dp"
        android:textColor="#000000"
        android:fontFamily="serif" />

</LinearLayout>

<!-- app/src/main/res/layout/item_message_agent.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="start"
    android:padding="4dp">

    <TextView
        android:id="@+id/messageText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:maxWidth="280dp"
        android:background="@drawable/bubble_agent"
        android:padding="12dp"
        android:textColor="#FFFFFF"
        android:fontFamily="serif" />

</LinearLayout>
```

### Step 5: Update MainActivity to Launch Chat
```kotlin
// In MainActivity.kt, update openStoneChat():
private fun openStoneChat() {
    val intent = Intent(this, ChatActivity::class.java)
    startActivity(intent)
}
```

---

## Files to Create/Modify

```
app/src/main/java/com/stone/launcher/chat/
├── ChatActivity.kt (NEW)
├── ChatAdapter.kt (NEW)
├── ChatMessage.kt (NEW)
└── viewholders/
    ├── UserMessageViewHolder.kt (NEW)
    ├── AgentMessageViewHolder.kt (NEW)
    └── TypingViewHolder.kt (NEW)

app/src/main/res/layout/
├── activity_chat.xml (NEW)
├── item_message_user.xml (NEW)
├── item_message_agent.xml (NEW)
└── item_typing.xml (NEW)

app/src/main/res/drawable/
├── bubble_user.xml (NEW)
├── bubble_agent.xml (NEW)
├── ic_mic.xml (NEW)
└── ic_send.xml (NEW)

app/src/main/AndroidManifest.xml (MODIFY - add ChatActivity)
app/src/main/java/com/stone/launcher/MainActivity.kt (MODIFY - launch chat)
```

---

## Testing Criteria

- [ ] Chat interface opens from swipe left on launcher
- [ ] Can type and send messages
- [ ] User messages appear on right
- [ ] Agent messages appear on left
- [ ] Auto-scroll to bottom works
- [ ] Typing indicator shows during simulated response
- [ ] Swipe down returns to launcher
- [ ] Voice button shows placeholder toast
- [ ] Input clears after sending

---

## Acceptance Criteria

- [ ] Native Kotlin implementation (no React Native)
- [ ] Looks like Stone.tsx design
- [ ] Smooth animations and scrolling
- [ ] Clean separation of UI components
- [ ] Ready for LiveKit integration (next ticket)
- [ ] No crashes or memory leaks
- [ ] Follows Android best practices

---

## Research Findings

*This section will be filled by research agent if needed*

---

## Next Steps

After this ticket:
1. TICKET_022: Research and integrate LiveKit Android SDK
2. TICKET_023: Connect to agents.js backend
3. Real-time chat with AI agent

---

## Notes

- This is UI only - no networking yet
- Use hardcoded responses for testing
- Focus on smooth UX and animations
- Prepare structure for LiveKit integration
- Keep components modular and reusable