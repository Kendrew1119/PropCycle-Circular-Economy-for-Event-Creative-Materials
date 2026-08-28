package com.propcycle.app.ui.lending;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.propcycle.app.R;
import com.propcycle.app.data.lending.LendingPolicy;
import com.propcycle.app.data.lending.LendingRequest;

import java.util.ArrayList;
import java.util.List;

/** Participant-aware lending request actions shown on Notifications. */
final class LendingRequestAdapter
        extends RecyclerView.Adapter<LendingRequestAdapter.RequestViewHolder> {

    enum Action {
        APPROVE, REJECT, CANCEL, ACTIVATE, REPORT_RETURN, CONFIRM_RETURN, RATE
    }

    interface Listener {
        void onAction(@NonNull LendingRequest request, @NonNull Action action);
    }

    private final String currentUid;
    private final Listener listener;
    private final List<LendingRequest> items = new ArrayList<>();
    private String busyRequestId;

    LendingRequestAdapter(@NonNull String currentUid, @NonNull Listener listener) {
        this.currentUid = currentUid;
        this.listener = listener;
    }

    void submitList(@NonNull List<LendingRequest> values, String busyId) {
        items.clear();
        items.addAll(values);
        busyRequestId = busyId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RequestViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lending_request, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override public int getItemCount() { return items.size(); }

    final class RequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView roleAndDates;
        private final TextView state;
        private final MaterialButton primary;
        private final MaterialButton secondary;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.request_item_title);
            roleAndDates = itemView.findViewById(R.id.request_role_and_dates);
            state = itemView.findViewById(R.id.request_state);
            primary = itemView.findViewById(R.id.request_primary_action);
            secondary = itemView.findViewById(R.id.request_secondary_action);
        }

        void bind(@NonNull LendingRequest request) {
            boolean owner = currentUid.equals(request.getOwnerUid());
            boolean busy = request.getId().equals(busyRequestId);
            title.setText(request.getItemTitle());
            roleAndDates.setText((owner ? "You are the owner" : "You are the borrower")
                    + "  •  " + request.getStartDate() + " to " + request.getEndDate());
            String label = LendingPolicy.displayLabel(request.getStatus());
            if ("active".equals(request.getStatus()) && request.isReturnReported()) {
                label = "Return reported - owner confirmation needed";
            }
            state.setText(label);
            primary.setVisibility(View.GONE);
            secondary.setVisibility(View.GONE);
            primary.setOnClickListener(null);
            secondary.setOnClickListener(null);

            if (owner && "pending".equals(request.getStatus())) {
                configure(primary, "Approve", request, Action.APPROVE, busy);
                configure(secondary, "Reject", request, Action.REJECT, busy);
            } else if (owner && "approved".equals(request.getStatus())) {
                configure(primary, "Confirm pickup", request, Action.ACTIVATE, busy);
            } else if (owner && "active".equals(request.getStatus())
                    && request.isReturnReported()) {
                configure(primary, "Confirm return", request, Action.CONFIRM_RETURN, busy);
            } else if (!owner && ("pending".equals(request.getStatus())
                    || "approved".equals(request.getStatus()))) {
                configure(primary, "Cancel request", request, Action.CANCEL, busy);
            } else if (!owner && "active".equals(request.getStatus())
                    && !request.isReturnReported()) {
                configure(primary, "Mark returned", request, Action.REPORT_RETURN, busy);
            } else if (!owner && "returned".equals(request.getStatus())) {
                configure(primary, "Rate owner", request, Action.RATE, busy);
            }
        }

        private void configure(
                @NonNull MaterialButton button,
                @NonNull String text,
                @NonNull LendingRequest request,
                @NonNull Action action,
                boolean busy) {
            button.setVisibility(View.VISIBLE);
            button.setText(text);
            button.setEnabled(!busy);
            button.setOnClickListener(ignored -> listener.onAction(request, action));
        }
    }
}
