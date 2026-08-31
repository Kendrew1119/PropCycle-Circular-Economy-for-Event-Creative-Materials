package com.propcycle.app.ui.chat;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.chat.ChatThread;

import java.util.ArrayList;
import java.util.List;

/** Proposal-faithful rows backed by real thread snapshots. */
final class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ThreadViewHolder> {

    interface OnThreadClickListener {
        void onThreadClick(@NonNull ChatThread thread);
    }

    private final OnThreadClickListener listener;
    private final List<ChatThread> items = new ArrayList<>();

    ChatThreadAdapter(@NonNull OnThreadClickListener listener) {
        this.listener = listener;
    }

    void submitList(@NonNull List<ChatThread> threads) {
        items.clear();
        items.addAll(threads);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_thread, parent, false);
        return new ThreadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        ChatThread thread = items.get(position);
        holder.title.setText(thread.getContextTitle());
        holder.preview.setText(thread.hasMessages()
                ? thread.getLastMessageText()
                : "No messages yet - start the conversation");
        holder.avatar.setText(initial(thread.getContextTitle()));
        holder.time.setText(relativeTime(thread));
        holder.itemView.setBackgroundResource(R.drawable.bg_messages_thread);
        holder.itemView.setContentDescription(
                "Open conversation about " + thread.getContextTitle());
        holder.itemView.setOnClickListener(ignored -> listener.onThreadClick(thread));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private static String initial(@NonNull String title) {
        String trimmed = title.trim();
        return trimmed.isEmpty() ? "P" : trimmed.substring(0, 1).toUpperCase();
    }

    @NonNull
    private static CharSequence relativeTime(@NonNull ChatThread thread) {
        long time = thread.getLastMessageAtMillis() > 0L
                ? thread.getLastMessageAtMillis()
                : thread.getUpdatedAtMillis();
        if (time <= 0L) {
            return thread.hasMessages() ? "Sending..." : "";
        }
        return DateUtils.getRelativeTimeSpanString(
                time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    static final class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatar;
        private final TextView title;
        private final TextView preview;
        private final TextView time;

        ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.thread_avatar);
            title = itemView.findViewById(R.id.thread_title);
            preview = itemView.findViewById(R.id.thread_preview);
            time = itemView.findViewById(R.id.thread_time);
        }
    }
}
