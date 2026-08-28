package com.propcycle.app.ui.activity;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.data.activity.ActivityLogRepository;
import com.propcycle.app.data.activity.ActivityRecord;
import com.propcycle.app.databinding.ItemActivityRecordBinding;

import java.util.ArrayList;
import java.util.List;

final class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.Holder> {

    interface Listener {
        void onOpen(@NonNull ActivityRecord record);
    }

    private final Listener listener;
    private final List<ActivityRecord> items = new ArrayList<>();

    RecentActivityAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    void submit(@NonNull List<ActivityRecord> records) {
        items.clear();
        items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemActivityRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemActivityRecordBinding binding;

        private Holder(@NonNull ItemActivityRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(@NonNull ActivityRecord record) {
            binding.activityIcon.setText(icon(record.getType()));
            binding.activityTitle.setText(record.getTitle());
            binding.activityDetail.setText(record.getDetail());
            binding.activityTime.setText(DateUtils.getRelativeTimeSpanString(
                    record.getOccurredAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
            binding.getRoot().setContentDescription(
                    record.getTitle() + ". " + record.getDetail());
            binding.getRoot().setOnClickListener(ignored -> listener.onOpen(record));
        }
    }

    @NonNull
    private static String icon(@NonNull String type) {
        if (ActivityLogRepository.TYPE_SCAN.equals(type)) {
            return "AI";
        }
        if (type.startsWith("marketplace")) {
            return "M";
        }
        if (type.startsWith("lending")) {
            return "L";
        }
        if (ActivityLogRepository.TYPE_RECYCLE_SEARCH.equals(type)) {
            return "R";
        }
        return "•";
    }
}
