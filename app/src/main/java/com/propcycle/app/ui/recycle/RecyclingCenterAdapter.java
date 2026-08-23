package com.propcycle.app.ui.recycle;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.propcycle.app.R;
import com.propcycle.app.data.recycle.RecyclingCenter;
import com.propcycle.app.data.recycle.RecyclingCenterPolicy;
import com.propcycle.app.databinding.ItemRecyclingCenterBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Small, selection-aware list paired with map markers. */
final class RecyclingCenterAdapter
        extends RecyclerView.Adapter<RecyclingCenterAdapter.CenterViewHolder> {

    interface Listener {
        void onCenterSelected(@NonNull RecyclingCenter center);
    }

    private final Listener listener;
    private List<RecyclingCenter> values = Collections.emptyList();
    private String selectedId = "";

    RecyclingCenterAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    void submitList(@NonNull List<RecyclingCenter> centers) {
        List<RecyclingCenter> oldValues = values;
        List<RecyclingCenter> newValues = Collections.unmodifiableList(new ArrayList<>(centers));
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldValues.size();
            }

            @Override
            public int getNewListSize() {
                return newValues.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return oldValues.get(oldPosition).getId()
                        .equals(newValues.get(newPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return sameContent(oldValues.get(oldPosition), newValues.get(newPosition));
            }
        });
        values = newValues;
        if (findPosition(selectedId) < 0) {
            selectedId = values.isEmpty() ? "" : values.get(0).getId();
        }
        changes.dispatchUpdatesTo(this);
    }

    void setSelectedId(@NonNull String value) {
        if (selectedId.equals(value)) {
            return;
        }
        int oldPosition = findPosition(selectedId);
        selectedId = value;
        int newPosition = findPosition(value);
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (newPosition >= 0) {
            notifyItemChanged(newPosition);
        }
    }

    int findPosition(@NonNull String value) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).getId().equals(value)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean sameContent(
            @NonNull RecyclingCenter first,
            @NonNull RecyclingCenter second) {
        return first.getName().equals(second.getName())
                && first.getAddress().equals(second.getAddress())
                && Double.compare(
                        first.getLocation().getLatitude(),
                        second.getLocation().getLatitude()) == 0
                && Double.compare(
                        first.getLocation().getLongitude(),
                        second.getLocation().getLongitude()) == 0
                && Objects.equals(first.getRating(), second.getRating())
                && Objects.equals(first.getDistanceKm(), second.getDistanceKm());
    }

    @NonNull
    @Override
    public CenterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecyclingCenterBinding binding = ItemRecyclingCenterBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new CenterViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CenterViewHolder holder, int position) {
        holder.bind(values.get(position), position, selectedId, listener);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    static final class CenterViewHolder extends RecyclerView.ViewHolder {

        private final ItemRecyclingCenterBinding binding;

        CenterViewHolder(@NonNull ItemRecyclingCenterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull RecyclingCenter center,
                int position,
                @NonNull String selectedId,
                @NonNull Listener listener) {
            boolean selected = center.getId().equals(selectedId);
            binding.markerNumber.setText(String.valueOf(position + 1));
            binding.centerName.setText(center.getName());
            binding.centerAddress.setText(center.getAddress());
            binding.centerDistance.setText(
                    RecyclingCenterPolicy.formatDistance(center.getDistanceKm()));
            binding.centerRating.setText(RecyclingCenterPolicy.formatRating(center.getRating()));
            binding.centerCard.setStrokeWidth(selected ? 3 : 1);
            binding.centerCard.setStrokeColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    selected ? R.color.pc_primary : R.color.pc_primary_container));
            binding.centerCard.setCardBackgroundColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    selected ? R.color.pc_surface_container : R.color.pc_surface));
            binding.centerCard.setContentDescription(
                    center.getName() + ", "
                            + RecyclingCenterPolicy.formatDistance(center.getDistanceKm()) + ", "
                            + RecyclingCenterPolicy.formatRating(center.getRating()));
            binding.centerCard.setOnClickListener(ignored -> listener.onCenterSelected(center));
        }
    }
}
