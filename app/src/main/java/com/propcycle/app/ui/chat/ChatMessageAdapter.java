package com.propcycle.app.ui.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.propcycle.app.R;
import com.propcycle.app.data.chat.ChatMessage;
import com.propcycle.app.data.marketplace.MarketplaceImageLoader;
import com.propcycle.app.data.marketplace.MarketplaceListing;
import com.propcycle.app.data.marketplace.MarketplaceListingStatusPolicy;
import com.propcycle.app.data.lending.LendingItem;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.lending.LendingRequest;
import com.propcycle.app.data.lending.LendingRequestActionPolicy;
import com.propcycle.app.ui.common.DemoImageCatalog;
import com.propcycle.app.ui.common.LocalTimestampFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** Renders text, Marketplace, and lending-request rows in one bounded message list. */
final class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onViewMarketplaceItem(@NonNull String itemId);
        void onViewLendingItem(@NonNull String itemId);
        void onLendingAction(
                @NonNull LendingRequest request,
                @NonNull LendingRequestActionPolicy.Action action);
    }

    private static final int VIEW_TEXT = 0;
    private static final int VIEW_MARKETPLACE_ITEM = 1;
    private static final int VIEW_LENDING_REQUEST = 2;

    private final List<ChatMessage> items = new ArrayList<>();
    private final MarketplaceImageLoader imageLoader;
    private final Listener listener;
    private String currentUserId = "";
    private MarketplaceListing marketplaceListing;
    private boolean marketplaceListingLoading;
    private Map<String, LendingRequest> lendingRequests = java.util.Collections.emptyMap();
    private Set<String> loadingLendingRequestIds = java.util.Collections.emptySet();
    private LendingItem lendingItem;
    private String busyLendingRequestId = "";

    ChatMessageAdapter(
            @NonNull Context context,
            @NonNull Listener listener) {
        imageLoader = new MarketplaceImageLoader(context);
        this.listener = listener;
    }

    void submitList(
            @NonNull List<ChatMessage> messages,
            @NonNull String uid,
            @Nullable MarketplaceListing listing,
            boolean listingLoading,
            @NonNull Map<String, LendingRequest> lendingRequests,
            @NonNull Set<String> loadingLendingRequestIds,
            @Nullable LendingItem lendingItem,
            @NonNull String busyLendingRequestId) {
        items.clear();
        items.addAll(messages);
        currentUserId = uid;
        marketplaceListing = listing;
        marketplaceListingLoading = listingLoading;
        this.lendingRequests = lendingRequests;
        this.loadingLendingRequestIds = loadingLendingRequestIds;
        this.lendingItem = lendingItem;
        this.busyLendingRequestId = busyLendingRequestId;
        notifyDataSetChanged();
    }

    void close() {
        imageLoader.close();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = items.get(position);
        if (ChatMessage.TYPE_MARKETPLACE_ITEM.equals(message.getType())) {
            return VIEW_MARKETPLACE_ITEM;
        }
        return ChatMessage.TYPE_LENDING_REQUEST.equals(message.getType())
                ? VIEW_LENDING_REQUEST : VIEW_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_MARKETPLACE_ITEM) {
            return new MarketplaceItemViewHolder(
                    inflater.inflate(R.layout.item_chat_marketplace_card, parent, false));
        }
        if (viewType == VIEW_LENDING_REQUEST) {
            return new LendingRequestViewHolder(
                    inflater.inflate(R.layout.item_chat_lending_request_card, parent, false));
        }
        return new MessageViewHolder(inflater.inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = items.get(position);
        if (holder instanceof MarketplaceItemViewHolder marketplaceHolder) {
            marketplaceHolder.bind(
                    message,
                    marketplaceListing,
                    marketplaceListingLoading,
                    imageLoader,
                    listener);
            return;
        }
        if (holder instanceof LendingRequestViewHolder lendingHolder) {
            LendingRequest request = lendingRequests.get(message.getRequestId());
            lendingHolder.bind(
                    message,
                    request,
                    loadingLendingRequestIds.contains(message.getRequestId()),
                    lendingItem,
                    busyLendingRequestId.equals(message.getRequestId()),
                    currentUserId,
                    imageLoader,
                    listener);
            return;
        }
        bindText((MessageViewHolder) holder, message);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof MarketplaceItemViewHolder marketplaceHolder) {
            marketplaceHolder.cancelImageLoad();
        } else if (holder instanceof LendingRequestViewHolder lendingHolder) {
            lendingHolder.cancelImageLoad();
        }
        super.onViewRecycled(holder);
    }

    private void bindText(@NonNull MessageViewHolder holder, @NonNull ChatMessage message) {
        boolean mine = currentUserId.equals(message.getSenderId());
        holder.message.setText(message.getText());
        String timeLabel = timeLabel(holder.itemView, message);
        holder.time.setText(timeLabel);
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
                (mine ? "You: " : "Other participant: ") + message.getText()
                        + (timeLabel.isEmpty() ? "" : ", " + timeLabel));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    private static String timeLabel(@NonNull View itemView, @NonNull ChatMessage message) {
        if (message.isPendingWrite() || message.getSentAtMillis() <= 0L) {
            return "Sending...";
        }
        return LocalTimestampFormatter.messageLabel(
                message.getSentAtMillis(),
                System.currentTimeMillis(),
                TimeZone.getDefault(),
                Locale.getDefault(),
                android.text.format.DateFormat.is24HourFormat(itemView.getContext()));
    }

    private static String offerLabel(@NonNull MarketplaceListing listing) {
        String intent = listing.getTransactionIntent();
        if ("donation".equalsIgnoreCase(intent)) {
            return "Free to a new home";
        }
        if ("exchange".equalsIgnoreCase(intent)) {
            String terms = listing.getExchangeTerms();
            return terms == null || terms.trim().isEmpty()
                    ? "Available for exchange"
                    : "Exchange · " + terms.trim();
        }
        long priceMinor = listing.getPriceMinor() == null ? 0L : listing.getPriceMinor();
        return String.format(Locale.getDefault(), "For sale · RM %.2f", priceMinor / 100.0);
    }

    private static String statusLabel(@NonNull MarketplaceListing listing) {
        if (MarketplaceListingStatusPolicy.SOLD.equals(listing.getStatus())) {
            return MarketplaceListingStatusPolicy.completedDisplayLabel(
                    listing.getTransactionIntent());
        }
        if (MarketplaceListingStatusPolicy.WITHDRAWN.equals(listing.getStatus())) {
            return "Withdrawn";
        }
        return "Available";
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

    static final class MarketplaceItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView placeholder;
        private final ProgressBar imageProgress;
        private final TextView title;
        private final TextView offer;
        private final TextView status;
        private final MaterialButton viewAction;
        private MarketplaceImageLoader.LoadHandle imageLoadHandle = () -> { };
        private String boundImageUrl = "";

        MarketplaceItemViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.marketplace_item_image);
            placeholder = itemView.findViewById(R.id.marketplace_item_placeholder);
            imageProgress = itemView.findViewById(R.id.marketplace_item_image_progress);
            title = itemView.findViewById(R.id.marketplace_item_title);
            offer = itemView.findViewById(R.id.marketplace_item_offer);
            status = itemView.findViewById(R.id.marketplace_item_status);
            viewAction = itemView.findViewById(R.id.view_marketplace_item_action);
        }

        void bind(
                @NonNull ChatMessage message,
                @Nullable MarketplaceListing listing,
                boolean listingLoading,
                @NonNull MarketplaceImageLoader imageLoader,
                @NonNull Listener clickListener) {
            cancelImageLoad();
            viewAction.setOnClickListener(ignored ->
                    clickListener.onViewMarketplaceItem(message.getItemId()));

            boolean matchingListing = listing != null
                    && message.getItemId().equals(listing.getId());
            if (!matchingListing) {
                title.setText(listingLoading ? "Loading item..." : "Item unavailable");
                offer.setText(listingLoading
                        ? "Fetching current Marketplace details"
                        : "This listing could not be loaded");
                status.setText(listingLoading ? "Loading" : "Unavailable");
                showPlaceholder(0);
                itemView.setContentDescription(title.getText() + ". View item details.");
                return;
            }

            title.setText(listing.getTitle());
            offer.setText(offerLabel(listing));
            status.setText(statusLabel(listing));
            bindImage(listing, imageLoader);
            itemView.setContentDescription(
                    listing.getTitle() + ", " + offer.getText() + ", " + status.getText()
                            + ". View item details.");
        }

        private void bindImage(
                @NonNull MarketplaceListing listing,
                @NonNull MarketplaceImageLoader imageLoader) {
            String imageUrl = listing.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                showPlaceholder(DemoImageCatalog.drawableFor(listing.getDemoImageKey()));
                return;
            }
            boundImageUrl = imageUrl;
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            imageProgress.setVisibility(View.VISIBLE);
            imageLoadHandle = imageLoader.load(imageUrl, new MarketplaceImageLoader.Callback() {
                @Override
                public void onLoaded(@NonNull Bitmap bitmap) {
                    if (!imageUrl.equals(boundImageUrl)) {
                        return;
                    }
                    image.setImageBitmap(bitmap);
                    image.setVisibility(View.VISIBLE);
                    placeholder.setVisibility(View.GONE);
                    imageProgress.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    if (imageUrl.equals(boundImageUrl)) {
                        showPlaceholder(DemoImageCatalog.drawableFor(listing.getDemoImageKey()));
                    }
                }
            });
        }

        private void showPlaceholder(int drawable) {
            imageProgress.setVisibility(View.GONE);
            if (drawable != 0) {
                image.setImageResource(drawable);
                image.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
                return;
            }
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.ic_marketplace_placeholder_box, 0, 0);
        }

        void cancelImageLoad() {
            imageLoadHandle.cancel();
            imageLoadHandle = () -> { };
            boundImageUrl = "";
        }
    }

    static final class LendingRequestViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final ProgressBar imageProgress;
        private final TextView title;
        private final TextView dates;
        private final TextView status;
        private final LinearLayout actionRow;
        private final MaterialButton primaryAction;
        private final MaterialButton secondaryAction;
        private final MaterialButton viewAction;
        private MarketplaceImageLoader.LoadHandle imageLoadHandle = () -> { };
        private String boundImageUrl = "";

        LendingRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.lending_request_image);
            imageProgress = itemView.findViewById(R.id.lending_request_image_progress);
            title = itemView.findViewById(R.id.lending_request_title);
            dates = itemView.findViewById(R.id.lending_request_dates);
            status = itemView.findViewById(R.id.lending_request_status);
            actionRow = itemView.findViewById(R.id.lending_request_action_row);
            primaryAction = itemView.findViewById(R.id.lending_request_primary_action);
            secondaryAction = itemView.findViewById(R.id.lending_request_secondary_action);
            viewAction = itemView.findViewById(R.id.view_lending_item_action);
        }

        void bind(
                @NonNull ChatMessage message,
                @Nullable LendingRequest request,
                boolean requestLoading,
                @Nullable LendingItem item,
                boolean busy,
                @NonNull String currentUid,
                @NonNull MarketplaceImageLoader imageLoader,
                @NonNull Listener listener) {
            cancelImageLoad();
            resetActions();
            viewAction.setOnClickListener(ignored ->
                    listener.onViewLendingItem(message.getItemId()));

            boolean matchingRequest = request != null
                    && message.getRequestId().equals(request.getId())
                    && message.getItemId().equals(request.getItemId());
            if (!matchingRequest) {
                title.setText(requestLoading ? "Loading request..." : "Request unavailable");
                dates.setText(requestLoading
                        ? "Fetching current lending details"
                        : "This lending request could not be loaded");
                status.setText(requestLoading ? "Loading" : "Unavailable");
                showFallbackImage();
                itemView.setContentDescription(title.getText() + ". View lending item details.");
                return;
            }

            title.setText(request.getItemTitle() == null
                    ? "Lending item" : request.getItemTitle());
            dates.setText(request.getStartDate() + " to " + request.getEndDate());
            String statusText = LendingPolicy.displayLabel(request.getStatus());
            if ("active".equals(request.getStatus()) && request.isReturnReported()) {
                statusText = "Return reported";
            }
            status.setText(statusText);
            bindImage(message, item, imageLoader);
            bindActions(request, currentUid, busy, listener);
            itemView.setContentDescription(
                    title.getText() + ", " + dates.getText() + ", " + status.getText()
                            + ". View lending item details.");
        }

        private void bindActions(
                @NonNull LendingRequest request,
                @NonNull String currentUid,
                boolean busy,
                @NonNull Listener listener) {
            List<LendingRequestActionPolicy.Action> actions =
                    LendingRequestActionPolicy.availableActions(request, currentUid);
            if (actions.isEmpty()) {
                return;
            }
            actionRow.setVisibility(View.VISIBLE);
            configure(primaryAction, request, actions.get(0), busy, listener);
            if (actions.size() > 1) {
                configure(secondaryAction, request, actions.get(1), busy, listener);
            }
        }

        private static void configure(
                @NonNull MaterialButton button,
                @NonNull LendingRequest request,
                @NonNull LendingRequestActionPolicy.Action action,
                boolean busy,
                @NonNull Listener listener) {
            button.setVisibility(View.VISIBLE);
            button.setText(LendingRequestActionPolicy.actionLabel(action));
            button.setEnabled(!busy);
            button.setOnClickListener(ignored -> listener.onLendingAction(request, action));
        }

        private void resetActions() {
            actionRow.setVisibility(View.GONE);
            primaryAction.setVisibility(View.GONE);
            secondaryAction.setVisibility(View.GONE);
            primaryAction.setOnClickListener(null);
            secondaryAction.setOnClickListener(null);
        }

        private void bindImage(
                @NonNull ChatMessage message,
                @Nullable LendingItem item,
                @NonNull MarketplaceImageLoader imageLoader) {
            if (item == null || !message.getItemId().equals(item.getId())) {
                showFallbackImage();
                return;
            }
            String url = item.getImageUrl();
            if (url == null || url.trim().isEmpty()) {
                int demoDrawable = DemoImageCatalog.drawableFor(item.getDemoImageKey());
                image.setPadding(demoDrawable == 0 ? 10 : 0,
                        demoDrawable == 0 ? 10 : 0,
                        demoDrawable == 0 ? 10 : 0,
                        demoDrawable == 0 ? 10 : 0);
                image.setScaleType(demoDrawable == 0
                        ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
                image.setImageResource(demoDrawable == 0
                        ? R.drawable.ic_bottom_nav_lend_out : demoDrawable);
                imageProgress.setVisibility(View.GONE);
                return;
            }
            boundImageUrl = url;
            image.setPadding(0, 0, 0, 0);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageDrawable(null);
            imageProgress.setVisibility(View.VISIBLE);
            imageLoadHandle = imageLoader.load(url, new MarketplaceImageLoader.Callback() {
                @Override
                public void onLoaded(@NonNull Bitmap bitmap) {
                    if (url.equals(boundImageUrl)) {
                        image.setImageBitmap(bitmap);
                        imageProgress.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError() {
                    if (url.equals(boundImageUrl)) {
                        showFallbackImage();
                    }
                }
            });
        }

        private void showFallbackImage() {
            image.setPadding(10, 10, 10, 10);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageResource(R.drawable.ic_bottom_nav_lend_out);
            imageProgress.setVisibility(View.GONE);
        }

        void cancelImageLoad() {
            imageLoadHandle.cancel();
            imageLoadHandle = () -> { };
            boundImageUrl = "";
        }
    }
}
