package com.propcycle.app.ui.lending;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared list row for lending list and map selection. */
public final class LendingItemAdapter
        extends RecyclerView.Adapter<LendingItemAdapter.ItemViewHolder> {

    public interface Listener {
        void onItemClick(@NonNull LendingItem item);
    }

    private final Listener listener;
    private final MarketplaceImageLoader imageLoader;
    private final List<LendingItem> items = new ArrayList<>();
    @Nullable private Double userLatitude;
    @Nullable private Double userLongitude;
    @Nullable private String selectedId;

    public LendingItemAdapter(
            @NonNull MarketplaceImageLoader imageLoader,
            @NonNull Listener listener) {
        this.imageLoader = imageLoader;
        this.listener = listener;
    }

    public void submitList(
            @NonNull List<LendingItem> values,
            @Nullable Double latitude,
            @Nullable Double longitude) {
        items.clear();
        items.addAll(values);
        userLatitude = latitude;
        userLongitude = longitude;
        notifyDataSetChanged();
    }

    public void setSelectedId(@Nullable String itemId) {
        selectedId = itemId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lending_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        LendingItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public void onViewRecycled(@NonNull ItemViewHolder holder) {
        holder.cancelImage();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public final class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final TextView meta;
        private final TextView distance;
        private MarketplaceImageLoader.LoadHandle imageHandle;
        private String boundUrl;

        private ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.lending_item_image);
            title = itemView.findViewById(R.id.lending_item_title);
            meta = itemView.findViewById(R.id.lending_item_meta);
            distance = itemView.findViewById(R.id.lending_item_distance);
        }

        private void bind(@NonNull LendingItem item) {
            cancelImage();
            title.setText(item.getTitle());
            String maxDays = item.getMaxBorrowDays() == null
                    ? "Up to 31 days"
                    : "Up to " + item.getMaxBorrowDays() + " days";
            meta.setText(LendingPolicy.displayLabel(item.getCategory())
                    + "  •  " + maxDays + "  •  " + item.getAreaLabel());
            if (userLatitude != null && userLongitude != null
                    && item.hasApproximateLocation()) {
                double km = LendingPolicy.distanceKm(
                        userLatitude,
                        userLongitude,
                        item.getLatitude(),
                        item.getLongitude());
                distance.setText(String.format(Locale.ROOT, "Approx. %.1f km away", km));
            } else {
                distance.setText(item.getDepositMinor() != null && item.getDepositMinor() > 0
                        ? String.format(Locale.ROOT, "Optional deposit RM %.2f",
                                item.getDepositMinor() / 100d)
                        : "No deposit stated");
            }
            boolean selected = selectedId != null && selectedId.equals(item.getId());
            itemView.setAlpha(selected ? 1f : 0.94f);
            itemView.setContentDescription("Open lending item " + item.getTitle());
            itemView.setOnClickListener(ignored -> listener.onItemClick(item));
            image.setImageResource(R.drawable.ic_bottom_nav_lend_out);
            String url = item.getImageUrl();
            if (url == null || url.trim().isEmpty()) {
                return;
            }
            boundUrl = url;
            imageHandle = imageLoader.load(url, new MarketplaceImageLoader.Callback() {
                @Override
                public void onLoaded(@NonNull Bitmap bitmap) {
                    if (url.equals(boundUrl)) {
                        image.setImageBitmap(bitmap);
                    }
                }

                @Override
                public void onError() {
                    if (url.equals(boundUrl)) {
                        image.setImageResource(R.drawable.ic_bottom_nav_lend_out);
                    }
                }
            });
        }

        private void cancelImage() {
            if (imageHandle != null) {
                imageHandle.cancel();
                imageHandle = null;
            }
            boundUrl = null;
        }
    }
}
