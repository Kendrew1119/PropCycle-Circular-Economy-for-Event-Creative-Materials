package com.propcycle.app.ui.marketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.data.marketplace.MarketplaceListingValidator;
import com.propcycle.app.databinding.ItemMarketplaceListingBinding;

import java.util.Objects;

/** Two-column proposal-style marketplace cards backed by immutable snapshot lists. */
public final class MarketplaceAdapter
        extends ListAdapter<MarketplaceListing, MarketplaceAdapter.ListingViewHolder> {

    private static final int[] HEIGHTS_DP = {216, 154, 148, 218, 182, 174};

    public interface OnListingClickListener {
        void onListingClick(@NonNull MarketplaceListing listing);
    }

    private static final DiffUtil.ItemCallback<MarketplaceListing> DIFFER =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull MarketplaceListing oldItem,
                        @NonNull MarketplaceListing newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull MarketplaceListing oldItem,
                        @NonNull MarketplaceListing newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                            && Objects.equals(
                                    oldItem.getTransactionIntent(),
                                    newItem.getTransactionIntent())
                            && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                            && Objects.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt());
                }
            };

    private final OnListingClickListener listener;
    private final MarketplaceImageLoader imageLoader;

    public MarketplaceAdapter(
            @NonNull MarketplaceImageLoader imageLoader,
            @NonNull OnListingClickListener listener) {
        super(DIFFER);
        this.imageLoader = imageLoader;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        ItemMarketplaceListingBinding binding = ItemMarketplaceListingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ListingViewHolder(binding, imageLoader, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        holder.bind(getItem(position), position);
    }

    @Override
    public void onViewRecycled(@NonNull ListingViewHolder holder) {
        holder.recycle();
        super.onViewRecycled(holder);
    }

    static final class ListingViewHolder extends RecyclerView.ViewHolder {

        private final ItemMarketplaceListingBinding binding;
        private final MarketplaceImageLoader imageLoader;
        private final OnListingClickListener listener;
        private MarketplaceImageLoader.LoadHandle imageLoadHandle;
        private String expectedImageUrl;

        private ListingViewHolder(
                @NonNull ItemMarketplaceListingBinding binding,
                @NonNull MarketplaceImageLoader imageLoader,
                @NonNull OnListingClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.imageLoader = imageLoader;
            this.listener = listener;
        }

        private void bind(@NonNull MarketplaceListing listing, int position) {
            String title = listing.getTitle() == null || listing.getTitle().trim().isEmpty()
                    ? "Untitled item"
                    : listing.getTitle();
            String meta = MarketplaceListingValidator.displayLabel(
                    listing.getTransactionIntent())
                    + "  |  "
                    + MarketplaceListingValidator.displayLabel(listing.getCategory());

            binding.listingTitle.setText(title);
            binding.listingMeta.setText(meta);
            bindImage(listing.getImageUrl());
            binding.getRoot().setContentDescription("Open marketplace listing " + title);
            binding.getRoot().setOnClickListener(ignored -> listener.onListingClick(listing));

            int cardColor = position % 2 == 0 ? R.color.pc_surface_mid : R.color.pc_surface;
            binding.listingCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), cardColor));

            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            int height = Math.round(
                    HEIGHTS_DP[position % HEIGHTS_DP.length]
                            * binding.getRoot().getResources().getDisplayMetrics().density);
            if (params == null) {
                params = new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        height);
            } else {
                params.height = height;
            }
            binding.getRoot().setLayoutParams(params);
        }

        private void bindImage(String imageUrl) {
            recycle();
            expectedImageUrl = imageUrl;
            binding.listingImage.setImageDrawable(null);
            binding.listingImage.setVisibility(View.GONE);
            binding.listingImagePlaceholder.setText("ITEM");
            binding.listingImagePlaceholder.setVisibility(View.VISIBLE);
            binding.listingImageProgress.setVisibility(View.GONE);
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return;
            }
            binding.listingImagePlaceholder.setText("Loading photo...");
            binding.listingImageProgress.setVisibility(View.VISIBLE);
            imageLoadHandle = imageLoader.load(imageUrl, new MarketplaceImageLoader.Callback() {
                @Override
                public void onLoaded(@NonNull android.graphics.Bitmap bitmap) {
                    if (!Objects.equals(expectedImageUrl, imageUrl)) {
                        return;
                    }
                    imageLoadHandle = null;
                    binding.listingImageProgress.setVisibility(View.GONE);
                    binding.listingImage.setImageBitmap(bitmap);
                    binding.listingImage.setVisibility(View.VISIBLE);
                    binding.listingImagePlaceholder.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    if (!Objects.equals(expectedImageUrl, imageUrl)) {
                        return;
                    }
                    imageLoadHandle = null;
                    binding.listingImageProgress.setVisibility(View.GONE);
                    binding.listingImagePlaceholder.setText("PHOTO UNAVAILABLE");
                }
            });
        }

        private void recycle() {
            expectedImageUrl = null;
            if (imageLoadHandle != null) {
                imageLoadHandle.cancel();
                imageLoadHandle = null;
            }
        }
    }
}
