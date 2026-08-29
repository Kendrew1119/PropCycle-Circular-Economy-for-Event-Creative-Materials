package com.propcycle.app.ui.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.chat.ChatMessage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Aligns message bubbles according to the active Firebase UID. */
final class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> items = new ArrayList<>();
    private String currentUserId = "";

    void submitList(@NonNull List<ChatMessage> messages, @NonNull String uid) {
        items.clear();
        items.addAll(messages);
        currentUserId = uid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = items.get(position);
        boolean mine = currentUserId.equals(message.getSenderId());
        holder.message.setText(message.getText());
        holder.time.setText(timeLabel(message));
        holder.bubble.setBackgroundResource(
                mine ? R.drawable.bg_conversation_mine : R.drawable.bg_conversation_other);
        holder.message.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                mine ? R.color.pc_white : R.color.pc_brand_text_primary));
        holder.time.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                mine ? R.color.pc_brand_soft_blue : R.color.pc_brand_text_secondary));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.bubble.getLayoutParams();
        params.gravity = mine ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(params);
        holder.itemView.setContentDescription(
                (mine ? "You: " : "Other participant: ") + message.getText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private static String timeLabel(@NonNull ChatMessage message) {
        if (message.isPendingWrite() || message.getSentAtMillis() <= 0L) {
            return "Sending...";
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(new Date(message.getSentAtMillis()));
    }

    static final class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout bubble;
        private final TextView message;
        private final TextView time;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.message_bubble);
            message = itemView.findViewById(R.id.message_text);
            time = itemView.findViewById(R.id.message_time);
        }
    }
}
