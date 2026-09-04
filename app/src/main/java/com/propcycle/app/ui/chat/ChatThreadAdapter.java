package com.propcycle.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.chat.ChatParticipantPolicy;
import com.propcycle.app.data.chat.ChatThread;
import com.propcycle.app.data.profile.ProfileAvatarPolicy;
import com.propcycle.app.data.profile.PublicProfile;
import com.propcycle.app.ui.common.LocalTimestampFormatter;
import com.propcycle.app.ui.common.ProfileAvatarRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Proposal-faithful rows backed by real thread snapshots. */
final class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ThreadViewHolder> {

    interface Listener {
        void onThreadClick(@NonNull ChatThread thread);

        void onProfileClick(@NonNull String userId);
    }

    private final Listener listener;
    private final List<ChatThread> items = new ArrayList<>();
    private Map<String, PublicProfile> profiles = Collections.emptyMap();
    private String currentUserId = "";

    ChatThreadAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    void submitList(@NonNull List<ChatThread> threads, @NonNull String currentUserId) {
        items.clear();
        items.addAll(threads);
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    void submitProfiles(@NonNull Map<String, PublicProfile> profiles) {
        this.profiles = profiles;
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
        String otherUserId = ChatParticipantPolicy.otherUserId(thread, currentUserId);
        PublicProfile profile = profiles.get(otherUserId);
        String displayName = profile == null ? "PropCycle Member" : profile.getDisplayName();
        String avatarKey = profile == null
                ? ProfileAvatarPolicy.DEFAULT : profile.getAvatarKey();
        ProfileAvatarRenderer.render(
                holder.avatarInitial,
                holder.avatarIcon,
                avatarKey,
                displayName);
        boolean canOpenProfile = !otherUserId.isEmpty();
        holder.avatar.setClickable(canOpenProfile);
        holder.avatar.setFocusable(canOpenProfile);
        holder.avatar.setEnabled(canOpenProfile);
        holder.avatar.setContentDescription(
                canOpenProfile ? "Open " + displayName + "'s profile" : null);
        holder.avatar.setOnClickListener(canOpenProfile
                ? ignored -> listener.onProfileClick(otherUserId)
                : null);
        holder.time.setText(timeLabel(holder.itemView, thread));
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
    private static CharSequence timeLabel(@NonNull View itemView, @NonNull ChatThread thread) {
        if (!thread.hasMessages()) {
            return "";
        }
        long time = thread.getLastMessageAtMillis();
        if (time <= 0L) {
            return thread.hasMessages() ? "Sending..." : "";
        }
        return LocalTimestampFormatter.compactLabel(
                time,
                System.currentTimeMillis(),
                TimeZone.getDefault(),
                Locale.getDefault(),
                android.text.format.DateFormat.is24HourFormat(itemView.getContext()));
    }

    static final class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final View avatar;
        private final TextView avatarInitial;
        private final ImageView avatarIcon;
        private final TextView title;
        private final TextView preview;
        private final TextView time;

        ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.thread_avatar);
            avatarInitial = itemView.findViewById(R.id.thread_avatar_initial);
            avatarIcon = itemView.findViewById(R.id.thread_avatar_icon);
            title = itemView.findViewById(R.id.thread_title);
            preview = itemView.findViewById(R.id.thread_preview);
            time = itemView.findViewById(R.id.thread_time);
        }
    }
}
