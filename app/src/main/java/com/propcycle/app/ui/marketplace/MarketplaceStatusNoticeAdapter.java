package com.propcycle.app.ui.marketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceStatusNotice;
import com.propcycle.app.ui.common.LocalTimestampFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Compact sold-listing notices; chat messages deliberately remain in Messages. */
public final class MarketplaceStatusNoticeAdapter
        extends RecyclerView.Adapter<MarketplaceStatusNoticeAdapter.NoticeViewHolder> {

    public interface Listener {
        void onOpenListing(@NonNull MarketplaceStatusNotice notice);
    }

    private final Listener listener;
    private final List<MarketplaceStatusNotice> items = new ArrayList<>();

    public MarketplaceStatusNoticeAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<MarketplaceStatusNotice> values) {
        items.clear();
        items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoticeViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketplace_status_notice, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        MarketplaceStatusNotice notice = items.get(position);
        String message = notice.isOwnerView()
                ? "You marked this listing as sold."
                : "Seller marked this listing as sold. Your existing chat is kept.";
        String time = LocalTimestampFormatter.compactLabel(
                notice.getUpdatedAtMillis(),
                System.currentTimeMillis(),
                TimeZone.getDefault(),
                Locale.getDefault(),
                android.text.format.DateFormat.is24HourFormat(holder.itemView.getContext()));
        holder.title.setText(notice.getTitle());
        holder.message.setText(message);
        holder.time.setText(time.isEmpty() ? "Sold" : "Sold • " + time);
        holder.itemView.setContentDescription(
                notice.getTitle() + ". " + message + ". Open listing details.");
        holder.itemView.setOnClickListener(ignored -> listener.onOpenListing(notice));
    }

    @Override public int getItemCount() { return items.size(); }

    static final class NoticeViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView message;
        private final TextView time;

        NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.marketplace_notice_title);
            message = itemView.findViewById(R.id.marketplace_notice_message);
            time = itemView.findViewById(R.id.marketplace_notice_time);
        }
    }
}
