package com.example.myapplication

import android.content.Context
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: ArrayList<Message>, private val context: Context) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    val SELF = 0
    val SENDER = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (viewType == SELF) {
            return MessageViewHolder(LayoutInflater.from(context).inflate(R.layout.row_message_self, parent, false),viewType)
        } else {
            return MessageViewHolder(LayoutInflater.from(context).inflate(R.layout.row_message, parent, false),viewType)
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.tvMessage.text = messages[holder.adapterPosition].message
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isOwnMessage) SELF else SENDER
    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return messages.size
    }

    inner class MessageViewHolder(view: View,viewType: Int) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)

        init {
            if (viewType == SELF)
                view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu, p1: View?, p2: ContextMenu.ContextMenuInfo?) {
            menu.add(0, 0, adapterPosition, "Delete")
        }
    }

}

