package com.stonelauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stonelauncher.R
import com.stonelauncher.models.ChatMessage

/**
 * Adapter for displaying chat messages
 *
 * TICKET_003: Chat Interface UI
 *
 * Handles three view types:
 * - User messages (right-aligned, blue bubbles)
 * - Agent messages (left-aligned, white bubbles)
 * - Typing indicator (animated dots)
 */
class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AGENT = 2
        private const val VIEW_TYPE_TYPING = 3
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
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AGENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_agent, parent, false)
                AgentMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_typing, parent, false)
                TypingViewHolder(view)
            }
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

    // ViewHolder for user messages
    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }

    // ViewHolder for agent messages
    class AgentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }

    // ViewHolder for typing indicator
    class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // Typing indicator is just animated dots in the layout
            // No additional binding needed
        }
    }
}
