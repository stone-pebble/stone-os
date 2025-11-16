package com.stonelauncher.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stonelauncher.R
import com.stonelauncher.models.MessageRole
import com.stonelauncher.models.MessageType
import com.stonelauncher.models.UnifiedMessage

/**
 * Adapter for unified message list in chat interface.
 *
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Displays different message types:
 * - User messages (right-aligned)
 * - Assistant messages (left-aligned)
 * - System messages (centered)
 * - Operation status (left-aligned, gray)
 * - Typing indicator
 */
class UnifiedMessageAdapter : ListAdapter<UnifiedMessage, UnifiedMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unified_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val container: LinearLayout = itemView.findViewById(R.id.messageContainer)

        fun bind(message: UnifiedMessage) {
            messageText.text = message.content

            // Style based on role and type
            when {
                message.isTyping -> {
                    // Typing indicator
                    messageText.text = "..."
                    container.gravity = Gravity.START
                    messageText.setBackgroundResource(R.drawable.bg_message_agent)
                    messageText.setTextColor(0xFFCCCCCC.toInt())
                }
                message.role == MessageRole.USER -> {
                    // User message (right-aligned)
                    container.gravity = Gravity.END
                    messageText.setBackgroundResource(R.drawable.bg_message_user)
                    messageText.setTextColor(0xFFFFFFFF.toInt())
                }
                message.role == MessageRole.SYSTEM || message.type == MessageType.OPERATION -> {
                    // System/operation message (centered or left, gray)
                    container.gravity = if (message.type == MessageType.STATUS) {
                        Gravity.CENTER
                    } else {
                        Gravity.START
                    }
                    messageText.setBackgroundResource(R.drawable.bg_message_system)
                    messageText.setTextColor(0xFF888888.toInt())
                }
                message.role == MessageRole.ASSISTANT -> {
                    // Assistant message (left-aligned)
                    container.gravity = Gravity.START
                    messageText.setBackgroundResource(R.drawable.bg_message_agent)
                    messageText.setTextColor(0xFFFFFFFF.toInt())
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<UnifiedMessage>() {
        override fun areItemsTheSame(oldItem: UnifiedMessage, newItem: UnifiedMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UnifiedMessage, newItem: UnifiedMessage): Boolean {
            return oldItem == newItem
        }
    }
}
